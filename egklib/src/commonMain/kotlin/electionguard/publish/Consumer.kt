package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.isDirectory
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeIF

/** public API to read from the election record */
interface Consumer {
    fun topdir() : String
    fun isJson() : Boolean

    fun readManifest(filepath : String): Result<Manifest, String>
    fun readElectionConfig(): Result<ElectionConfig, String>
    fun readElectionInitialized(): Result<ElectionInitialized, String>
    fun readTallyResult(): Result<TallyResult, String>
    fun readDecryptionResult(): Result<DecryptionResult, String>

    //// Use iterators, so that we never have to read in all objects at once.
    fun hasEncryptedBallots() : Boolean
    fun iterateEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    fun iterateCastBallots(): Iterable<EncryptedBallot>  // encrypted ballots that are CAST
    fun iterateSpoiledBallots(): Iterable<EncryptedBallot> // encrypted ballots that are SPOILED
    fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot>

    //// not part of the election record, private data
    // read plaintext ballots in given directory, with filter
    fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    // trustee in given directory for given guardianId
    fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF
}

fun makeConsumer(
    topDir: String,
    group: GroupContext,
    isJson: Boolean? = null, // false = protobuf, true = json; default: check if manifest.json file exists
): Consumer {
    val jsonSerialization = isJson ?: topDir.endsWith(".zip") ||
            pathExists("$topDir/${ElectionRecordJsonPaths.MANIFEST_FILE}")

    return if (jsonSerialization) {
        ConsumerJson(topDir, group)
    } else {
        ConsumerProto(topDir, group)
    }
}

// specify the manifest filename, or the directory that its in. May be JSON or proto. If JSON, may be zipped.
// TODO needs testing
fun readManifest(
    manifestDirOrFile: String,
    group: GroupContext,
): Result<Manifest, String> {
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

    return if (isJson) {
        ConsumerJson(manifestDir, group).readManifest(manifestFile)
    } else {
        ConsumerProto(manifestDir, group).readManifest(manifestFile)
    }
}