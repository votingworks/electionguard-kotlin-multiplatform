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
    val groupContext: GroupContext,
) {
    val path = ElectionRecordPath(topDir)

    init {
        if (!exists(topDir)) {
            throw RuntimeException("Non existent directory $topDir")
        }
    }

    actual fun readElectionConfig(): Result<ElectionConfig, String> {
        return groupContext.readElectionConfig(path.electionConfigPath())
    }

    actual fun readElectionInitialized(): Result<ElectionInitialized, String> {
        return groupContext.readElectionInitialized(path.electionInitializedPath())
    }

    actual fun readTallyResult(): Result<TallyResult, String> {
        return groupContext.readTallyResult(path.tallyResultPath())
    }

    actual fun readDecryptionResult(): Result<DecryptionResult, String> {
        return groupContext.readDecryptionResult(path.decryptionResultPath())
    }

    actual fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        if (!exists(path.submittedBallotProtoPath())) {
            return emptyList()
        }
        return Iterable { SubmittedBallotIterator(groupContext, path.submittedBallotProtoPath()) { true } }
    }

    actual fun iterateCastBallots(): Iterable<SubmittedBallot> {
        if (!exists(path.submittedBallotProtoPath())) {
            return emptyList()
        }
        return Iterable { SubmittedBallotIterator(groupContext, path.submittedBallotProtoPath())
            { it.state === electionguard.protogen.SubmittedBallot.BallotState.CAST }
        }
    }

    actual fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        if (!exists(path.submittedBallotProtoPath())) {
            return emptyList()
        }
        return Iterable { SubmittedBallotIterator(groupContext, path.submittedBallotProtoPath())
            { it.state === electionguard.protogen.SubmittedBallot.BallotState.SPOILED }
        }
    }

    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        if (!exists(path.spoiledBallotProtoPath())) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(groupContext, path.spoiledBallotProtoPath())}
    }

    actual fun iteratePlaintextBallots(ballotDir : String, filter : (PlaintextBallot) -> Boolean): Iterable<PlaintextBallot> {
        return Iterable { PlaintextBallotIterator(path.plaintextBallotProtoPath(ballotDir), filter) }
    }

    actual fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF> {
        return readTrustees(groupContext, trusteeDir)
    }

}