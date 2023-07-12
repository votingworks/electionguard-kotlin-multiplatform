package electionguard.encrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.hashFunction
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.*
import io.ktor.utils.io.core.Closeable
import mu.KotlinLogging

private val logger = KotlinLogging.logger("AddEncryptedBallot")

/** Encrypt a ballot and add to election record. Single threaded only. */
class AddEncryptedBallot(
    val group: GroupContext,
    val manifest: Manifest,
    val electionInit: ElectionInitialized,
    val device : String,
    val configBaux0 : ByteArray,
    val chainCodes : Boolean,
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
    val confirmationCodes = mutableListOf<UInt256>()
    val baux0 : ByteArray

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
            val chain: EncryptedBallotChain = chainResult.unwrap()
            require (chainCodes == chain.chaining)
            baux0 = chain.baux0
            ballotIds.addAll(chain.ballotIds)
            confirmationCodes.addAll(chain.confirmationCodes)
            first = false

            // hmmm you could check EncryptedBallotChain each time, in case of crash

        } else {
            baux0 = if (!chainCodes) configBaux0 else
                hashFunction(electionInit.extendedBaseHash.bytes, 0x24.toByte(), configBaux0).bytes // eq 60
        }
    }

    fun encryptAndAdd(ballot: PlaintextBallot, state : EncryptedBallot.BallotState): Boolean {
        if (closed) {
            throw RuntimeException("Adding ballot after chain has been closed")
        }

        val mess = ballotValidator.validate(ballot)
        if (mess.hasErrors()) {
            publisher.writePlaintextBallot(invalidDir, listOf(ballot))
            println(" wrote ${ballot.ballotId} invalid ballots to $invalidDir")
            return false
        }

        val bauxj: ByteArray = if (!chainCodes || first) baux0 else
                               hashFunction(confirmationCodes.last().bytes, configBaux0).bytes // eq 61
        first = false

        // println(" encryptAndAdd ${ballot.ballotId} bauxj=${bauxj.contentToString()}")
        val ciphertextBallot = encryptor.encrypt(ballot, bauxj)

        val eballot =  ciphertextBallot.submit(state)
        sink.writeEncryptedBallot(eballot) // the sink must append

        ballotIds.add(eballot.ballotId)
        confirmationCodes.add(eballot.confirmationCode)

        // hmmm you could write EncryptedBallotChain each time, in case of crash
        return true
    }

    override fun close() {

        // data class EncryptedBallots(
        //    val encryptingDevice: String,
        //    val baux0: ByteArray,
        //    val ballotIds: List<String>,
        //    val closingHash: UInt256?,
        //    val metadata: Map<String, String> = emptyMap(),
        //)
        val closing = EncryptedBallotChain(device, baux0, ballotIds, confirmationCodes, chainCodes, closeChain())
        publisher.writeEncryptedBallotChain(closing)
        sink.close()
    }

    // TODO can we open it again and start adding more to the chain ??
    // TODO where do we store Hbar ?
    fun closeChain() : UInt256? {
        closed = true
        if (!chainCodes) return null

        // Hbar = H(HE ; 24, Baux )
        // Baux = H(Bℓ ) ∥ Baux,0 ∥ b("CLOSE") (62)
        // H(Bℓ ) is the final confirmation code in the chain
        val bauxFinal = hashFunction(confirmationCodes.last().bytes, configBaux0, "CLOSE")
        return hashFunction(electionInit.extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
    }
}