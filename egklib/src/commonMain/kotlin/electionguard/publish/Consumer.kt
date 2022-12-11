package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.fileExists
import electionguard.decrypt.DecryptingTrusteeIF

/** public API to read from the election record */
interface Consumer {
    fun topdir() : String
    fun isJson() : Boolean

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
    group: GroupContext, // false = create directories if not already exist, true = create clean directories,
    isJson: Boolean? = null, // false = protobuf, true = json; default check if manifest.json file exists
): Consumer {
    val jsonSerialization = isJson?:
        fileExists("$topDir/${ElectionRecordJsonPaths.MANIFEST_FILE}") || topDir.endsWith(".zip")

    return if (jsonSerialization) ConsumerJson(topDir, group) else ConsumerProto(topDir, group)
}