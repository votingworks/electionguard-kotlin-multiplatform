package electionguard.publish

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

// implements the public API
actual class ConsumerProto actual constructor(private val topDir: String, private val groupContext: GroupContext) : Consumer {
    private val path = ElectionRecordProtoPaths(topDir)

    init {
        if (!exists(topDir)) {
            throw RuntimeException("Non existent directory $topDir")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = false

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        return makeManifestInternal(manifestBytes).component1()!!
    }

    actual override fun readManifestBytes(filename : String): ByteArray {
        return gulp(filename)
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return try {
            readElectionConfig(path.electionConfigPath())
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig ${path.electionConfigPath()} error")
        }
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        return try {
            groupContext.readElectionInitialized(path.electionInitializedPath())
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized ${path.electionInitializedPath()} error")
        }
    }

    actual override fun encryptingDevices(): List<String> = emptyList()
    actual override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, String> = Err("not implemented")
    actual override fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> = emptyList()
    actual override fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> = emptyList()

    actual override fun readTallyResult(): Result<TallyResult, String> {
        return try {
            groupContext.readTallyResult(path.tallyResultPath())
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult ${path.tallyResultPath()} error")
        }
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, String> {
        return try {
            groupContext.readDecryptionResult(path.decryptionResultPath())
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult ${path.decryptionResultPath()} error")
        }
    }

    actual override fun hasEncryptedBallots(): Boolean {
        return exists(path.encryptedBallotPath())
    }

    actual override fun iterateEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        if (!exists(path.encryptedBallotPath())) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(groupContext, path.encryptedBallotPath(), null, filter) }
    }

    actual override fun iterateCastBallots(): Iterable<EncryptedBallot> {
        if (!exists(path.encryptedBallotPath())) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(groupContext, path.encryptedBallotPath(),
            { it.state === electionguard.protogen.EncryptedBallot.BallotState.CAST }, null)
        }
    }

    actual override fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        if (!exists(path.encryptedBallotPath())) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(groupContext, path.encryptedBallotPath(),
            { it.state === electionguard.protogen.EncryptedBallot.BallotState.SPOILED }, null)
        }
    }

    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        if (!exists(path.spoiledBallotPath())) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(groupContext, path.spoiledBallotPath())}
    }

    actual override fun iteratePlaintextBallots(
        ballotDir : String,
        filter : ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        return Iterable { PlaintextBallotIterator(path.plaintextBallotPath(ballotDir), filter) }
    }

    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = path.decryptingTrusteePath(trusteeDir, guardianId)
        return groupContext.readTrustee(filename)
    }

}