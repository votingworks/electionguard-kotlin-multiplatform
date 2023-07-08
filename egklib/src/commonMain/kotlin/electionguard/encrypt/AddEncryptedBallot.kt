package electionguard.encrypt

import electionguard.ballot.*
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.*
import io.ktor.utils.io.core.Closeable
import mu.KotlinLogging

private val logger = KotlinLogging.logger("RunEncryptBallot")

/** Encrypt a ballot and add to election record. Single threaded only. */
class AddEncryptedBallot(
    val group: GroupContext,
    val manifest: Manifest,
    val electionInit: ElectionInitialized,
    val outputDir: String, // write ballots here, must not have multiple writers to same directory
    val invalidDir: String,
    val isJson : Boolean,
    val chainCodes : Boolean,
    createNew : Boolean = false,
): Closeable {

    val encryptor = Encryptor(
        group,
        manifest,
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.extendedBaseHash
    )
    val ballotValidator = BallotInputValidation(manifest)
    val publisher = makePublisher(outputDir, createNew, isJson)
    val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink()

    var codeBaux: ByteArray = ByteArray(0)

    init {
        val manifestValidator = ManifestInputValidation(manifest)
        val errors = manifestValidator.validate()
        if (errors.hasErrors()) {
            throw RuntimeException("ManifestInputValidation FAILED $errors")
        }
    }

    fun encryptAndAdd(ballot: PlaintextBallot, state : EncryptedBallot.BallotState): Boolean {
        val mess = ballotValidator.validate(ballot)
        if (mess.hasErrors()) {
            publisher.writePlaintextBallot(invalidDir, listOf(ballot)) // LOOK write just one ??
            println(" wrote ${ballot.ballotId} invalid ballots to $invalidDir")
            return false
        }

        val ciphertextBallot = if (chainCodes) {
            encryptor.encrypt(ballot, null, null, codeBaux)
        } else {
            encryptor.encrypt(ballot, null)
        }

        codeBaux = ciphertextBallot.confirmationCode.bytes
        val eballot =  ciphertextBallot.submit(state)
        sink.writeEncryptedBallot(eballot) // the sink must append
        return true
    }

    override fun close() {
        sink.close()
    }
}