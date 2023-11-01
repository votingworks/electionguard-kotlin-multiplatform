package electionguard.publish

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.fileReadBytes
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.pep.BallotPep
import electionguard.protoconvert.import
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging
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
        throw RuntimeException(result.toString()) // LOOK
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, ErrorMessages> {
        return readElectionConfig(protoPaths.electionConfigPath())
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages> {
        return groupContext.readElectionInitialized(protoPaths.electionInitializedPath())
    }

    actual override fun readTallyResult(): Result<TallyResult, ErrorMessages> {
        return groupContext.readTallyResult(protoPaths.tallyResultPath())
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages> {
        return groupContext.readDecryptionResult(protoPaths.decryptionResultPath())
    }

    actual override fun encryptingDevices(): List<String> {
        val topBallotPath = Path.of(protoPaths.encryptedBallotDir())
        if (!Files.exists(topBallotPath)) {
            return emptyList()
        }
        val deviceDirs: Stream<Path> = Files.list(topBallotPath)
        return deviceDirs.map { it.getName( it.nameCount - 1).toString() }.toList() // last name in the path
    }

    actual override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallotChain device='$device'")
        val ballotChainFile = protoPaths.encryptedBallotChain(device)
        if (!Files.exists(Path.of(ballotChainFile))) {
            return errs.add("file '$ballotChainFile' does not exist")
        }
        return try {
            var proto: electionguard.protogen.EncryptedBallotChain
            FileInputStream(ballotChainFile).use { inp ->
                proto = electionguard.protogen.EncryptedBallotChain.decodeFromStream(inp)
            }
            val result = proto.import( errs)
            if (errs.hasErrors()) Err(errs) else Ok(result!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
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
            return Iterable { EncryptedBallotBatchIterator(batchedFileName, groupContext, null, filter) }
        }
        // encrypted ballots are in separate protobuf files
        return Iterable { EncryptedBallotFileIterator(dirPath, filter) }
    }

    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        // use batch (all in one protobuf) if it exists
        val batchedFileName: String = protoPaths.decryptedBatchPath()
        if (Files.exists(Path.of(batchedFileName))) {
            return Iterable { DecryptedBallotBatchIterator(batchedFileName) }
        }
        // otherwise, decrypted ballots are in separate protobuf files
        val decryptedBallotDir: String = protoPaths.decryptedBallotDir()
        if (Files.exists(Path.of(batchedFileName))) {
            return Iterable { DecryptedBallotFileIterator(Path.of(decryptedBallotDir)) }
        }
        // otherwise there are none
        return emptyList()
    }

    actual override fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> {
        val devices = encryptingDevices()
        return Iterable { DeviceIterator(devices.iterator(), filter) }
    }

    actual override fun hasEncryptedBallots(): Boolean {
        return Files.exists(Path.of(protoPaths.encryptedBallotPath()))
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
    actual override fun readTrustee(trusteeDir: String, guardianId: String): Result<DecryptingTrusteeIF, ErrorMessages> {
        val filename = protoPaths.decryptingTrusteePath(trusteeDir, guardianId)
        return groupContext.readTrustee(filename)
    }

    actual override fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallot '$ballotDir/$ballotId'")
        return errs.add("Not implemented yet")
    }


    actual override fun iteratePepBallots(pepDir : String): Iterable<BallotPep> {
        throw RuntimeException("Not implemented yet")
    }

    ////////////////////////////////////////////////////////////////////
    // The low level reading functions for protobuf

    private fun makeManifestResult(manifestBytes: ByteArray): Result<Manifest,ErrorMessages> {
        return try {
            var proto: electionguard.protogen.Manifest
            ByteArrayInputStream(manifestBytes).use { inp -> proto = electionguard.protogen.Manifest.decodeFromStream(inp) }
            Ok(proto.import())
        } catch (t: Throwable) {
            ErrorMessages("makeManifestResult").add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun readElectionConfig(filename: String): Result<ElectionConfig, ErrorMessages> {
        val errs = ErrorMessages("readElectionConfigProto '$filename'")
        if (!Files.exists(Path.of(filename))) {
            return errs.add("file does not exist")
        }
        return try {
            var proto: electionguard.protogen.ElectionConfig
            FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionConfig.decodeFromStream(inp) }
            proto.import(errs)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun GroupContext.readElectionInitialized(filename: String): Result<ElectionInitialized, ErrorMessages> {
        val errs = ErrorMessages("readElectionInitialized '$filename'")
        if (!Files.exists(Path.of(filename))) {
            return errs.add("file does not exist")
        }
        return try {
            FileInputStream(filename).use { inp ->
                val proto = electionguard.protogen.ElectionInitialized.decodeFromStream(inp)
                val init = proto.import(this, errs)
                if (errs.hasErrors()) Err(errs) else Ok(init!!)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun GroupContext.readTallyResult(filename: String): Result<TallyResult, ErrorMessages> {
        val errs = ErrorMessages("readTallyResult file '${filename}'")
        if (!Files.exists(Path.of(filename))) {
            return errs.add("file does not exist ")
        }
        return try {
            var proto: electionguard.protogen.TallyResult
            FileInputStream(filename).use {
                inp -> proto = electionguard.protogen.TallyResult.decodeFromStream(inp)
            }
            val result = proto.import(this, errs)
            if (errs.hasErrors()) Err(errs) else Ok(result!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun GroupContext.readDecryptionResult(filename: String): Result<DecryptionResult, ErrorMessages> {
        val errs = ErrorMessages("readDecryptionResult file '${filename}'")
        if (!Files.exists(Path.of(filename))) {
            return errs.add("file does not exist ")
        }
        return try {
            var proto: electionguard.protogen.DecryptionResult
            FileInputStream(filename).use { inp ->
                proto = electionguard.protogen.DecryptionResult.decodeFromStream(inp)
            }
            val result = proto.import(this, errs)
            if (errs.hasErrors()) Err(errs) else Ok(result!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun GroupContext.readTrustee(filename: String): Result<DecryptingTrusteeIF, ErrorMessages> {
        val errs = ErrorMessages("readTrustee file '${filename}'")
        if (!Files.exists(Path.of(filename))) {
            return errs.add("file does not exist ")
        }
        return try {
            var proto: electionguard.protogen.DecryptingTrustee
            FileInputStream(filename).use { inp ->
                proto = electionguard.protogen.DecryptingTrustee.decodeFromStream(inp)
            }
            val result = proto.import(this, errs)
            if (errs.hasErrors()) Err(errs) else Ok(result!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
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

    //// Encrypted Ballot iteration

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
                    val errs = ErrorMessages("EncryptedBallotChainIterator file='$ballotFile'")
                    val result: EncryptedBallot? = proto.import(groupContext, errs)
                    if (errs.hasErrors()) {
                        logger.error { errs.toString() }
                        continue
                    } else {
                        val eballot = result!!
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

    private class EncryptedBallotBatchIterator(
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
                val errs = ErrorMessages("EncryptedBallotBatchIterator ballotId='${ballotProto.ballotId}'")
                val result: EncryptedBallot? = ballotProto.import(groupContext, errs)
                if (errs.hasErrors()) {
                    logger.error { errs.toString() }
                    continue
                } else {
                    val eballot = result!!
                    if (filter != null && !filter.test(eballot)) {
                        continue // skip it
                    }
                    return setNext(eballot)
                }
            }
        }
    }

    private inner class EncryptedBallotFileIterator(
        ballotDir: Path,
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
                val errs = ErrorMessages("EncryptedBallotFileIterator file='$ballotFile'")
                val result: EncryptedBallot? = proto.import(groupContext, errs)
                if (errs.hasErrors()) {
                    logger.error { errs.toString() }
                    continue
                } else {
                    val eballot = result!!
                    if (filter != null && !filter.test(eballot)) {
                        continue // skip it
                    }
                    return setNext(eballot)
                }
            }
            return done()
        }
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

    //// Decrypted ballot iteration

    private inner class DecryptedBallotBatchIterator(filename: String) : AbstractIterator<DecryptedTallyOrBallot>() {
        private val input: FileInputStream = FileInputStream(filename)

        override fun computeNext() {
            while (true) {
                val length = readVlen(input)
                if (length < 0) {
                    input.close()
                    return done()
                }
                val message = input.readNBytes(length)
                val proto = electionguard.protogen.DecryptedTallyOrBallot.decodeFromByteBuffer(ByteBuffer.wrap(message))
                val errs = ErrorMessages("DecryptedBallotBatchIterator id='${proto.id}'")
                val result: DecryptedTallyOrBallot? = proto.import(groupContext, errs)
                if (errs.hasErrors()) {
                    logger.error { errs.toString() }
                    continue
                } else {
                    return setNext(result!!)
                }
            }
        }
    }

    private inner class DecryptedBallotFileIterator(ballotDir: Path) : AbstractIterator<DecryptedTallyOrBallot>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val ballotFile = pathList[idx++]
                var proto: electionguard.protogen.DecryptedTallyOrBallot
                FileInputStream(ballotFile.toString()).use { inp ->
                    proto = electionguard.protogen.DecryptedTallyOrBallot.decodeFromStream(inp)
                }
                val errs = ErrorMessages("DecryptedBallotFileIterator file='$ballotFile'")
                val result: DecryptedTallyOrBallot? = proto.import(groupContext, errs)
                if (errs.hasErrors()) {
                    logger.error { errs.toString() }
                    continue
                } else {
                    return setNext(result!!)
                }
            }
            return done()
        }
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
