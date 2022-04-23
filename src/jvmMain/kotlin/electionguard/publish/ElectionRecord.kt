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
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate

internal val logger = KotlinLogging.logger("ElectionRecord")

actual class ElectionRecord actual constructor(
    topDir: String,
    val groupContext: GroupContext,
) {
    private val electionRecordDir = Path.of(topDir).resolve(ElectionRecordPath.ELECTION_RECORD_DIR)
    val path = ElectionRecordPath(topDir)

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
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

    // all plaintext ballots
    actual fun iteratePlaintextBallots(
        ballotDir: String,
        filter: (PlaintextBallot) -> Boolean
    ): Iterable<PlaintextBallot> {
        if (!Files.exists(Path.of(path.plaintextBallotProtoPath(ballotDir)))) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(path.plaintextBallotProtoPath(ballotDir), filter) }
    }

    // all submitted ballots, cast or spoiled
    actual fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        val filename = path.submittedBallotProtoPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { SubmittedBallotIterator(filename, groupContext) { true } }
    }

    // only cast SubmittedBallots
    actual fun iterateCastBallots(): Iterable<SubmittedBallot> {
        val filename = path.submittedBallotProtoPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val filter =
            Predicate<electionguard.protogen.SubmittedBallot> { it.state == electionguard.protogen.SubmittedBallot.BallotState.CAST }
        return Iterable { SubmittedBallotIterator(filename, groupContext, filter) }
    }

    // only spoiled SubmittedBallots
    actual fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        val filename = path.submittedBallotProtoPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val filter =
            Predicate<electionguard.protogen.SubmittedBallot> { it.state == electionguard.protogen.SubmittedBallot.BallotState.SPOILED }
        return Iterable { SubmittedBallotIterator(filename, groupContext, filter) }
    }

    // all spoiled ballot tallies
    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        val filename = path.spoiledBallotProtoPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(filename, groupContext) }
    }

    actual fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF> {
        return readTrustees(groupContext, trusteeDir)
    }

}