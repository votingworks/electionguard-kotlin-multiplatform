package electionguard.publish

import electionguard.ballot.ElectionConstants
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.json.ConstantsJson
import electionguard.json.import
import electionguard.verifier.verifyChallengedBallots
import electionguard.verifier.verifyEncryptedBallots
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue


// run verifier on zipped JSON record, only supported on JVM
@OptIn(ExperimentalSerializationApi::class)
class TestZippedJson {
    val zippedJson = "src/commonTest/data/testElectionRecord/jsonZip/json25.zip"
    val fs = FileSystems.newFileSystem(Path.of(zippedJson), mutableMapOf<String, String>())
    val fsp = fs.provider()

    @Test
    fun showEntryPaths() {
        val wtf = fs.rootDirectories
        wtf.forEach { root ->
                Files.walk(root).forEach { path -> println(path) }
            }
    }

    @Test
    fun readConstants() {
        val path : Path = fs.getPath("/constants.json")
        var constants: ElectionConstants
        fsp.newInputStream(path).use { inp ->
            val json = Json.decodeFromStream<ConstantsJson>(inp)
            constants = json.import()
            println("constants = $constants")
        }
    }

    @Test
    fun testVerifyEncryptedBallots() {
        verifyEncryptedBallots(productionGroup(), zippedJson, 11)
        verifyChallengedBallots(productionGroup(), zippedJson)
    }

}