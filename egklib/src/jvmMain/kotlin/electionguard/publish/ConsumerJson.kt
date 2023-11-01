@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json2.*
import electionguard.pep.BallotPep
import electionguard.pep.BallotPepJson
import electionguard.pep.import
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.nio.file.*
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
    val jsonReader = Json { explicitNulls = false }

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

    actual override fun readElectionConfig(): Result<ElectionConfig, ErrorMessages> {
        return readElectionConfig(
            fileSystem.getPath(jsonPaths.electionConstantsPath()),
            fileSystem.getPath(jsonPaths.manifestPath()),
            fileSystem.getPath(jsonPaths.electionConfigPath()),
        )
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages> {
        val config : Result<ElectionConfig, ErrorMessages> = readElectionConfig()
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

    actual override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallotChain device '$device'")
        val ballotChainPath = Path.of(jsonPaths.encryptedBallotChain(device))
        if (!Files.exists(ballotChainPath)) {
            return errs.add("'$ballotChainPath' file does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(ballotChainPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<EncryptedBallotChainJson>(inp)
                val chain = json.import(errs)
                if (errs.hasErrors()) Err(errs) else Ok(chain!!)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
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
        return Iterable { EncryptedBallotFileIterator(deviceDirPath, filter) }
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

    actual override fun readTallyResult(): Result<TallyResult, ErrorMessages> {
        val init = readElectionInitialized()
        if (init is Err) {
            return Err(init.error)
        }
        return readTallyResult(
            fileSystem.getPath(jsonPaths.encryptedTallyPath()),
            init.unwrap(),
        )
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages> {
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
        return Iterable { DecryptedBallotIterator(dirPath, group) }
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
    actual override fun readTrustee(trusteeDir: String, guardianId: String): Result<DecryptingTrusteeIF,ErrorMessages> {
        val errs = ErrorMessages("readTrustee $guardianId from directory $trusteeDir")
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        if (!Files.exists(fileSystem.getPath(filename))) {
            return errs.add("file does not exist ")
        }
        return readTrustee(fileSystem.getPath(filename))
    }

    actual override fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallot ballotId=$ballotId from directory $ballotDir")
        val ballotFilename = jsonPaths.encryptedBallotPath(ballotDir, ballotId)
        if (!Files.exists(fileSystem.getPath(ballotFilename))) {
            return errs.add("'$ballotFilename' file does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(fileSystem.getPath(ballotFilename), StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<EncryptedBallotJson>(inp)
                val eballot = json.import(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(eballot!!)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
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
                    val json = jsonReader.decodeFromStream<BallotPepJson>(inp)
                    val errs = ErrorMessages("readPepBallot file=$file")
                    val pepBallot = json.import(group, errs)
                    if (errs.hasErrors()) {
                        logger.error { errs.toString() }
                    } else {
                        return setNext(pepBallot!!)
                    }
                }
            }
            return done()
        }
    }


    //////// The low level reading functions

    private fun readElectionConfig(constantsPath: Path, manifestFile: Path, configFile: Path): Result<ElectionConfig, ErrorMessages> {
        val errs = ErrorMessages("readElectionConfigJson")

        if (!Files.exists(constantsPath)) {
            errs.add("Constants '$constantsPath' file does not exist ")
        }
        if (!Files.exists(manifestFile)) {
            errs.add("Manifest '$manifestFile' file does not exist ")
        }
        if (!Files.exists(configFile)) {
            errs.add("ElectionConfig '$configFile' file does not exist ")
        }
        if (errs.hasErrors()) {
            return Err(errs)
        }

        val constantErrs = errs.nested("constants file '$constantsPath'")
        val constants : ElectionConstants? = try {
            fileSystemProvider.newInputStream(constantsPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<ElectionConstantsJson>(inp)
                json.import(constantErrs)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        val manifestBytes = try {
            readManifestBytes(manifestFile.toString())
        } catch (t: Throwable) {
            errs.nested("manifest file '$manifestFile'").add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        val configErrs = errs.nested("config file '$configFile'")
        return try {
            var electionConfig: ElectionConfig?
            fileSystemProvider.newInputStream(configFile, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<ElectionConfigJson>(inp)
                electionConfig = json.import(constants, manifestBytes, configErrs)
            }
            if (errs.hasErrors()) Err(errs) else Ok(electionConfig!!)
        } catch (t: Throwable) {
            return configErrs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun readElectionInitialized(initPath: Path, config: ElectionConfig): Result<ElectionInitialized, ErrorMessages> {
        val errs = ErrorMessages("ElectionInitializedJson file '${initPath}")
        if (!Files.exists(initPath)) {
            return errs.add("file does not exist ")
        }
        return try {
            var electionInitialized: ElectionInitialized?
            fileSystemProvider.newInputStream(initPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<ElectionInitializedJson>(inp)
                electionInitialized = json.import(group, config, errs)
            }
            if (errs.hasErrors()) Err(errs) else Ok(electionInitialized!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun readTallyResult(tallyPath: Path, init: ElectionInitialized): Result<TallyResult, ErrorMessages> {
        val errs = ErrorMessages("TallyResult file '${tallyPath}'")
        if (!Files.exists(tallyPath)) {
            return errs.add("file does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(tallyPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<EncryptedTallyJson>(inp)
                val encryptedTally = json.import(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(TallyResult(init, encryptedTally!!, emptyList()))
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun readDecryptionResult(
        decryptedTallyPath: Path,
        tallyResult: TallyResult
    ): Result<DecryptionResult, ErrorMessages> {
        val errs = ErrorMessages("DecryptedTally '$decryptedTallyPath'")
        if (!Files.exists(decryptedTallyPath)) {
            return errs.add("file does not exist ")
        }
        return try {
            fileSystemProvider.newInputStream(decryptedTallyPath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                val decryptedTallyOrBallot = json.import(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(DecryptionResult(tallyResult, decryptedTallyOrBallot!!))
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun readTrustee(filePath: Path): Result<DecryptingTrusteeIF, ErrorMessages> {
        val errs = ErrorMessages("readTrustee '$filePath'")
        return try {
            fileSystemProvider.newInputStream(filePath, StandardOpenOption.READ).use { inp ->
                val json = Json.decodeFromStream<TrusteeJson>(inp)
                val decryptingTrustee = json.importDecryptingTrustee(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(decryptingTrustee!!)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
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
                    val json = jsonReader.decodeFromStream<PlaintextBallotJson>(inp)
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

    //// Encrypted ballots iteration

    private inner class EncryptedBallotFileIterator(
        ballotDir: Path,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val ballotFilePath = pathList[idx++]
                try {
                    val errs = ErrorMessages("EncryptedBallotJson '$ballotFilePath'")
                    val encryptedBallot = readEncryptedBallot(ballotFilePath, errs)
                    if (errs.hasErrors()) {
                        logger.error { errs.toString() }
                    } else {
                        if (filter == null || filter.test(encryptedBallot!!)) {
                            return setNext(encryptedBallot!!)
                        } // otherwise skip it
                    }
                } catch (t : Throwable) {
                    println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  Exception= ${t.message} ${t.stackTraceToString()}")
                    logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n Exception= ${t.message} ${t.stackTraceToString()}"}
                }
            }
            return done()
        }
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
                    if (!Files.exists(ballotFilePath)) {
                        println("EncryptedBallotDeviceIterator file '${ballotFilePath}' does not exist, skipping}")
                        continue
                    }
                    try {
                        val errs = ErrorMessages("EncryptedBallotJson '$ballotFilePath'")
                        val encryptedBallot = readEncryptedBallot(ballotFilePath, errs)
                        if (errs.hasErrors()) {
                            logger.error { errs.toString() }
                        } else {
                            if (filter == null || filter.test(encryptedBallot!!)) {
                                return setNext(encryptedBallot!!)
                            } // otherwise skip it
                        }
                    } catch (t : Throwable) {
                        println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  Exception= ${t.message} ${t.stackTraceToString()}")
                        logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  Exception= ${t.message} ${t.stackTraceToString()}"}
                    }
                } else {
                    return done()
                }
            }
        }
    }

    fun readEncryptedBallot(ballotFilePath : Path, errs: ErrorMessages): EncryptedBallot? {
        fileSystemProvider.newInputStream(ballotFilePath, StandardOpenOption.READ).use { inp ->
            val json = Json.decodeFromStream<EncryptedBallotJson>(inp)
            return json.import(group, errs)
        }
    }

    //// Decrypted ballots iteration

    private inner class DecryptedBallotIterator(
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
                    val errs = ErrorMessages("DecryptedBallotIterator '$file'")
                    val decryptedTallyOrBallot = json.import(group, errs)
                    if (errs.hasErrors()) {
                        logger.error { errs.toString() }
                    } else {
                        return setNext(decryptedTallyOrBallot!!)
                    }
                }
            }
            return done()
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