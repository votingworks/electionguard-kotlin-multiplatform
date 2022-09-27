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
import electionguard.decrypt.DecryptingTrusteeIF
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate

internal val logger = KotlinLogging.logger("ElectionRecord")

// implements the public API
actual class Consumer actual constructor(
    val topDir: String,
    val groupContext: GroupContext,
) {
    val path = ElectionRecordPath(topDir)

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
        }
    }

    actual fun topdir(): String {
        return this.topDir
    }

    actual fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(path.electionConfigPath())
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
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        if (!Files.exists(Path.of(path.plaintextBallotPath(ballotDir)))) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(groupContext, path.plaintextBallotPath(ballotDir), filter) }
    }

    // all submitted ballots, cast or spoiled
    actual fun iterateEncryptedBallots(
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        val filename = path.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(filename, groupContext, null, filter) }
    }

    actual fun hasEncryptedBallots(): Boolean {
        return Files.exists(Path.of(path.encryptedBallotPath()))
    }

    // only EncryptedBallot that are CAST
    actual fun iterateCastBallots(): Iterable<EncryptedBallot> {
        val filename = path.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val protoFilter =
            Predicate<electionguard.protogen.EncryptedBallot> { it.state == electionguard.protogen.EncryptedBallot.BallotState.CAST }
        return Iterable { EncryptedBallotIterator(filename, groupContext, protoFilter, null) }
    }

    // only EncryptedBallot that are SPOILED
    actual fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        val filename = path.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val protoFilter =
            Predicate<electionguard.protogen.EncryptedBallot> { it.state == electionguard.protogen.EncryptedBallot.BallotState.SPOILED }
        return Iterable { EncryptedBallotIterator(filename, groupContext, protoFilter, null) }
    }

    // all tallies in the file
    actual fun iterateSpoiledBallotTallies(): Iterable<DecryptedTallyOrBallot> {
        val filename = path.spoiledBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(filename, groupContext) }
    }

    actual fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = path.decryptingTrusteePath(trusteeDir, guardianId)
        return groupContext.readTrustee(filename)
    }

}