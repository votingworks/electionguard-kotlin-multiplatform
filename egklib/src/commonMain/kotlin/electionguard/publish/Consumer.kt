package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.isDirectory
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.input.ManifestInputValidation
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("Consumer")

/** public API to read from the election record */
interface Consumer {
    fun topdir() : String
    fun isJson() : Boolean

    fun readManifestBytes(filename : String): ByteArray
    fun makeManifest(manifestBytes: ByteArray): Manifest

    fun readElectionConfig(): Result<ElectionConfig, ErrorMessages>
    fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages>
    fun readTallyResult(): Result<TallyResult, ErrorMessages>
    fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages>

    /** Are there any encrypted ballots? */
    fun hasEncryptedBallots() : Boolean
    /** The list of devices that have encrypted ballots. */
    fun encryptingDevices(): List<String>
    /** The encrypted ballot chain for specified device. */
    fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages>
    /** Read a specific file containing an encrypted ballot. */
    fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, ErrorMessages>
    /** Read encrypted ballots for specified device. */
    fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    /** Read all encrypted ballots for all devices. */
    fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    fun iterateAllCastBallots(): Iterable<EncryptedBallot>  = iterateAllEncryptedBallots{  it.state == EncryptedBallot.BallotState.CAST }
    fun iterateAllSpoiledBallots(): Iterable<EncryptedBallot>  = iterateAllEncryptedBallots{  it.state == EncryptedBallot.BallotState.SPOILED }

    /** Read all decrypted ballots, usually the challenged ones. */
    fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot>

    //// not part of the election record

    /** read plaintext ballots in given directory, private data. */
    fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    /** read trustee in given directory for given guardianId, private data. */
    fun readTrustee(trusteeDir: String, guardianId: String): Result<DecryptingTrusteeIF, ErrorMessages>
}

fun makeConsumer(
    group: GroupContext,
    topDir: String,
    isJson: Boolean? = null, // if not set, check if manifest.json file exists
): Consumer {
    val useJson = isJson ?: topDir.endsWith(".zip") ||
            pathExists("$topDir/${ElectionRecordJsonPaths.MANIFEST_FILE}")

    return if (useJson) {
        ConsumerJson(topDir, group)
    } else {
        ConsumerProto(topDir, group)
    }
}

fun makeInputBallotSource(
    ballotDir: String,
    group: GroupContext,
    isJson: Boolean? = null, // if not set, check if PLAINTEXT_BALLOT_FILE file exists
): Consumer {
    val useJson = isJson ?: !pathExists("$ballotDir/${ElectionRecordProtoPaths.PLAINTEXT_BALLOT_FILE}")

    return if (useJson) {
        ConsumerJson(ballotDir, group)
    } else {
        ConsumerProto(ballotDir, group)
    }
}

fun makeTrusteeSource(
    trusteeDir: String,
    group: GroupContext,
    isJson: Boolean,
): Consumer {

    return if (isJson) {
        ConsumerJson(trusteeDir, group)
    } else {
        ConsumerProto(trusteeDir, group)
    }
}

/**
 * Read the manifest and check that the file parses and validates.
 * @param manifestDirOrFile manifest filename, or the directory that its in. May be JSON or proto. If JSON, may be zipped
 * @return isJson, manifest, manifestBytes
 */
fun readAndCheckManifest(group: GroupContext, manifestDirOrFile: String): Triple<Boolean, Manifest, ByteArray> {

    val isZip = manifestDirOrFile.endsWith(".zip")
    val isDirectory = isDirectory(manifestDirOrFile)
    val isJson = if (isDirectory) {
        pathExists("$manifestDirOrFile/${ElectionRecordJsonPaths.MANIFEST_FILE}")
    } else {
        isZip || manifestDirOrFile.endsWith(".json")
    }

    val manifestFile = if (isDirectory) {
        if (isJson) "$manifestDirOrFile/${ElectionRecordJsonPaths.MANIFEST_FILE}" else
            "$manifestDirOrFile/${ElectionRecordProtoPaths.MANIFEST_FILE}"
    } else if (isZip) {
        ElectionRecordJsonPaths.MANIFEST_FILE
    } else {
        manifestDirOrFile
    }

    val manifestDir = if (isDirectory || isZip) {
        manifestDirOrFile
    } else {
        manifestDirOrFile.substringBeforeLast("/")
    }

    val consumer = if (isJson) {
        ConsumerJson(manifestDir, group)
    } else {
        ConsumerProto(manifestDir, group)
    }

    try {
        val manifestBytes = consumer.readManifestBytes(manifestFile)
        // make sure it parses
        val manifest = consumer.makeManifest(manifestBytes)
        // make sure it validates
        val errors = ManifestInputValidation(manifest).validate()
        if (errors.hasErrors()) {
            logger.error { "*** ManifestInputValidation error on manifest file= $manifestFile \n $errors" }
            throw RuntimeException("*** ManifestInputValidation error on manifest file= $manifestFile \n $errors")
        }
        return Triple(isJson, manifest, manifestBytes)

    } catch (t: Throwable) {
        logger.error {"readAndCheckManifestBytes Exception= ${t.message} ${t.stackTraceToString()}" }
        throw t
    }

}