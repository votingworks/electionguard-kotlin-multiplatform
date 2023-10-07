package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.isDirectory
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.input.ManifestInputValidation
import electionguard.pep.BallotPep

/** public API to read from the election record */
interface Consumer {
    fun topdir() : String
    fun isJson() : Boolean

    fun readManifestBytes(filename : String): ByteArray
    fun makeManifest(manifestBytes: ByteArray): Manifest

    fun readElectionConfig(): Result<ElectionConfig, String>
    fun readElectionInitialized(): Result<ElectionInitialized, String>
    fun readTallyResult(): Result<TallyResult, String>
    fun readDecryptionResult(): Result<DecryptionResult, String>

    fun encryptingDevices(): List<String>
    fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, String>
    fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    fun iterateAllCastBallots(): Iterable<EncryptedBallot>  = iterateAllEncryptedBallots{  it.state == EncryptedBallot.BallotState.CAST }
    fun iterateAllSpoiledBallots(): Iterable<EncryptedBallot>  = iterateAllEncryptedBallots{  it.state == EncryptedBallot.BallotState.SPOILED }
    fun hasEncryptedBallots() : Boolean

    fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot>
    fun iteratePepBallots(pepDir : String): Iterable<BallotPep>

    //// not part of the election record, private data
    // read plaintext ballots in given directory, with filter
    fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    // trustee in given directory for given guardianId
    fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF

    fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, String>
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
    // TODO
    // val useJson = isJson ?: !pathExists("$trusteeDir/${ElectionRecordProtoPaths.PLAINTEXT_BALLOT_FILE}")

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
        println("*** ManifestInputValidation FAILED on manifest in $manifestDirOrFile")
        println("$errors")
        throw RuntimeException("*** ManifestInputValidation FAILED on manifest in $manifestDirOrFile")
    }

    return Triple(isJson, manifest, manifestBytes)
}