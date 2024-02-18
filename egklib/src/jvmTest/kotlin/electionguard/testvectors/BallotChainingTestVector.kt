package electionguard.testvectors

import electionguard.ballot.*
import electionguard.core.*
import electionguard.core.Base16.fromHexSafe
import electionguard.core.Base16.toHex
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor
import electionguard.cli.ManifestBuilder
import electionguard.encrypt.AddEncryptedBallot
import electionguard.input.RandomBallotProvider
import electionguard.json2.*
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class BallotChainingTestVector {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    private var outputFile = "testOut/testvectors/BallotChainingTestVector.json"

    val group = productionGroup()
    val nBallots = 12

    @Serializable
    data class EncryptedBallotJson(
        val ballotId: String,
        val ballotNonce: UInt256Json,
        val codeBaux: String,
        val task: String,
        val expected_confirmationCode: UInt256Json,
        val contests: List<EncryptedContestJson>,
    )

    @Serializable
    data class EncryptedContestJson(
        val contestId: String,
        val task: String,
        val expected_contestHash: UInt256Json,
    )

    fun CiphertextBallot.publishJson(): EncryptedBallotJson {
        return EncryptedBallotJson(
            this.ballotId,
            this.ballotNonce.publishJson(),
            this.codeBaux.toHex(),
            "Compute confirmation code (eq 58)",
            this.confirmationCode.publishJson(),
            this.contests.map { EncryptedContestJson(
                it.contestId,
                "Compute contest hash (eq 57)",
                it.contestHash.publishJson()) }
        )
    }

    @Serializable
    data class BallotChainingTestVector(
        val desc: String,
        val joint_public_key: String,
        val extended_base_hash: String,
        val configBaux0: ByteArray,
        val ballots: List<PlaintextBallotJsonV>,
        val expected_encrypted_ballots: List<EncryptedBallotJson>,
    )

    @Test
    fun testBallotChainingTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("BallotChainingTestVector.json").toString()
        makeBallotChainingTestVector()
        readBallotChainingTestVector()
    }

    fun makeBallotChainingTestVector() {
        val publicKey = group.gPowP(group.randomElementModQ())
        val extendedBaseHash = UInt256.random()
        val configBaux0 = UInt256.random().bytes

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val encryptor = AddEncryptedBallot(
            group,
            manifest,
            true,
            configBaux0,
            ElGamalPublicKey(publicKey),
            extendedBaseHash,
            "device",
            "workingDir",
            "invalidDir",
            isJson = true,
        )

        val cballots = mutableListOf<CiphertextBallot>()
        val ballots = RandomBallotProvider(manifest, nBallots).ballots()

        var count = 1
        ballots.forEach { ballot ->
            val errs = ErrorMessages("Ballot ${ballot.ballotId}")
            val cballot = encryptor.encrypt(ballot, errs)
            if (errs.hasErrors()) {
                println(errs)
            } else {
                cballots.add(cballot!!)
                count++
            }
        }

        val confirmationCodeTestVector = BallotChainingTestVector(
            "Test ballot confirmation code chaining, section 3.4.3",
            publicKey.toHex(),
            extendedBaseHash.toHex(),
            configBaux0,
            ballots.map { it.publishJsonE() },
            cballots.map { it.publishJson() },
        )
        // println(jsonFormat.encodeToString(confirmationCodeTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonReader.encodeToStream(confirmationCodeTestVector, out)
            out.close()
        }
    }

    fun readBallotChainingTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: BallotChainingTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                jsonReader.decodeFromStream<BallotChainingTestVector>(inp)
            }

        val publicKey = ElGamalPublicKey(group.base16ToElementModP(testVector.joint_public_key)!!)
        val extendedBaseHash = UInt256(testVector.extended_base_hash.fromHexSafe())
        val configBaux0 : ByteArray = testVector.configBaux0
        val ballotsZipped = testVector.ballots.zip(testVector.expected_encrypted_ballots)

        var prevCode : ByteArray? = null
        ballotsZipped.forEach { (ballot, eballot) ->
            val manifest = PlaintextBallotJsonManifestFacade(ballot)
            val encryptor = Encryptor(group, manifest, publicKey, extendedBaseHash, "device")
            val ballotNonce = eballot.ballotNonce.import()
            val codeBaux : ByteArray = eballot.codeBaux.fromHexSafe()
            val errs = ErrorMessages("Ballot ${eballot.ballotId}")
            val cyberBallot = encryptor.encrypt(ballot.import(), codeBaux, errs, ballotNonce)
            checkEquals(eballot, cyberBallot!!)
            if (prevCode != null) {
                val expect = prevCode!! + configBaux0
                assertTrue(expect.contentEquals(codeBaux))
            }
            prevCode = cyberBallot.confirmationCode.bytes
        }
    }

    fun checkEquals(expect : EncryptedBallotJson, actual : CiphertextBallot) {
        assertEquals(expect.ballotId, actual.ballotId)
        assertEquals(expect.expected_confirmationCode.import(), actual.confirmationCode)

        expect.contests.zip(actual.contests).forEach { (expectContest, actualContest) ->
            assertEquals(expectContest.expected_contestHash.import(), actualContest.contestHash)
        }
        println("ballot ${actual.ballotId} is ok")
    }
}