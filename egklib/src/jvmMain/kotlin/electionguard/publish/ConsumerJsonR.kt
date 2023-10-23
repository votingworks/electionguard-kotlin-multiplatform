@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json.*
import electionguard.json2.*
import electionguard.pep.BallotPep
import electionguard.pep.BallotPepJson
import electionguard.pep.import
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import java.util.function.Predicate
import java.util.stream.Stream

private val logger = KotlinLogging.logger("ConsumerJsonRJvm")

/** Can read both zipped and unzipped JSON election record */

actual class ConsumerJsonR actual constructor(val topDir: String, val group: GroupContext) : Consumer {
    var fileSystem : FileSystem = FileSystems.getDefault()
    var fileSystemProvider : FileSystemProvider = fileSystem.provider()
    var jsonPaths = ElectionRecordJsonRPaths(topDir)
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

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
            jsonPaths = ElectionRecordJsonRPaths("")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = true

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        ByteArrayInputStream(manifestBytes).use { inp ->
            val json = jsonReader.decodeFromStream<ManifestJson>(inp)
            return json.import()
        }
    }

    actual override fun readManifestBytes(filename : String): ByteArray {
        // need to use fileSystemProvider for zipped files
        val manifestPath = fileSystem.getPath(filename)
        val manifestBytes =
            fileSystemProvider.newInputStream(manifestPath).use { inp ->
                inp.readAllBytes()
            }
        return manifestBytes
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(
            fileSystem.getPath(jsonPaths.electionParametersPath()),
            fileSystem.getPath(jsonPaths.manifestCanonicalPath()),
            fileSystem.getPath(jsonPaths.electionHashesPath()),
        )
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(
            fileSystem.getPath(jsonPaths.jointElectionKeyPath()),
            fileSystem.getPath(jsonPaths.electionHashesExtPath()),
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
            fileSystemProvider.newInputStream(ballotChainPath).use { inp ->
                val json = jsonReader.decodeFromStream<EncryptedBallotChainJson>(inp)
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
                    val ballotFilePath = Path.of(jsonPaths.encryptedBallotDevicePath(device, ballotIds.next()))
                    val encryptedBallot = readEncryptedBallot(ballotFilePath)
                    if (filter == null || filter.test(encryptedBallot)) {
                        setNext(encryptedBallot)
                        return
                    }
                } else {
                    return done()
                }
            }
        }
    }

    fun readEncryptedBallot(ballotFilePath : Path): EncryptedBallot{
        fileSystemProvider.newInputStream(ballotFilePath).use { inp ->
            val json = jsonReader.decodeFromStream<EncryptedBallotJson>(inp)
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
        val filename = jsonPaths.guardianPrivatePath(trusteeDir, guardianId)
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
                fileSystemProvider.newInputStream(file).use { inp ->
                    val json = jsonReader.decodeFromStream<BallotPepJson>(inp)
                    val pepBallot = json.import(group)
                    return setNext(pepBallot)
                }
            }
            return done()
        }
    }


    //////// The low level reading functions

    private fun readElectionConfig(
        parametersFile: Path,
        manifestFile: Path,
        electionHashesFile: Path
    ): Result<ElectionConfig, String> {
        return try {
            var parameters: ElectionParameters
            fileSystemProvider.newInputStream(parametersFile).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionParametersJsonR>(inp)
                parameters = json.import()
            }

            val manifestBytes = readManifestBytes(manifestFile.toString())

            var electionHashes: ElectionHashes
            fileSystemProvider.newInputStream(electionHashesFile).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionHashesJsonR>(inp)
                electionHashes = json.import()
            }
            val electionConfig = ElectionConfig(
                "config_version",
                parameters.electionConstants,
                parameters.varyingParameters.n,
                parameters.varyingParameters.k,
                electionHashes.Hp,
                electionHashes.Hm,
                electionHashes.Hb,
                manifestBytes,
                false,
                ByteArray(0),
            )
            Ok(electionConfig)
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig error")
        }
    }

    private fun readElectionInitialized(
        jointPublicKeyFile: Path,
        extendedHashFile: Path,
        config: ElectionConfig
    ): Result<ElectionInitialized, String> {
        return try {
            var jointPublicKey: ElementModP
            fileSystemProvider.newInputStream(jointPublicKeyFile).use { inp ->
                val json = jsonReader.decodeFromStream<JointElectionPublicKeyJsonR>(inp)
                jointPublicKey = json.import(group)
            }

            var extendedHash: UInt256
            fileSystemProvider.newInputStream(extendedHashFile).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionHashesExtJsonR>(inp)
                extendedHash = json.import()
            }

            val guardiansR = mutableListOf<GuardianR>()
            for (idx in 1..config.numberOfGuardians) {
                val publicGuardianPath = Path.of(jsonPaths.guardianPath(idx))
                fileSystemProvider.newInputStream(publicGuardianPath).use { inp ->
                    val json = jsonReader.decodeFromStream<GuardianJsonR>(inp)
                    val guardian = json.import(group)
                    guardiansR.add(guardian)
                }
            }

            val electionInitialized = ElectionInitialized(
                config,
                jointPublicKey,
                extendedHash,
                guardiansR.map { it.convert(group) },
            )
            Ok(electionInitialized)
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized error")
        }
    }

    private fun readTallyResult(filename: Path, init: ElectionInitialized): Result<TallyResult, String> {
        return try {
            fileSystemProvider.newInputStream(filename).use { inp ->
                val json = jsonReader.decodeFromStream<EncryptedTallyJson>(inp)
                val encryptedTally = json.import(group)
                Ok(TallyResult(init, encryptedTally, emptyList()))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename error")
        }
    }

    private fun readDecryptionResult(
        decryptedTallyPath: Path,
        tallyResult: TallyResult
    ): Result<DecryptionResult, String> {
        // all the coefficients in a map in one file
        return try {
            fileSystemProvider.newInputStream(decryptedTallyPath).use { inp ->
                val json = jsonReader.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                val decryptedTallyOrBallot = json.import(group)
                Ok(DecryptionResult(tallyResult, decryptedTallyOrBallot))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $decryptedTallyPath error")
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
                fileSystemProvider.newInputStream(file).use { inp ->
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
                fileSystemProvider.newInputStream(file).use { inp ->
                    val json = jsonReader.decodeFromStream<EncryptedBallotJson>(inp)
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
                fileSystemProvider.newInputStream(file).use { inp ->
                    val json = jsonReader.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
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
            fileSystemProvider.newInputStream(filePath).use { inp ->
                val json = jsonReader.decodeFromStream<TrusteeJson>(inp)
                val decryptingTrustee = json.importDecryptingTrustee(group)
                Ok(decryptingTrustee)
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filePath error")
        }
    }
}