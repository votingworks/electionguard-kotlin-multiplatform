@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.fileReadBytes
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json2.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.io.ByteArrayInputStream
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
            val wtf = fileSystem.rootDirectories
            wtf.forEach { root ->
                Files.walk(root).forEach { path -> println(path) }
            }
            fileSystemProvider = fileSystem.provider()
            jsonPaths = ElectionRecordJsonPaths("")

            println("electionConstantsPath = ${jsonPaths.electionConstantsPath()} -> ${fileSystem.getPath(jsonPaths.electionConstantsPath())}")
            println("manifestPath = ${jsonPaths.manifestPath()} -> ${fileSystem.getPath(jsonPaths.manifestPath())}")
            println("electionConfigPath = ${jsonPaths.electionConfigPath()} -> ${fileSystem.getPath(jsonPaths.electionConfigPath())}")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = true

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        ByteArrayInputStream(manifestBytes).use { inp ->
            val json = Json.decodeFromStream<ManifestJson>(inp)
            return json.import()
        }
    }

    actual override fun readManifestBytes(filename : String): ByteArray {
        return fileReadBytes(filename)
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(
            fileSystem.getPath(jsonPaths.electionConstantsPath()),
            fileSystem.getPath(jsonPaths.manifestPath()),
            fileSystem.getPath(jsonPaths.electionConfigPath()),
        )
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(
            fileSystem.getPath(jsonPaths.electionInitializedPath()),
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

    private fun readElectionConfig(constantsFile: Path, manifestFile: Path, configFile: Path,): Result<ElectionConfig, String> {
        return try {
            var constants: ElectionConstants
            fileSystemProvider.newInputStream(constantsFile).use { inp ->
                val json = Json.decodeFromStream<ElectionConstantsJson>(inp)
                constants = json.import()
            }

            // need to use fileSystemProvider for zipped files
            val manifestBytes =
            fileSystemProvider.newInputStream(manifestFile).use { inp ->
                inp.readAllBytes()
            }

            // val manifestBytes = fileReadBytes(manifestFile.toString())

            var electionConfig: ElectionConfig
            fileSystemProvider.newInputStream(configFile).use { inp ->
                val json = Json.decodeFromStream<ElectionConfigJson>(inp)
                electionConfig = json.import(constants, manifestBytes)
            }
            Ok(electionConfig)
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $configFile failed")
        }
    }

    private fun readElectionInitialized(
        contextFile: Path,
        config: ElectionConfig
    ): Result<ElectionInitialized, String> {
        return try {
            var electionInitialized: ElectionInitialized
            fileSystemProvider.newInputStream(contextFile).use { inp ->
                val json = Json.decodeFromStream<ElectionInitializedJson>(inp)
                electionInitialized = json.import(group, config)
            }
            Ok(electionInitialized)
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $contextFile failed")
        }
    }

    private fun readTallyResult(filename: Path, init: ElectionInitialized): Result<TallyResult, String> {
        return try {
            fileSystemProvider.newInputStream(filename).use { inp ->
                val json = Json.decodeFromStream<EncryptedTallyJson>(inp)
                val wncryptedTally = json.import(group)
                Ok(TallyResult(init, wncryptedTally, emptyList(), emptyList()))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    private fun readDecryptionResult(
        decryptedTallyPath: Path,
        tallyResult: TallyResult
    ): Result<DecryptionResult, String> {
        // all the coefficients in a map in one file
        return try {
            fileSystemProvider.newInputStream(decryptedTallyPath).use { inp ->
                val json = Json.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                val decryptedTallyOrBallot = json.import(group)
                Ok(DecryptionResult(tallyResult, decryptedTallyOrBallot))
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
                    val plaintextBallot = json.import()
                    if (filter == null || filter.test(plaintextBallot)) {
                        setNext(plaintextBallot)
                        return
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
                    val json = Json.decodeFromStream<EncryptedBallotJson>(inp)
                    val encryptedBallot = json.import(group)
                    if (filter == null || filter.test(encryptedBallot)) {
                        setNext(encryptedBallot)
                        return
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
                    val json = Json.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                    val decryptedTallyOrBallot = json.import(group)
                    setNext(decryptedTallyOrBallot)
                    return
                }
            }
            return done()
        }
    }

    private fun readTrustee(filePath: Path): Result<DecryptingTrusteeDoerre, String> {
        return try {
            fileSystemProvider.newInputStream(filePath).use { inp ->
                val json = Json.decodeFromStream<TrusteeJson>(inp)
                val decryptingTrustee = json.importDecryptingTrustee(group)
                Ok(decryptingTrustee)
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