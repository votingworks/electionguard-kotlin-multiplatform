package electionguard.encrypt

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
    val baux0 : ByteArray,
    val chainCodes : Boolean,
    val outputDir: String, // write ballots here, must not have multiple writers to same directory
    val invalidDir: String,
    val isJson : Boolean,
    createNew : Boolean = false,
): Closeable {

    val encryptor = Encryptor(
        group,
        manifest,
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.extendedBaseHash,
        device,
    )
    val ballotValidator = BallotInputValidation(manifest)
    val publisher = makePublisher(outputDir, createNew, isJson)
    val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink()

    var Hj : ByteArray = ByteArray(0) // confirmation code of jth ballot in bytes
    var first = true
    var closed = false

    init {
        val manifestValidator = ManifestInputValidation(manifest)
        val errors = manifestValidator.validate()
        if (errors.hasErrors()) {
            throw RuntimeException("ManifestInputValidation FAILED $errors")
        }
        println(" AddEncryptedBallot baux0=${baux0.contentToString()}")
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

        val bauxj: ByteArray = if (!chainCodes) baux0 else
            if (first) hashFunction(electionInit.extendedBaseHash.bytes, 0x24.toByte(), baux0).bytes // eq 60
            else hashFunction(Hj, baux0).bytes // eq 61
        first = false

        // println(" encryptAndAdd ${ballot.ballotId} bauxj=${bauxj.contentToString()}")
        val ciphertextBallot = encryptor.encrypt(ballot, null, null, bauxj)

        Hj = ciphertextBallot.confirmationCode.bytes
        val eballot =  ciphertextBallot.submit(state)
        sink.writeEncryptedBallot(eballot) // the sink must append
        return true
    }

    override fun close() {
        sink.close()
    }

    // TODO can we open it again and start adding more to the chain ??
    // TODO where do we store Hbar ?
    fun closeChain() : UInt256 {
        closed = true
        // Hbar = H(HE ; 24, Baux )
        // Baux = H(Bℓ ) ∥ Baux,0 ∥ b("CLOSE") (62)
        // H(Bℓ ) is the final confirmation code in the chain
        val bauxFinal = hashFunction(Hj, baux0, "CLOSE")
        return hashFunction(electionInit.extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
    }
}