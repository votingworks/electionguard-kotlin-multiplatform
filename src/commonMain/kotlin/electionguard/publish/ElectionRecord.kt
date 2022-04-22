package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

expect class ElectionRecord(
    topDir: String,
    groupContext: GroupContext,
    publisherMode: PublisherMode = PublisherMode.readonly) {

    fun readElectionConfig(): Result<ElectionConfig, String>
    fun readElectionInitialized(): Result<ElectionInitialized, String>
    fun readTallyResult(): Result<TallyResult, String>
    fun readDecryptionResult(): Result<DecryptionResult, String>

    // Use iterators, so that we never have to read in all objects at once.
    fun iterateSubmittedBallots(): Iterable<SubmittedBallot>
    fun iterateCastBallots(): Iterable<SubmittedBallot>
    fun iterateSpoiledBallots(): Iterable<SubmittedBallot>
    fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally>

    // not part of the election record, private data
    fun iteratePlaintextBallots(ballotDir : String, filter : (PlaintextBallot) -> Boolean): Iterable<PlaintextBallot>
    fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF>

    fun writeElectionConfig(config: ElectionConfig)
    fun writeElectionInitialized(init: ElectionInitialized)
    fun writeEncryptions(init: ElectionInitialized, encrypted: Iterable<SubmittedBallot>)
    fun writeTallyResult(tally: TallyResult)
    fun writeDecryptionResult(decryption: DecryptionResult)

    fun submittedBallotSink(): SubmittedBallotSinkIF

    fun writeInvalidBallots(invalidDir: String, invalidBallots: List<PlaintextBallot>)
}

interface SubmittedBallotSinkIF {
    fun writeSubmittedBallot(ballot: SubmittedBallot)
    fun close()
}

enum class PublisherMode {
    readonly, // read files only
    writeonly, // write new files, but do not create directories
    createIfMissing, // create directories if not already exist
    createNew // create clean directories
}