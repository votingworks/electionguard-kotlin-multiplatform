@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate

private val logger = KotlinLogging.logger("ConsumerJson")

/** Can read both zipped and unzipped election record */
actual class ConsumerJson actual constructor(val topDir: String, val group: GroupContext) : Consumer {
    var fileSystem = FileSystems.getDefault()
    var fileSystemProvider = fileSystem.provider()
    var jsonPaths = ElectionRecordJsonPaths(topDir)

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
        }
        if (topDir.endsWith(".zip")) {
            val filePath = Path.of(topDir)
            fileSystem = FileSystems.newFileSystem(filePath, emptyMap<String, String>())
            fileSystemProvider = fileSystem.provider()
            jsonPaths = ElectionRecordJsonPaths("")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = true

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(
            fileSystem.getPath(jsonPaths.electionConstantsPath()),
            fileSystem.getPath(jsonPaths.manifestPath()),
        )
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(
            fileSystem.getPath(jsonPaths.electionContextPath()),
            config.unwrap(),
        )
    }

    actual override fun readTallyResult(): Result<TallyResult, String> {
        val init = readElectionInitialized()
        if (init is Err) {
            return Err(init.error)
        }
        return readTallyResult(
            fileSystem.getPath(jsonPaths.encryptedTallyPath()),
            init.unwrap(),
        )
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, String> {
        val tally = readTallyResult()
        if (tally is Err) {
            return Err(tally.error)
        }

        return readDecryptionResult(
            fileSystem.getPath(jsonPaths.lagrangePath()),
            fileSystem.getPath(jsonPaths.decryptedTallyPath()),
            tally.unwrap()
        )
    }

    actual override fun hasEncryptedBallots(): Boolean {
        return Files.exists(fileSystem.getPath(jsonPaths.encryptedBallotDir()))
    }

    // all submitted ballots, with filter
    actual override fun iterateEncryptedBallots(
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        val dirPath = fileSystem.getPath(jsonPaths.encryptedBallotDir())
        if (!Files.exists(dirPath)) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(dirPath, group, filter) }
    }

    // only EncryptedBallot that are CAST
    actual override fun iterateCastBallots(): Iterable<EncryptedBallot> {
        return iterateEncryptedBallots { it.state == EncryptedBallot.BallotState.CAST }
    }

    // only EncryptedBallot that are SPOILED
    actual override fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        return iterateEncryptedBallots { it.state == EncryptedBallot.BallotState.SPOILED }
    }

    // decrypted spoiled ballots
    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val dirPath = fileSystem.getPath(jsonPaths.decryptedBallotDir())
        if (!Files.exists(dirPath)) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(dirPath, group) }
    }

    // plaintext ballots in given directory, with filter
    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        val dirPath = fileSystem.getPath(ballotDir)
        if (!Files.exists(dirPath)) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(dirPath, filter) }
    }

    // trustee in given directory for given guardianId
    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        return readTrustee(fileSystem.getPath(filename)).unwrap()
    }

    //////// The low level reading functions for protobuf

    private fun readElectionConfig(constantsFile: Path, manifestFile: Path): Result<ElectionConfig, String> {
        return try {
            var constants: ElectionConstants
            fileSystemProvider.newInputStream(constantsFile).use { inp ->
                val json = Json.decodeFromStream<ConstantsJson>(inp)
                constants = json.import()
            }

            var manifest: Manifest
            fileSystemProvider.newInputStream(manifestFile).use { inp ->
                val json = Json.decodeFromStream<ManifestJson>(inp)
                manifest = json.import()
            }

            Ok(
                ElectionConfig(
                    constants,
                    manifest,
                    1, // LOOK these are not in JSON, can we pass them in?
                    1, // LOOK not in JSON
                )
            )
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $constantsFile failed")
        }
    }

    private fun readElectionInitialized(
        contextFile: Path,
        config: ElectionConfig
    ): Result<ElectionInitialized, String> {
        return try {
            val guardiansPath = fileSystem.getPath(jsonPaths.guardianDir())
            val (guardians, errors) = readGuardians(guardiansPath).partition()
            if (errors.isNotEmpty()) {
                return Err(errors.joinToString("\n"))
            } else {
                var context: ElectionInitialized
                fileSystemProvider.newInputStream(contextFile).use { inp ->
                    val json = Json.decodeFromStream<ContextJson>(inp)
                    context = json.import(group, config, guardians)
                }
                Ok(context)
            }
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $contextFile failed")
        }
    }

    private fun readGuardians(guardianDir: Path): List<Result<Guardian, String>> {
        val result = guardianDir.pathList().map {
            fileSystemProvider.newInputStream(it).use { inp ->
                val json = Json.decodeFromStream<GuardianJson>(inp)
                json.import(group)
            }
        }
        return result
    }

    private fun readTallyResult(filename: Path, init: ElectionInitialized): Result<TallyResult, String> {
        return try {
            fileSystemProvider.newInputStream(filename).use { inp ->
                val json = Json.decodeFromStream<EncryptedTallyJson>(inp)
                val tallyResult = json.import(group)
                if (tallyResult is Err)
                    Err(tallyResult.unwrapError())
                else
                    Ok(TallyResult(init, tallyResult.unwrap(), emptyList(), emptyList()))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    private fun readDecryptionResult(
        lagrangePath: Path,
        decryptedTallyPath: Path,
        tallyResult: TallyResult
    ): Result<DecryptionResult, String> {
        // all the coefficients in a map in one file
        var lagrangeCoordinates: List<LagrangeCoordinate>
        fileSystemProvider.newInputStream(lagrangePath).use { inp ->
            val json = Json.decodeFromStream<CoefficientsJson>(inp)
            lagrangeCoordinates = json.import(group)
        }

        return try {
            fileSystemProvider.newInputStream(decryptedTallyPath).use { inp ->
                val json = Json.decodeFromStream<DecryptedTallyJson>(inp)
                val dtallyResult = json.import(group)
                if (dtallyResult is Err)
                    Err(dtallyResult.unwrapError())
                else
                    Ok(DecryptionResult(tallyResult, dtallyResult.unwrap(), lagrangeCoordinates))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $decryptedTallyPath failed")
        }
    }

    private inner class PlaintextBallotIterator(
        ballotDir: Path,
        private val filter: Predicate<PlaintextBallot>?
    ) : AbstractIterator<PlaintextBallot>() {
        val pathList = ballotDir.pathList()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file).use { inp ->
                    val json = Json.decodeFromStream<PlaintextBallotJson>(inp)
                    val ballotResult = json.import()
                    if (ballotResult is Err) {
                        logger.atWarn().log { "Failed to open ${file} error = ${ballotResult.unwrapError()}" }
                    } else {
                        val ballot = ballotResult.unwrap()
                        if (filter == null || filter.test(ballot)) {
                            setNext(ballot)
                            return
                        }
                    }
                }
            }
            return done()
        }
    }

    private inner class EncryptedBallotIterator(
        ballotDir: Path,
        private val group: GroupContext,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {
        val pathList = ballotDir.pathList()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file).use { inp ->
                    val json = Json.decodeFromStream<SubmittedBallotJson>(inp)
                    val ballotResult = json.import(group)
                    if (ballotResult is Err) {
                        logger.atWarn().log { "Failed to open ${file} error = ${ballotResult.unwrapError()}" }
                    } else {
                        val ballot = ballotResult.unwrap()
                        if (filter == null || filter.test(ballot)) {
                            setNext(ballot)
                            return
                        }
                    }
                }
            }
            return done()
        }
    }

    private inner class SpoiledBallotTallyIterator(
        ballotDir: Path,
        private val group: GroupContext,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        val pathList = ballotDir.pathList()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file).use { inp ->
                    val json = Json.decodeFromStream<DecryptedTallyJson>(inp)
                    val tallyResult = json.import(group)
                    if (tallyResult is Err) {
                        logger.atWarn().log { "Failed to open ${file} error = ${tallyResult.unwrapError()}" }
                    } else {
                        setNext(tallyResult.unwrap())
                        return
                    }
                }
            }
            return done()
        }
    }

    private fun readTrustee(filePath: Path): Result<DecryptingTrusteeDoerre, String> {
        return try {
            fileSystemProvider.newInputStream(filePath).use { inp ->
                val json = Json.decodeFromStream<DecryptingTrusteeJson>(inp)
                val trusteeResult = json.import(group)
                if (trusteeResult is Err)
                    Err(trusteeResult.unwrapError())
                else
                    Ok(trusteeResult.unwrap())
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filePath failed")
        }
    }

    private fun Path.pathList(): List<Path> {
        return Files.walk(this, 1).use { fileStream ->
            fileStream.filter { it != this }.toList()
        }
    }
}