package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

actual class ElectionRecord actual constructor(
    val topDir: String,
    val groupContext: GroupContext,
) {
    val path = ElectionRecordPath(topDir)

    init {
        if (!exists(topDir)) {
            throw RuntimeException("Non existent directory $topDir")
        }
    }

    actual fun topdir(): String {
        return this.topDir
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

    actual fun iterateEncryptedBallots(filter : (EncryptedBallot) -> Boolean): Iterable<EncryptedBallot> {
        if (!exists(path.encryptedBallotPath())) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(groupContext, path.encryptedBallotPath(), null, null) }
    }

    actual fun iterateCastBallots(): Iterable<EncryptedBallot> {
        if (!exists(path.encryptedBallotPath())) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(groupContext, path.encryptedBallotPath(),
            { it.state === electionguard.protogen.EncryptedBallot.BallotState.CAST }, null)
        }
    }

    actual fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        if (!exists(path.encryptedBallotPath())) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(groupContext, path.encryptedBallotPath(),
            { it.state === electionguard.protogen.EncryptedBallot.BallotState.SPOILED }, null)
        }
    }

    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        if (!exists(path.spoiledBallotPath())) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(groupContext, path.spoiledBallotPath())}
    }

    actual fun iteratePlaintextBallots(ballotDir : String, filter : (PlaintextBallot) -> Boolean): Iterable<PlaintextBallot> {
        return Iterable { PlaintextBallotIterator(groupContext, path.plaintextBallotPath(ballotDir), filter) }
    }

    actual fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = path.decryptingTrusteePath(trusteeDir, guardianId)
        return groupContext.readTrustee(filename)
    }

}