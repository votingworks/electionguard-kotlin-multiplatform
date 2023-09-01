package electionguard.encrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.Base16.fromHex
import electionguard.core.hashFunction
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("AddEncryptedBallot")

/** Encrypt a ballot and add to election record. Single threaded only. */
class AddEncryptedBallot(
    val group: GroupContext,
    val manifest: Manifest,
    val electionInit: ElectionInitialized,
    val device : String,
    val outputDir: String, // write ballots here, must not have multiple writers to same directory
    val invalidDir: String,
    val isJson : Boolean,
    createNew : Boolean = false,
): Closeable {
    // note that the encryptor doesnt know if its chained
    val encryptor = Encryptor(
        group,
        manifest,
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.extendedBaseHash,
        device,
    )
    val ballotValidator = BallotInputValidation(manifest)
    val publisher = makePublisher(outputDir, createNew, isJson)
    val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink(device)
    val ballotIds = mutableListOf<String>()
    val pending = mutableMapOf<String, CiphertextBallot>() // ccode.toHex() is key
    val configBaux0 : ByteArray = electionInit.config.configBaux0
    val configChaining : Boolean = electionInit.config.chainConfirmationCodes
    val baux0 : ByteArray

    private var lastConfirmationCode: UInt256 = UInt256.ZERO
    private var first = true
    private var closed = false

    init {
        val manifestValidator = ManifestInputValidation(manifest)
        val errors = manifestValidator.validate()
        if (errors.hasErrors()) {
            throw RuntimeException("ManifestInputValidation FAILED $errors")
        }

        val consumer = makeConsumer(outputDir, group, isJson)
        val chainResult = consumer.readEncryptedBallotChain(device)
        if (chainResult is Ok) {
            // this is a restart on an existing chain
            val chain: EncryptedBallotChain = chainResult.unwrap()
            require (configChaining == chain.chaining)
            baux0 = chain.baux0
            ballotIds.addAll(chain.ballotIds)
            this.lastConfirmationCode = chain.lastConfirmationCode
            first = false

            // hmmm you could check EncryptedBallotChain each time, in case of crash

        } else {
            baux0 = if (!configChaining) configBaux0 else
                hashFunction(electionInit.extendedBaseHash.bytes, 0x24.toByte(), configBaux0).bytes // eq 60
        }
    }

    // must be single threaded
    fun encrypt(ballot: PlaintextBallot): Result<CiphertextBallot, String> {
        if (closed) {
            return Err("Adding ballot after chain has been closed")
        }

        val mess = ballotValidator.validate(ballot)
        if (mess.hasErrors()) {
            publisher.writePlaintextBallot(invalidDir, listOf(ballot))
            println(" wrote ${ballot.ballotId} invalid ballots to $invalidDir")
            return Err(" ${ballot.ballotId} did not validate, write to invalidDir= $invalidDir")
        }

        val bauxj: ByteArray = if (!configChaining || first) baux0 else
                               hashFunction(lastConfirmationCode.bytes, configBaux0).bytes // eq 61
        first = false

        val ciphertextBallot = encryptor.encrypt(ballot, bauxj)
        ballotIds.add(ciphertextBallot.ballotId)
        this.lastConfirmationCode = ciphertextBallot.confirmationCode

        // hmmm you could write CiphertextBallot to a log, in case of crash
        pending[ciphertextBallot.confirmationCode.toHex()] = ciphertextBallot
        return Ok(ciphertextBallot)
    }

    fun submit(ccode: UInt256, state : EncryptedBallot.BallotState): Result<Boolean, String> {
        val cballot = pending.remove(ccode.toHex())
        if (cballot == null) {
            logger.error{ "Tried to submit unknown ballot ccode=$ccode" }
            return Err( "Tried to submit unknown ballot ccode=$ccode" )
        }
        try {
            val eballot = cballot.submit(state)
            sink.writeEncryptedBallot(eballot) // the sink must append
            return Ok(true)
        } catch (t: Throwable) {
            logger.throwing(t) // TODO
            return Err("Tried to submit Ciphertext ballot ccode=$ccode error = ${t.message}")
        }
    }

    // write out pending, encryptedBallots, and chain (if chainCodes is true)
    fun sync() {
        if (pending.isNotEmpty()) {
            val keys = pending.keys.toList()
            keys.forEach {
                logger.error{ "pending Ciphertext ballot ${it} was not submitted" }
                val ba = it.fromHex() ?: throw RuntimeException("illegal confirmation code")
                submit(UInt256(ba), EncryptedBallot.BallotState.UNKNOWN)
            }
        }
        val closing = EncryptedBallotChain(device, baux0, ballotIds, this.lastConfirmationCode, configChaining, closeChain())
        publisher.writeEncryptedBallotChain(closing)
    }

    fun closeChain() : UInt256? {
        if (!configChaining) return null

        // Hbar = H(HE ; 24, Baux )
        // Baux = H(Bℓ ) ∥ Baux,0 ∥ b("CLOSE") (62)
        // H(Bℓ ) is the final confirmation code in the chain
        val bauxFinal = hashFunction(lastConfirmationCode.bytes, configBaux0, "CLOSE")
        return hashFunction(electionInit.extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
    }

    override fun close() {
        sync()
        sink.close()
        closed = true
    }
}