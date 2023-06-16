package electionguard.publish

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Guardian
import electionguard.ballot.Manifest
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json.CoefficientsJson
import electionguard.json.ConstantsJson
import electionguard.json.ContextJson
import electionguard.json.DecryptedTallyJson
import electionguard.json.DecryptingTrusteeJson
import electionguard.json.EncryptedTallyJson
import electionguard.json.GuardianJson
import electionguard.json.ManifestJson
import electionguard.json.PlaintextBallotJson
import electionguard.json.SubmittedBallotJson
import electionguard.json.import
import kotlinx.cinterop.toKString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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

    actual override fun readManifest(filepath : String): Result<Manifest, String> {
        return try {
            val manifestJson = jsonFormat.decodeFromString<ManifestJson>(gulp(filepath).toKString())
            val manifest = manifestJson.import()
            Ok(manifest)
        } catch (e: Exception) {
            Err(e.message ?: "readManifest $filepath failed")
        }
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(jsonPaths.electionConstantsPath(), jsonPaths.manifestPath())
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(jsonPaths.electionContextPath(), config.unwrap())
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

    fun readElectionConfig(constantsFile: String, manifestFile: String): Result<ElectionConfig, String> {
        return try {
            val constantsJson = jsonFormat.decodeFromString<ConstantsJson>(gulp(constantsFile).toKString())
            val electionConstants = constantsJson.import()

            val manifestJson = jsonFormat.decodeFromString<ManifestJson>(gulp(manifestFile).toKString())
            val manifest = manifestJson.import()

            Ok(
                ElectionConfig(
                    "TODOtoo",
                    electionConstants,
                    ByteArray(0),
                    manifest,
                    1, // LOOK not in JSON
                    1, // LOOK not in JSON
                    "N/A",
                    "N/A",                )
            )
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $constantsFile failed")
        }
    }

    fun readElectionInitialized(contextFile: String, config: ElectionConfig): Result<ElectionInitialized, String> {
        return try {
            val (guardians, errors) = readGuardians().partition()
            if (errors.isNotEmpty()) {
                return Err(errors.joinToString("\n"))
            } else {
                val contextJson = jsonFormat.decodeFromString<ContextJson>(gulp(contextFile).toKString())
                Ok(contextJson.import(group, config, guardians))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $contextFile failed")
        }
    }

    private fun readGuardians(): List<Result<Guardian, String>> {
        val fileList = openDir(jsonPaths.guardianDir())
        return fileList.map {
            val filename = "${jsonPaths.guardianDir()}/$it"
            val json = jsonFormat.decodeFromString<GuardianJson>(gulp(filename).toKString())
            json.import(group)
        }
    }

    fun readTallyResult(filename: String, init: ElectionInitialized): Result<TallyResult, String> {
        return try {
            val json = jsonFormat.decodeFromString<EncryptedTallyJson>(gulp(filename).toKString())
            val tallyResult = json.import(group)
            if (tallyResult is Err)
                Err(tallyResult.unwrapError())
            else
                Ok(TallyResult(init, tallyResult.unwrap(), emptyList(), emptyList()))
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    fun readDecryptionResult(filename: String, tallyResult: TallyResult): Result<DecryptionResult, String> {
        // all the coefficients in a map in one file
        val jsonCoeff = jsonFormat.decodeFromString<CoefficientsJson>(gulp(jsonPaths.lagrangePath()).toKString())
        val lagrangeCoordinates = jsonCoeff.import(group)

        return try {
            val json = jsonFormat.decodeFromString<DecryptedTallyJson>(gulp(filename).toKString())
            val dtallyResult = json.import(group)
            if (dtallyResult is Err)
                Err(dtallyResult.unwrapError())
            else
                Ok(DecryptionResult(tallyResult, dtallyResult.unwrap(), lagrangeCoordinates))
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
                val ballotResult = json.import()
                if (ballotResult is Err) {
                    logger.warn { "Failed to open ${filename} error = ${ballotResult.unwrapError()}" }
                } else {
                    val ballot = ballotResult.unwrap()
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
                val json = jsonFormat.decodeFromString<SubmittedBallotJson>(gulp(filename).toKString())
                val ballotResult = json.import(group)
                if (ballotResult is Err) {
                    logger.warn { "Failed to open ${filename} error = ${ballotResult.unwrapError()}" }
                } else {
                    val ballot = ballotResult.unwrap()
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
                val json = jsonFormat.decodeFromString<DecryptedTallyJson>(gulp(filename).toKString())
                val ballotResult = json.import(group)
                if (ballotResult is Err) {
                    logger.warn { "Failed to open ${filename} error = ${ballotResult.unwrapError()}" }
                } else {
                    setNext(ballotResult.unwrap())
                    return
                }
            }
            return done()
        }
    }

    private fun readTrustee(filename: String): Result<DecryptingTrusteeDoerre, String> {
        return try {
            val json = jsonFormat.decodeFromString<DecryptingTrusteeJson>(gulp(filename).toKString())
            val trusteeResult = json.import(group)
            if (trusteeResult is Err)
                Err(trusteeResult.unwrapError())
            else
                Ok(trusteeResult.unwrap())
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }

}