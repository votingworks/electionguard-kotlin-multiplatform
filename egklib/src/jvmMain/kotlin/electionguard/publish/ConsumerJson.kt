@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json2.*
import electionguard.pep.BallotPep
import electionguard.pep.BallotPepJson
import electionguard.pep.import
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileAttributeView
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.spi.FileSystemProvider
import java.util.function.Predicate
import java.util.stream.Stream
import kotlin.io.path.isDirectory

private val logger = KotlinLogging.logger("ConsumerJsonJvm")

/** Can read both zipped and unzipped JSON election record */
actual class ConsumerJson actual constructor(val topDir: String, val group: GroupContext) : Consumer {
    var fileSystem : FileSystem = FileSystems.getDefault()
    var fileSystemProvider : FileSystemProvider = fileSystem.provider()
    var jsonPaths = ElectionRecordJsonPaths(topDir)
    val jsonIgnoreNulls = Json { explicitNulls = false }

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
        // need to use fileSystemProvider for zipped files
        val manifestPath = fileSystem.getPath(filename)
        val manifestBytes =
            fileSystemProvider.newInputStream(manifestPath, StandardOpenOption.READ).use { inp ->
                inp.readAllBytes()
            }
        return manifestBytes
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

    ///////////////////////////////////////////////////////////////////////////

    actual override fun encryptingDevices(): List<String> {
        val topBallotPath = Path.of(jsonPaths.encryptedBallotDir())
        if (!Files.exists(topBallotPath)) {
            return emptyList()
        }
        val deviceDirs: Stream<Path> = Files.list(topBallotPath)
        return deviceDirs.map { it.getName( it.nameCount - 1).toString() }.toList() // last name in the path
    }

    actual override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, String> {
        val ballotChainPath = Path.of(jsonPaths.encryptedBallotChain(device))
        if (!Files.exists(ballotChainPath)) {
            return Err("readEncryptedBallotChain path '$ballotChainPath' does not exist")
        }
        return try {
            var chain: EncryptedBallotChain
            fileSystemProvider.newInputStream(ballotChainPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<EncryptedBallotChainJson>(inp)
                chain = json.import()
            }
            Ok(chain)
        } catch (e: Exception) {
            Err("error ${e.message}")
        }
    }

    actual override fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> {
        val deviceDirPath = Path.of(jsonPaths.encryptedBallotDir(device))
        if (!Files.exists(deviceDirPath)) {
            throw RuntimeException("ConsumerJson.iterateEncryptedBallots: $deviceDirPath doesnt exist")
        }
        val chainResult = readEncryptedBallotChain(device)
        if (chainResult is Ok) {
            val chain = chainResult.unwrap()
            return Iterable { EncryptedBallotDeviceIterator(device, chain.ballotIds.iterator(), filter) }
        }
        // just read individual files
        return Iterable { EncryptedBallotFileIterator(deviceDirPath, group, filter) }
    }

    private inner class EncryptedBallotDeviceIterator(
        val device: String,
        val ballotIds: Iterator<String>,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {

        override fun computeNext() {
            while (true) {
                if (ballotIds.hasNext()) {
                    val ballotFilePath : Path = Path.of(jsonPaths.encryptedBallotDevicePath(device, ballotIds.next()))
                    try {
                        val encryptedBallot = readEncryptedBallot(ballotFilePath)
                        if (filter == null || filter.test(encryptedBallot)) {
                            setNext(encryptedBallot)
                            return
                        }
                    } catch (t : Throwable) {
                        println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  ${t.message}")
                        logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  ${t.message}"}
                    }
                } else {
                    return done()
                }
            }
        }
    }

    fun readEncryptedBallot(ballotFilePath : Path): EncryptedBallot{
        fileSystemProvider.newInputStream(ballotFilePath, StandardOpenOption.READ).use { inp ->
            val json = Json.decodeFromStream<EncryptedBallotJson>(inp)
            return json.import(group)
        }
    }

    actual override fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> {
        val devices = encryptingDevices()
        return Iterable { DeviceIterator(devices.iterator(), filter) }
    }

    private inner class DeviceIterator(
        val devices: Iterator<String>,
        private val filter : ((EncryptedBallot) -> Boolean)?,
    ) : AbstractIterator<EncryptedBallot>() {
        var innerIterator: Iterator<EncryptedBallot>? = null

        override fun computeNext() {
            while (true) {
                if (innerIterator != null && innerIterator!!.hasNext()) {
                    return setNext(innerIterator!!.next())
                }
                if (devices.hasNext()) {
                    innerIterator = iterateEncryptedBallots(devices.next(), filter).iterator()
                } else {
                    return done()
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

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

    // read the trustee in the given directory for the given guardianId
    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        val result =  readTrustee(fileSystem.getPath(filename))
        return if (result is Ok) result.unwrap() else throw Exception(result.getError())
    }

    actual override fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, String> {
        val ballotFilename = jsonPaths.encryptedBallotPath(ballotDir, ballotId)
        if (!pathExists(ballotFilename)) {
            return Err("readEncryptedBallot '$ballotFilename' file does not exist")
        }
        return Ok(readEncryptedBallot(Path.of(ballotFilename)))
    }

    actual override fun iteratePepBallots(pepDir : String): Iterable<BallotPep> {
        return Iterable { PepBallotIterator(group, Path.of(pepDir)) }
    }

    private inner class PepBallotIterator(val group: GroupContext, ballotDir: Path) : AbstractIterator<BallotPep>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file, StandardOpenOption.READ).use { inp ->
                    val json = jsonIgnoreNulls.decodeFromStream<BallotPepJson>(inp)
                    val pepBallot = json.import(group)
                    return setNext(pepBallot)
                }
            }
            return done()
        }
    }


    //////// The low level reading functions

    private fun readElectionConfig(constantsPath: Path, manifestFile: Path, configFile: Path): Result<ElectionConfig, String> {
        val errors = mutableListOf<String>()

        if (!Files.exists(constantsPath)) {
            errors.add("Constants '$constantsPath' file does not exist ")
        }
        if (!Files.exists(manifestFile)) {
            errors.add("Manifest '$manifestFile' file does not exist ")
        }
        if (!Files.exists(configFile)) {
            errors.add("ElectionConfig '$configFile' file does not exist ")
        }
        if (errors.isNotEmpty()) {
            return Err(errors.joinToString { "," })
        }

        val constants =  try {
            fileSystemProvider.newInputStream(constantsPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<ElectionConstantsJson>(inp)
                json.import()
            }
        } catch (e: Exception) {
            return Err("readElectionConfig on constants file $constantsPath error= ${e.message}")
        }

        val manifestBytes = try {
            readManifestBytes(manifestFile.toString())
        } catch (e: Exception) {
            return Err("readElectionConfig on manifest file $manifestFile error= ${e.message}")
        }

        return try {
            var electionConfig: ElectionConfig
            fileSystemProvider.newInputStream(configFile, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<ElectionConfigJson>(inp)
                electionConfig = json.import(constants, manifestBytes)
            }
            Ok(electionConfig)
        } catch (e: Exception) {
            Err("readElectionConfig on configFile $configFile error= ${e.message}")
        }
    }

    private fun readElectionInitialized(
        initPath: Path,
        config: ElectionConfig
    ): Result<ElectionInitialized, String> {
        if (!Files.exists(initPath)) {
            return Err("ElectionInitialized '$initPath' file does not exist ")
        }
        return try {
            var electionInitialized: ElectionInitialized
            fileSystemProvider.newInputStream(initPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<ElectionInitializedJson>(inp)
                electionInitialized = json.import(group, config)
            }
            Ok(electionInitialized)
        } catch (e: Exception) {
            Err("readElectionInitialized on file $initPath error= ${e.message}")
        }
    }

    private fun readTallyResult(tallyPath: Path, init: ElectionInitialized): Result<TallyResult, String> {
        if (!Files.exists(tallyPath)) {
            return Err("EncryptedTally '$tallyPath' file does not exist ")
        }
        return try {
            fileSystemProvider.newInputStream(tallyPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<EncryptedTallyJson>(inp)
                val encryptedTally = json.import(group)
                Ok(TallyResult(init, encryptedTally, emptyList()))
            }
        } catch (e: Exception) {
            Err("readTallyResult on file $tallyPath error= ${e.message}")
        }
    }

    private fun readDecryptionResult(
        decryptedTallyPath: Path,
        tallyResult: TallyResult
    ): Result<DecryptionResult, String> {
        if (!Files.exists(decryptedTallyPath)) {
            return Err("DecryptedTally '$decryptedTallyPath' file does not exist ")
        }
        return try {
            fileSystemProvider.newInputStream(decryptedTallyPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                val decryptedTallyOrBallot = json.import(group)
                Ok(DecryptionResult(tallyResult, decryptedTallyOrBallot))
            }
        } catch (e: Exception) {
            Err("readDecryptionResult on file $decryptedTallyPath error= ${e.message}")
        }
    }

    private inner class PlaintextBallotIterator(
        ballotDir: Path,
        private val filter: Predicate<PlaintextBallot>?
    ) : AbstractIterator<PlaintextBallot>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file, StandardOpenOption.READ).use { inp ->
                    val json = jsonIgnoreNulls.decodeFromStream<PlaintextBallotJson>(inp)
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

    private inner class EncryptedBallotFileIterator(
        ballotDir: Path,
        private val group: GroupContext,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file, StandardOpenOption.READ).use { inp ->
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
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file, StandardOpenOption.READ).use { inp ->
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
        if (!pathExists(filePath.toString())) {
            return Err("readTrustee '$filePath' file does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(filePath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<TrusteeJson>(inp)
                val decryptingTrustee = json.importDecryptingTrustee(group)
                Ok(decryptingTrustee)
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filePath error")
        }
    }
}

fun Path.pathList(): List<Path> {
    // LOOK does this sort?
    // LOOK "API Note: This method must be used within a try-with-resources statement"
    return Files.walk(this, 1).use { fileStream ->
        fileStream.filter { it != this }.toList()
    }
}

fun Path.pathListNoDirs(): List<Path> {
    // LOOK does this sort?
    // LOOK "API Note: This method must be used within a try-with-resources statement"
    return Files.walk(this, 1).use { fileStream ->
        fileStream.filter { it != this && !it.isDirectory() }.toList()
    }
}