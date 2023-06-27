package electionguard.publish

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.ballot.ElectionConfig
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json2.*
import kotlinx.cinterop.toKString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// TODO convert to 1.9

actual class ConsumerJson actual constructor(private val topDir: String, private val group: GroupContext) : Consumer {
    val jsonPaths = ElectionRecordJsonPaths(topDir)
    val jsonFormat = Json { prettyPrint = true }

    init {
        if (!exists(topDir)) {
            throw RuntimeException("Non existent directory $topDir")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = true

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        val json = jsonFormat.decodeFromString<ManifestJson>(manifestBytes.toKString())
        return json.import()
    }

    actual override fun readManifestBytes(filename : String): ByteArray {
        return gulp(filename)
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(jsonPaths.electionConstantsPath(), jsonPaths.manifestPath(), jsonPaths.electionConfigPath())
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(jsonPaths.electionInitializedPath(), config.unwrap())
    }

    actual override fun readTallyResult(): Result<TallyResult, String> {
        val init = readElectionInitialized()
        if (init is Err) {
            return Err(init.error)
        }
        return readTallyResult(jsonPaths.encryptedTallyPath(), init.unwrap())
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, String> {
        val tally = readTallyResult()
        if (tally is Err) {
            return Err(tally.error)
        }
        return readDecryptionResult(jsonPaths.decryptedTallyPath(), tally.unwrap())
    }

    actual override fun hasEncryptedBallots(): Boolean {
        return exists(jsonPaths.encryptedBallotDir()) // LOOK check if its empty?
    }

    actual override fun iterateEncryptedBallots(filter: ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        val dirname = jsonPaths.encryptedBallotDir()
        if (!exists(dirname)) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(dirname, filter) }
    }

    actual override fun iterateCastBallots(): Iterable<EncryptedBallot> {
        val dirname = jsonPaths.encryptedBallotDir()
        if (!exists(dirname)) {
            return emptyList()
        }
        return Iterable {
            EncryptedBallotIterator(dirname)
            { it.state === EncryptedBallot.BallotState.CAST }
        }
    }

    actual override fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        val dirname = jsonPaths.encryptedBallotDir()
        if (!exists(dirname)) {
            return emptyList()
        }
        return Iterable {
            EncryptedBallotIterator(dirname)
            { it.state === EncryptedBallot.BallotState.SPOILED }
        }
    }

    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val dirname = jsonPaths.decryptedBallotDir()
        if (!exists(dirname)) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(group, dirname) }
    }

    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        if (!exists(ballotDir)) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(ballotDir, filter) }
    }

    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        return readTrustee(filename).unwrap()
    }

    ////////////////////////////////////////////////////////////////////////

    fun readElectionConfig(constantsFile: String, manifestFilename: String, configFile: String,): Result<ElectionConfig, String> {
        return try {
            val constantsJson = jsonFormat.decodeFromString<ElectionConstantsJson>(gulp(constantsFile).toKString())
            val electionConstants = constantsJson.import()

            Ok(
                makeElectionConfig(
                    "TODOtoo",
                    electionConstants,
                    1, // LOOK not in JSON
                    1, // LOOK not in JSON
                    "N/A",
                    "N/A",
                    ByteArray(0), // TODO manifest
                )
            )
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $constantsFile failed")
        }
    }

    fun readElectionInitialized(contextFile: String, config: ElectionConfig): Result<ElectionInitialized, String> {
        return try {
            val initJson: ElectionInitializedJson = jsonFormat.decodeFromString<ElectionInitializedJson>(gulp(contextFile).toKString())
            Ok(initJson.import(group, config))
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $contextFile failed")
        }
    }

    fun readTallyResult(filename: String, init: ElectionInitialized): Result<TallyResult, String> {
        return try {
            val json = jsonFormat.decodeFromString<EncryptedTallyJson>(gulp(filename).toKString())
            val tallyResult: EncryptedTally = json.import(group)
            if (tallyResult == null)
                Err("failed to read EncryptedTallyJson")
            else
                Ok(TallyResult(init, tallyResult, emptyList(), emptyList()))
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    fun readDecryptionResult(filename: String, tallyResult: TallyResult): Result<DecryptionResult, String> {
        return try {
            val json = jsonFormat.decodeFromString<DecryptedTallyOrBallotJson>(gulp(filename).toKString())
            val dtallyResult = json.import(group)
            if (dtallyResult == null)
                Err("failed to read DecryptedTallyOrBallotJson $filename")
            else
                Ok(DecryptionResult(tallyResult, dtallyResult))
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }

    inner class PlaintextBallotIterator(
        val dirname: String,
        val filter: ((PlaintextBallot) -> Boolean)?,
    ) : AbstractIterator<PlaintextBallot>() {
        val fileList = openDir(dirname)
        var idx = 0

        override fun computeNext() {
            while (idx < fileList.size) {
                val filename = "$dirname/${fileList[idx++]}"
                val json = jsonFormat.decodeFromString<PlaintextBallotJson>(gulp(filename).toKString())
                val ballot = json.import()
                if (ballot == null) {
                    Err("failed to read PlaintextBallotJson ${filename}")
                } else {
                    if (filter == null || filter!!(ballot)) {
                        setNext(ballot)
                        return
                    }
                }
            }
            return done()
        }
    }

    inner class EncryptedBallotIterator(
        val dirname: String,
        private val filter: ((EncryptedBallot) -> Boolean)?,
    ) : AbstractIterator<EncryptedBallot>() {
        val fileList = openDir(dirname)
        var idx = 0

        override fun computeNext() {
            while (idx < fileList.size) {
                val filename = "$dirname/${fileList[idx++]}"
                val json = jsonFormat.decodeFromString<EncryptedBallotJson>(gulp(filename).toKString())
                val ballot = json.import(group)
                if (ballot == null) {
                    logger.warn { "Failed to read EncryptedBallotJson ${filename}" }
                } else {
                    if (filter == null || filter!!(ballot)) {
                        setNext(ballot)
                        return
                    }
                }
            }
            return done()
        }
    }

    inner class SpoiledBallotTallyIterator(
        private val group: GroupContext,
        private val dirname: String,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        val fileList = openDir(dirname)
        var idx = 0

        override fun computeNext() {
            while (idx < fileList.size) {
                val filename = "$dirname/${fileList[idx++]}"
                val json = jsonFormat.decodeFromString<DecryptedTallyOrBallotJson>(gulp(filename).toKString())
                val ballot = json.import(group)
                if (ballot == null) {
                    Err("failed to read DecryptedTallyOrBallotJson $filename")
                } else {
                    setNext(ballot)
                    return
                }
            }
            return done()
        }
    }

    private fun readTrustee(filename: String): Result<DecryptingTrusteeDoerre, String> {
        return try {
            val json = jsonFormat.decodeFromString<TrusteeJson>(gulp(filename).toKString())
            val trusteeResult = json.importDecryptingTrustee(group)
            if (trusteeResult == null)
                Err("failed to read DecryptingTrustee $filename")
            else
                Ok(trusteeResult)
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }

}