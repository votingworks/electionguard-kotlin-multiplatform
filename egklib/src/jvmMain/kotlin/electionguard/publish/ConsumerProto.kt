package electionguard.publish

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.fileReadBytes
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.pep.BallotPep
import electionguard.protoconvert.import
import mu.KotlinLogging
import pbandk.decodeFromByteBuffer
import pbandk.decodeFromStream
import java.io.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate
import java.util.stream.Stream

private val logger = KotlinLogging.logger("ElectionRecord")

actual class ConsumerProto actual constructor(val topDir: String, val groupContext: GroupContext): Consumer {
    val protoPaths = ElectionRecordProtoPaths(topDir)

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun isJson() = false

    actual override fun readManifestBytes(filename : String): ByteArray {
        return fileReadBytes(filename)
    }

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        val result = makeManifestResult(manifestBytes)
        if (result is Ok) return result.value
        throw RuntimeException(result.toString())
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(protoPaths.electionConfigPath())
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        return groupContext.readElectionInitialized(protoPaths.electionInitializedPath())
    }

    actual override fun readTallyResult(): Result<TallyResult, String> {
        return groupContext.readTallyResult(protoPaths.tallyResultPath())
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, String> {
        return groupContext.readDecryptionResult(protoPaths.decryptionResultPath())
    }

    ///////////////////////////////////////////////////////////////////////////

    actual override fun encryptingDevices(): List<String> {
        val topBallotPath = Path.of(protoPaths.encryptedBallotDir())
        if (!Files.exists(topBallotPath)) {
            return emptyList()
        }
        val deviceDirs: Stream<Path> = Files.list(topBallotPath)
        return deviceDirs.map { it.getName( it.nameCount - 1).toString() }.toList() // last name in the path
    }

    actual override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, String> {
        val ballotChain = protoPaths.encryptedBallotChain(device)
        if (!Files.exists(Path.of(ballotChain))) {
            return Err("readEncryptedBallotChain path '$ballotChain' does not exist")
        }
        return try {
            var proto: electionguard.protogen.EncryptedBallotChain
            FileInputStream(ballotChain).use { inp ->
                proto = electionguard.protogen.EncryptedBallotChain.decodeFromStream(inp)
            }
            proto.import()
        } catch (e: Exception) {
            Err("failed")
        }
    }

    actual override fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> {
        val dirPath = Path.of(protoPaths.encryptedBallotDir(device))
        if (!Files.exists(dirPath)) {
            throw RuntimeException("$dirPath doesnt exist")
        }
        // use ballotChain if it exists
        val chainResult = readEncryptedBallotChain(device)
        if (chainResult is Ok) {
            val chain = chainResult.unwrap()
            return Iterable { EncryptedBallotChainIterator(device, chain.ballotIds.iterator(), filter) }
        }
        // use batch (all in one protobuf) if it exists
        val batchedFileName: String = protoPaths.encryptedBallotBatched(device)
        if (Files.exists(Path.of(batchedFileName))) {
            return Iterable { EncryptedBallotIterator(batchedFileName, groupContext, null, filter) }
        }
        // just read individual files
        return Iterable { EncryptedBallotFileIterator(dirPath, groupContext, filter) }
    }

    private inner class EncryptedBallotChainIterator(
        val device: String,
        val ballotIds: Iterator<String>,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {

        override fun computeNext() {
            while (true) {
                if (ballotIds.hasNext()) {
                    val ballotFile = protoPaths.encryptedBallotPath(device, ballotIds.next())
                    var proto: electionguard.protogen.EncryptedBallot
                    FileInputStream(ballotFile).use { inp ->
                        proto = electionguard.protogen.EncryptedBallot.decodeFromStream(inp)
                    }
                    val result = proto.import(groupContext)
                    if (result is Err) {
                        println("Error importing $ballotFile")
                        continue
                    } else {
                        val eballot: EncryptedBallot = result.unwrap()
                        if (filter != null && !filter.test(eballot)) {
                            continue // skip it
                        }
                        return setNext(eballot)
                    }
                } else {
                    return done()
                }
            }
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
                val ballotFile = pathList[idx++]
                var proto: electionguard.protogen.EncryptedBallot
                FileInputStream(ballotFile.toString()).use { inp ->
                    proto = electionguard.protogen.EncryptedBallot.decodeFromStream(inp)
                }
                val result = proto.import(groupContext)
                if (result is Ok) { // TODO error message
                    val encryptedBallot = result.unwrap()
                    if (filter == null || filter.test(encryptedBallot)) {
                        return setNext(encryptedBallot)
                    }
                }
            }
            return done()
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

    actual override fun hasEncryptedBallots(): Boolean {
        return Files.exists(Path.of(protoPaths.encryptedBallotPath()))
    }

    /* all submitted ballots, cast or spoiled
    actual override fun iterateEncryptedBallots(filter: ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        val filename = protoPaths.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(filename, groupContext, null, filter) }
    }

    // only EncryptedBallot that are CAST
    actual override fun iterateCastBallots(): Iterable<EncryptedBallot> {
        val filename = protoPaths.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val protoFilter =
            Predicate<electionguard.protogen.EncryptedBallot> { it.state == electionguard.protogen.EncryptedBallot.BallotState.CAST }
        return Iterable { EncryptedBallotIterator(filename, groupContext, protoFilter, null) }
    }

    // only EncryptedBallot that are SPOILED
    actual override fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        val filename = protoPaths.encryptedBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        val protoFilter =
            Predicate<electionguard.protogen.EncryptedBallot> { it.state == electionguard.protogen.EncryptedBallot.BallotState.SPOILED }
        return Iterable { EncryptedBallotIterator(filename, groupContext, protoFilter, null) }
    }

     */

    // all tallies in the SPOILED_BALLOT_FILE file
    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val filename = protoPaths.spoiledBallotPath()
        if (!Files.exists(Path.of(filename))) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(filename, groupContext) }
    }

    // plaintext ballots in given directory, with filter
    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        if (!Files.exists(Path.of(protoPaths.plaintextBallotPath(ballotDir)))) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(protoPaths.plaintextBallotPath(ballotDir), filter) }
    }

    // trustee in given directory for given guardianId
    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = protoPaths.decryptingTrusteePath(trusteeDir, guardianId)
        return groupContext.readTrustee(filename)
    }

    actual override fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, String> =
        Err("Not implemented yet")


    //////// The low level reading functions for protobuf

    private fun makeManifestResult(manifestBytes: ByteArray): Result<Manifest,String> {
        return try {
            var proto: electionguard.protogen.Manifest
            ByteArrayInputStream(manifestBytes).use { inp -> proto = electionguard.protogen.Manifest.decodeFromStream(inp) }
            proto.import()
        } catch (e: Exception) {
            Err(e.message ?: "makeManifestResult failed")
        }
    }

    private fun readElectionConfig(filename: String): Result<ElectionConfig, String> {
        return try {
            var proto: electionguard.protogen.ElectionConfig
            FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionConfig.decodeFromStream(inp) }
            proto.import()
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $filename failed")
        }
    }

    private fun GroupContext.readElectionInitialized(filename: String): Result<ElectionInitialized, String> {
        return try {
            var proto: electionguard.protogen.ElectionInitialized
            FileInputStream(filename).use { inp ->
                proto = electionguard.protogen.ElectionInitialized.decodeFromStream(inp)
            }
            proto.import(this)
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $filename failed")
        }
    }

    private fun GroupContext.readTallyResult(filename: String): Result<TallyResult, String> {
        return try {
            var proto: electionguard.protogen.TallyResult
            FileInputStream(filename).use { inp -> proto = electionguard.protogen.TallyResult.decodeFromStream(inp) }
            proto.import(this)
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    private fun GroupContext.readDecryptionResult(filename: String): Result<DecryptionResult, String> {
        return try {
            var proto: electionguard.protogen.DecryptionResult
            FileInputStream(filename).use { inp ->
                proto = electionguard.protogen.DecryptionResult.decodeFromStream(inp)
            }
            proto.import(this)
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }

    private class PlaintextBallotIterator(
        filename: String,
        private val filter: Predicate<PlaintextBallot>?
    ) : AbstractIterator<PlaintextBallot>() {
        private val input: FileInputStream = FileInputStream(filename)

        override fun computeNext() {
            while (true) {
                val length = readVlen(input)
                if (length < 0) {
                    input.close()
                    return done()
                }
                val message = input.readNBytes(length)
                val ballotProto = electionguard.protogen.PlaintextBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
                val ballot = ballotProto.import()
                if (filter != null && !filter.test(ballot)) {
                    continue // skip it
                }
                setNext(ballot)
                break
            }
        }
    }

    // Create iterators, so that we never have to read in all ballots at once.
    private class EncryptedBallotIterator(
        filename: String,
        private val groupContext: GroupContext,
        private val protoFilter: Predicate<electionguard.protogen.EncryptedBallot>?,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {

        private val input: FileInputStream = FileInputStream(filename)

        override fun computeNext() {
            while (true) {
                val length = readVlen(input)
                if (length < 0) {
                    input.close()
                    return done()
                }
                val message = input.readNBytes(length)
                val ballotProto = electionguard.protogen.EncryptedBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
                if (protoFilter != null && !protoFilter.test(ballotProto)) {
                    continue // skip it
                }
                val ballotResult = ballotProto.import(groupContext)
                if (ballotResult is Ok) {
                    if (filter != null && !filter.test(ballotResult.unwrap())) {
                        continue // skip it
                    }
                    setNext(ballotResult.unwrap())
                    break
                } else {
                    logger.warn { "Error on ${ballotProto.ballotId} = ${ballotResult.unwrapError()}" }
                    continue
                }
            }
        }
    }

    private class SpoiledBallotTallyIterator(
        filename: String,
        private val group: GroupContext,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        private val input: FileInputStream = FileInputStream(filename)

        override fun computeNext() {
            val length = readVlen(input)
            if (length < 0) {
                input.close()
                return done()
            }
            val message = input.readNBytes(length)
            val tallyProto =
                electionguard.protogen.DecryptedTallyOrBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
            val tally = tallyProto.import(group)

            setNext(tally.getOrElse { throw RuntimeException("Tally failed to parse") })
        }
    }

    private fun GroupContext.readTrustee(filename: String): DecryptingTrusteeDoerre {
        var proto: electionguard.protogen.DecryptingTrustee
        FileInputStream(filename).use { inp -> proto = electionguard.protogen.DecryptingTrustee.decodeFromStream(inp) }
        return proto.import(this).getOrElse { throw RuntimeException("DecryptingTrustee $filename failed to parse") }
    }

    actual override fun iteratePepBallots(pepDir : String): Iterable<BallotPep> {
        return emptyList()
    }

}

// variable length (base 128) int32
fun readVlen(input: InputStream): Int {
    var ib: Int = input.read()
    if (ib == -1) {
        return -1
    }

    var result = ib.and(0x7F)
    var shift = 7
    while (ib.and(0x80) != 0) {
        ib = input.read()
        if (ib == -1) {
            return -1
        }
        val im = ib.and(0x7F).shl(shift)
        result = result.or(im)
        shift += 7
    }
    return result
}
