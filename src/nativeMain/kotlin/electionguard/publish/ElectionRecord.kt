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

actual class ElectionRecord actual constructor(
    topDir: String,
    groupContext: GroupContext,
    publisherMode: PublisherMode
) {
    actual fun readElectionConfig(): Result<ElectionConfig, String> {
        TODO("Not yet implemented")
    }

    actual fun readElectionInitialized(): Result<ElectionInitialized, String> {
        TODO("Not yet implemented")
    }

    actual fun readTallyResult(): Result<TallyResult, String> {
        TODO("Not yet implemented")
    }

    actual fun readDecryptionResult(): Result<DecryptionResult, String> {
        TODO("Not yet implemented")
    }

    actual fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        TODO("Not yet implemented")
    }

    actual fun iterateCastBallots(): Iterable<SubmittedBallot> {
        TODO("Not yet implemented")
    }

    actual fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        TODO("Not yet implemented")
    }

    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        TODO("Not yet implemented")
    }

    actual fun iteratePlaintextBallots(
        ballotDir: String,
        filter: (PlaintextBallot) -> Boolean
    ): Iterable<PlaintextBallot> {
        TODO("Not yet implemented")
    }

    actual fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF> {
        TODO("Not yet implemented")
    }

    actual fun writeElectionConfig(config: ElectionConfig) {
    }

    actual fun writeElectionInitialized(init: ElectionInitialized) {
    }

    actual fun writeEncryptions(
        init: ElectionInitialized,
        encrypted: Iterable<SubmittedBallot>
    ) {
    }

    actual fun writeTallyResult(tally: TallyResult) {
    }

    actual fun writeDecryptionResult(decryption: DecryptionResult) {
    }

    actual fun submittedBallotSink(): SubmittedBallotSinkIF {
        TODO("Not yet implemented")
    }

    actual fun writeInvalidBallots(
        invalidDir: String,
        invalidBallots: List<PlaintextBallot>
    ) {
    }

}