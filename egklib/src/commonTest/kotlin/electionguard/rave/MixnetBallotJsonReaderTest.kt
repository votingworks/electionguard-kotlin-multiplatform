package electionguard.rave

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.publish.makeConsumer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MixnetBallotJsonReaderTest {
    val topdir = "src/commonTest/data/rave/working"
    // val topdir = "/home/stormy/tmp/working"

    var fileSystem = FileSystems.getDefault()
    var fileSystemProvider = fileSystem.provider()
    val group = productionGroup()
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun testMixnetInput() {
        val result = readMixnetBallotArray("$topdir/vf/input-ciphertexts.json")
        assertTrue(result is Ok)
        println(result.unwrap().show())
    }

    @Test
    fun testMixnetOutput() {
        val result = readMixnetBallotArray("$topdir/vf/after-mix-2-ciphertexts.json")
        assertTrue(result is Ok)
        println(result.unwrap().show())
    }

    @Test
    fun testMixnetOutputDecrypt() {
        val decryptor = CiphertextDecryptor(
            group,
            "$topdir/eg/keyceremony",
            "$topdir/eg/trustees",
        )

        val converted = readMixnetJsonBallots(group, "$topdir/vf/after-mix-2-ciphertexts.json")
        converted.forEachIndexed { idx, it ->
            it.ciphertext.forEach { ciphertext ->
                val vote = decryptor.decrypt(ciphertext)
                print("$vote,")
                assertNotNull(vote)
            }
            println("\nballot ${idx + 1} OK")
        }
    }

    @Test
    fun testEncryptedBallotDecrypt() {
        val decryptor = CiphertextDecryptor(
            group,
            "$topdir/eg/keyceremony",
            "$topdir/eg/trustees",
        )

        val mixnetBallots = mutableListOf<MixnetBallot>()
        val consumer = makeConsumer(group, "$topdir/eg/encryption")
        consumer.iterateAllCastBallots().forEach { encryptedBallot ->
            val ciphertexts = mutableListOf<ElGamalCiphertext>()
            ciphertexts.add(encryptedBallot.encryptedSn!!) // always the first one
            encryptedBallot.contests.forEach { contest ->
                contest.selections.forEach { selection ->
                    ciphertexts.add(selection.encryptedVote)
                }
            }
            mixnetBallots.add(MixnetBallot(ciphertexts))
        }

        mixnetBallots.forEachIndexed { idx, it ->
            it.ciphertext.forEach { ciphertext ->
                val vote = decryptor.decrypt(ciphertext)
                print("$vote,")
                assertNotNull(vote)
            }
            println("\nballot ${idx + 1} OK")
        }
    }

    @Test
    fun testEncryptedBallotMatch() {
        val mixnetFile = "$topdir/vf/after-mix-2-ciphertexts.json"
        val decryptor = CiphertextDecryptor(
            group,
            "$topdir/eg/keyceremony",
            "$topdir/eg/trustees",
        )

        val encryptedBallots = mutableMapOf<Int, EncryptedBallot>() // key is the SN (for now)
        val consumer = makeConsumer(group, "$topdir/eg/encryption")
        consumer.iterateAllCastBallots().forEach { encryptedBallot ->
            val sn = decryptor.decryptPep(encryptedBallot.encryptedSn!!)!!
            encryptedBallots[sn.hashCode()] = encryptedBallot
        }
        val blindingTrustees = mutableListOf<PepTrustee>()
        repeat(3) {
            blindingTrustees.add(PepTrustee(it, group))
        }

        val mixnetPep = MixnetPepBlindTrust(
            group,
            decryptor.init.extendedBaseHash,
            decryptor.init.jointPublicKey(),
            blindingTrustees,
            decryptor
        )

        val mixnetBallots = readMixnetJsonBallots(group, mixnetFile)
        mixnetBallots.forEachIndexed { idx, it ->
            val first = decryptor.decryptPep(it.ciphertext[0])!!
            val match = encryptedBallots[first.hashCode()]
            if (match == null) {
                println("Match ballot ${idx + 1} NOT FOUND")
            } else {
                println("Match ballot ${idx + 1} FOUND")
                val result = mixnetPep.testEquivalent(match, MixnetBallot(it.ciphertext.subList(1, it.ciphertext.size)))
                if (result is Err) println(result)
                assertTrue(result is Ok)
            }
        }
    }

    private fun readMixnetBallotArray(filename: String): Result<MixnetBallotJson, String> =
        try {
            val path = Path.of(filename)
            val mixnetBallotJson : MixnetBallotJson
            fileSystemProvider.newInputStream(path).use { inp ->
                val lists = jsonReader.decodeFromStream<List<List<List<String>>>>(inp)
                mixnetBallotJson = MixnetBallotJson(lists)
            }
            Ok(mixnetBallotJson)
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