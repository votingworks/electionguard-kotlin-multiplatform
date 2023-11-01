package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.isDirectory
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.input.ManifestInputValidation
import electionguard.pep.BallotPep
import electionguard.util.ErrorMessages

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

    /** The list of devices that have encrytpted ballots. */
    fun encryptingDevices(): List<String>
    /** The encrypted ballot chain for specified device. */
    fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages>
    /** Read encrypted ballots for specified devices. */
    fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    /** Read all encrypted ballots for all devices. */
    fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    fun iterateAllCastBallots(): Iterable<EncryptedBallot>  = iterateAllEncryptedBallots{  it.state == EncryptedBallot.BallotState.CAST }
    fun iterateAllSpoiledBallots(): Iterable<EncryptedBallot>  = iterateAllEncryptedBallots{  it.state == EncryptedBallot.BallotState.SPOILED }
    fun hasEncryptedBallots() : Boolean

    /** Read all decrypted ballots, usually the challenged ones. */
    fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot>

    //// not part of the election record
    /** read plaintext ballots in given directory, private data. */
    fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    /** read trustee in given directory for given guardianId, private data. */
    fun readTrustee(trusteeDir: String, guardianId: String): Result<DecryptingTrusteeIF, ErrorMessages>

    /** Read all the PEP ratio ballots in the given directory. */
    fun iteratePepBallots(pepDir : String): Iterable<BallotPep>
    /** Read a specific file containing an encrypted ballot (eg for PEP). */
    fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, ErrorMessages>
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

// specify the manifest filename, or the directory that its in. May be JSON or proto. If JSON, may be zipped.
// check that the file parses and validates ok
// TODO needs testing
fun readAndCheckManifestBytes(
    group: GroupContext,
    manifestDirOrFile: String,
): Triple<Boolean, Manifest, ByteArray> {
    val isDirectory = isDirectory(manifestDirOrFile)
    val isJson = if (isDirectory) {
        manifestDirOrFile.endsWith(".zip") ||
        pathExists("$manifestDirOrFile/${ElectionRecordJsonPaths.MANIFEST_FILE}")
    } else {
        manifestDirOrFile.endsWith(".json")
    }

    val manifestFile = if (isDirectory) {
        if (isJson) "$manifestDirOrFile/${ElectionRecordJsonPaths.MANIFEST_FILE}" else
            "$manifestDirOrFile/${ElectionRecordProtoPaths.MANIFEST_FILE}"
    } else {
        manifestDirOrFile
    }

    val manifestDir = if (isDirectory) {
        manifestDirOrFile
    } else {
        manifestDirOrFile.substringBeforeLast("/")
    }

    val consumer = if (isJson) {
        ConsumerJson(manifestDir, group)
    } else {
        ConsumerProto(manifestDir, group)
    }

    val manifestBytes = consumer.readManifestBytes(manifestFile)
    // make sure it parses
    val manifest = consumer.makeManifest(manifestBytes)
    // make sure it validates
    val errors = ManifestInputValidation(manifest).validate()
    if (errors.hasErrors()) {
        println("*** ManifestInputValidation error on manifest in $manifestDirOrFile")
        println("$errors")
        throw RuntimeException("*** ManifestInputValidation error on manifest in $manifestDirOrFile")
    }

    return Triple(isJson, manifest, manifestBytes)
}