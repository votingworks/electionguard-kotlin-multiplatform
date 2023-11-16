package electionguard.rave

import electionguard.core.GroupContext
import electionguard.core.createDirectories
import electionguard.publish.pathListNoDirs
import electionguard.publish.removeAllFiles
import electionguard.util.ErrorMessages

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.spi.FileSystemProvider

private const val PEP_BALLOT_PREFIX = "pepballot-"
private const val JSON_SUFFIX = ".json"
private val logger = KotlinLogging.logger("RaveIO")

actual class RaveIO actual constructor(val pepDir: String, val group: GroupContext, val createNew: Boolean) {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    var fileSystemProvider : FileSystemProvider = FileSystems.getDefault().provider()

    actual fun pepBallotSink(): PepBallotSinkIF {
        if (createNew) {
            removeAllFiles(Path.of(pepDir))
        }
        createDirectories(pepDir)
        return PepBallotSink()
    }

    inner class PepBallotSink() : PepBallotSinkIF {
        override fun writePepBallot(pepBallot: BallotPep) {
            val pepJson = pepBallot.publishJson()
            FileOutputStream(pepBallotPath(pepBallot.ballotId)).use { out ->
                jsonReader.encodeToStream(pepJson, out)
                out.close()
            }
        }
        override fun close() {
        }
    }

    private fun pepBallotPath(ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "${pepDir}/${PEP_BALLOT_PREFIX}$id${JSON_SUFFIX}"
    }

    //////////////////////////////////////////////////////////////////////////////


    actual fun iteratePepBallots(): Iterable<BallotPep> {
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
}