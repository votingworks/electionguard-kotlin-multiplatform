package electionguard.mixnet

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.core.fileReadText
import electionguard.core.productionGroup
import electionguard.decrypt.CiphertextDecryptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MixnetBallotJsonReaderTest {
    val topdir = "src/commonTest/data/mixnet/working/vf"

    var fileSystem = FileSystems.getDefault()
    var fileSystemProvider = fileSystem.provider()
    val group = productionGroup()
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun testInputWrap() {
        val result = readMixnetBallotWrapped("$topdir/input-ciphertexts.json")
        assertTrue(result is Ok)
        println(result.unwrap().show())
    }

    @Test
    fun testOutputWrap() {
        val result = readMixnetBallotWrapped("$topdir/after-mix-2-ciphertexts.json")
        assertTrue(result is Ok)
        println(result.unwrap().show())
    }

    @Test
    fun testInputImport() {
        val result = readMixnetBallotWrapped("$topdir/input-ciphertexts.json")
        assertTrue(result is Ok)
        val converted = result.unwrap().import(group)
        println(converted.forEachIndexed{ idx, it -> println("ballot ${idx+1}\n${it.show()}")} )
    }

    @Test
    fun testOutputImport() {
        val converted = readMixnetJsonBallots(group, "$topdir/after-mix-2-ciphertexts.json")
        println(converted.forEachIndexed{ idx, it -> println("ballot ${idx+1}\n${it.show()}")} )
    }

    @Test
    fun testOutputDecrypt() {
        val decryptor = CiphertextDecryptor(
            group,
            "src/commonTest/data/mixnet/working/eg/keyceremony",
            "src/commonTest/data/mixnet/working/eg/trustees",
        )

        val converted = readMixnetJsonBallots(group, "$topdir/after-mix-2-ciphertexts.json")
        converted.forEachIndexed { idx, it ->
            it.ciphertext.forEach { ciphertext ->
                val vote = decryptor.decrypt(ciphertext)
                assertNotNull(vote)
            }
            println("ballot ${idx + 1} OK")
        }
    }

    private fun readMixnetBallot(filename: String): Result<MixnetBallotJson, String> =
        try {
            var mixnetInput: MixnetBallotJson
            val path = Path.of(filename)
            fileSystemProvider.newInputStream(path).use { inp ->
                mixnetInput = jsonReader.decodeFromStream<MixnetBallotJson>(inp)
            }
            Ok(mixnetInput)
        } catch (e: Exception) {
            e.printStackTrace()
            Err(e.message ?: "readMixnetInput on $filename error")
        }

    private fun readMixnetBallotWrapped(filename: String): Result<MixnetBallotJson, String> =
        try {
            val text = fileReadText(filename)
            val wrap = "{ \"wtf\": $text }"
            var mixnetInput: MixnetBallotJson = jsonReader.decodeFromString<MixnetBallotJson>(wrap)
            Ok(mixnetInput)
        } catch (e: Exception) {
            e.printStackTrace()
            Err(e.message ?: "readMixnetInput on $filename error")
        }
}