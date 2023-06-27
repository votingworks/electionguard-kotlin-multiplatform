package electionguard.publish

import electionguard.ballot.ElectionConstants
import electionguard.core.productionGroup
import electionguard.json2.ElectionConstantsJson
import electionguard.json2.import
import electionguard.verifier.verifyChallengedBallots
import electionguard.verifier.verifyEncryptedBallots
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test


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
            val json = Json.decodeFromStream<ElectionConstantsJson>(inp)
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