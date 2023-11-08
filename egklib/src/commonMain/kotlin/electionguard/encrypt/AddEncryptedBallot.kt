package electionguard.encrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.hashFunction
import electionguard.decryptBallot.DecryptWithNonce
import electionguard.input.BallotInputValidation
import electionguard.publish.*
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("AddEncryptedBallot")

/** Encrypt a ballot and add to election record. Single threaded only. */
class AddEncryptedBallot(
    val group: GroupContext,
    val manifest: Manifest, // should already be validated
    val configChaining: Boolean,
    val configBaux0: ByteArray,
    val jointPublicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256,
    val deviceName: String,
    val outputDir: String, // write ballots to outputDir/encrypted_ballots/deviceName, must not have multiple writers to same directory
    val invalidDir: String, // write plaintext ballots that fail validation
    val isJson: Boolean, // must match election record serialization type
) : Closeable {
    val ballotValidator = BallotInputValidation(manifest)

    // note that the encryptor doesnt know if its chained
    val encryptor = Encryptor(
        group,
        manifest,
        jointPublicKey,
        extendedBaseHash,
        deviceName,
    )
    val decryptor = DecryptWithNonce(
        group,
        jointPublicKey,
        extendedBaseHash
    )

    val publisher = makePublisher(outputDir, false, isJson)
    val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink(deviceName)
    val baux0: ByteArray

    private val ballotIds = mutableListOf<String>()
    private val pending = mutableMapOf<UInt256, CiphertextBallot>() // key = ccode.toHex()
    private var lastConfirmationCode: UInt256 = UInt256.ZERO
    private var first = true
    private var closed = false

    init {
        val consumer = makeConsumer(group, outputDir, isJson)
        val chainResult = consumer.readEncryptedBallotChain(deviceName)
        if (chainResult is Ok) {
            // this is a restart on an existing chain
            val chain: EncryptedBallotChain = chainResult.unwrap()
            require(configChaining == chain.chaining) { "mismatched chaining config=$configChaining ouputDir=${chain.chaining}" }
            baux0 = chain.baux0
            ballotIds.addAll(chain.ballotIds)
            this.lastConfirmationCode = chain.lastConfirmationCode
            first = false

            // hmmm you could check EncryptedBallotChain each time, in case of crash

        } else {
            baux0 = if (!configChaining) configBaux0 else
                // H0 = H(HE ; 0x24, Baux,0 ), eq (59)
                hashFunction(extendedBaseHash.bytes, 0x24.toByte(), configBaux0).bytes
        }
    }

    fun encrypt(ballot: PlaintextBallot, errs : ErrorMessages): CiphertextBallot? {
        if (closed) {
            errs.add("Trying to add ballot after chain has been closed")
            return null
        }

        val validation = ballotValidator.validate(ballot)
        if (validation.hasErrors()) {
            publisher.writePlaintextBallot(invalidDir, listOf(ballot))
            errs.add("${ballot.ballotId} did not validate (wrote to invalidDir=$invalidDir) because $validation")
            return null
        }

        // Baux,j = Hj−1 ∥ Baux,0 eq (60)
        val bauxj: ByteArray = if (!configChaining || first) baux0 else lastConfirmationCode.bytes + configBaux0
        first = false

        val ciphertextBallot = encryptor.encrypt(ballot, bauxj, errs)
        if (errs.hasErrors()) {
            return null
        }
        ballotIds.add(ciphertextBallot!!.ballotId)
        this.lastConfirmationCode = ciphertextBallot.confirmationCode

        // hmmm you could write CiphertextBallot to a log, in case of crash
        pending[ciphertextBallot.confirmationCode] = ciphertextBallot
        return ciphertextBallot
    }

    /** encrypt and cast, does not leave in pending. optional write. */
    fun encryptAndCast(ballot: PlaintextBallot, errs : ErrorMessages, writeToDisk: Boolean = true): EncryptedBallot? {
        val cballot = encrypt(ballot, errs)
        if (errs.hasErrors()) {
            return null
        }
        val eballot = cballot!!.submit(EncryptedBallot.BallotState.CAST)
        if (writeToDisk) {
            submit(eballot.confirmationCode, EncryptedBallot.BallotState.CAST)
        } else {
            // remove from pending
           pending.remove(eballot.confirmationCode)
        }
        return eballot
    }

    fun submit(ccode: UInt256, state: EncryptedBallot.BallotState): Result<EncryptedBallot, String> {
        val cballot = pending.remove(ccode)
        if (cballot == null) {
            logger.error { "Tried to submit state=$state  unknown ballot ccode=$ccode" }
            return Err("Tried to submit state=$state  unknown ballot ccode=$ccode")
        }
        return try {
            val eballot = cballot.submit(state)
            sink.writeEncryptedBallot(eballot)
            Ok(eballot)
        } catch (t: Throwable) {
            logger.throwing(t) // TODO
            Err("Tried to submit Ciphertext ballot state=$state ccode=$ccode error = ${t.message}")
        }
    }

    fun cast(ccode: UInt256): Result<EncryptedBallot, String> {
        return submit(ccode, EncryptedBallot.BallotState.CAST)
    }

    fun challenge(ccode: UInt256): Result<EncryptedBallot, String> {
        return submit(ccode, EncryptedBallot.BallotState.SPOILED)
    }

    fun challengeAndDecrypt(ccode: UInt256): Result<PlaintextBallot, String> {
        val cballot = pending.remove(ccode)
        if (cballot == null) {
            logger.error { "Tried to submit unknown ballot ccode=$ccode" }
            return Err("Tried to submit unknown ballot ccode=$ccode")
        }
        try {
            val eballot = cballot.spoil()
            sink.writeEncryptedBallot(eballot) // record the encrypted, challenged ballot
            with(decryptor) {
                val dballotResult: Result<PlaintextBallot, String> = eballot.decrypt(cballot.ballotNonce)
                if (dballotResult is Ok) {
                    return Ok(dballotResult.unwrap())
                } else {
                    return Err("Decryption error ballot ccode=$ccode")
                }
            }
        } catch (t: Throwable) {
            logger.throwing(t) // TODO
            return Err("Tried to challenge Ciphertext ballot ccode=$ccode error = ${t.message}")
        }
    }

    // write out pending encryptedBallots, and chain (if chainCodes is true)
    fun sync() {
        if (pending.isNotEmpty()) {
            val copyPending = pending.toMap() // make copy so it can be modified
            copyPending.keys.forEach {
                logger.error { "pending Ciphertext ballot ${it} was not submitted, marking 'UNKNOWN'" }
                submit(it, EncryptedBallot.BallotState.UNKNOWN)
            }
        }
        val closing =
            EncryptedBallotChain(deviceName, baux0, ballotIds, this.lastConfirmationCode, configChaining, closeChain())
        publisher.writeEncryptedBallotChain(closing)
    }

    fun closeChain(): UInt256? {
        if (!configChaining) return null

        // Hbar = H(HE ; 0x24, Baux )
        // Baux = H(Bℓ) ∥ Baux,0 ∥ b("CLOSE")   (eq 61)
        // H(Bℓ) is the final confirmation code in the chain
        val bauxFinal = lastConfirmationCode.bytes + configBaux0 + "CLOSE".encodeToByteArray()
        return hashFunction(extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
    }

    override fun close() {
        sync()
        sink.close()
        closed = true
    }
}