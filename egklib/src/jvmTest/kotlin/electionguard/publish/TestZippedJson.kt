package electionguard.publish

import electionguard.cli.RunVerifier
import electionguard.core.productionGroup
import electionguard.json2.ElectionConstantsJson
import electionguard.json2.import
import electionguard.util.ErrorMessages
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions.assertNotNull
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test

// run verifier on zipped JSON record, only supported on JVM
@OptIn(ExperimentalSerializationApi::class)
class TestZippedJson {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    val inputDir = "src/commonTest/data/workflow/allAvailableJson"
    val zippedJson = "testOut/allAvailableJson.zip"
    val fs: FileSystem
    val fsp: FileSystemProvider

    init {
        zipFolder(File(inputDir), File(zippedJson))
        fs = FileSystems.newFileSystem(Path.of(zippedJson), mutableMapOf<String, String>())
        fsp = fs.provider()
    }



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
        fsp.newInputStream(path).use { inp ->
            val json = jsonReader.decodeFromStream<ElectionConstantsJson>(inp)
            val result = json.import(ErrorMessages("readConstants"))
            assertNotNull(result)
            println("constants = ${result}")
        }
    }

    @Test
    fun testVerifyEncryptedBallots() {
        RunVerifier.verifyEncryptedBallots(productionGroup(), zippedJson, 11)
        RunVerifier.verifyChallengedBallots(productionGroup(), zippedJson)
    }
}

fun zipFolder(unzipped: File, zipped: File) {
    if (!unzipped.exists() || !unzipped.isDirectory) return
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipped))).use { zos ->
        unzipped.walkTopDown().filter { it.absolutePath != unzipped.absolutePath }.forEach { file ->
            val zipFileName = file.absolutePath.removePrefix(unzipped.absolutePath).removePrefix(File.separator)
            val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "" )}")
            zos.putNextEntry(entry)
            if (file.isFile) file.inputStream().use { it.copyTo(zos) }
        }
    }
}