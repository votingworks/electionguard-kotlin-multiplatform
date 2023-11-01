package electionguard.testvectors

import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.core.Base16.fromHexSafe
import electionguard.core.Base16.toHex
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor
import electionguard.cli.ManifestBuilder
import electionguard.input.RandomBallotProvider
import electionguard.json2.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    private val jsonFormat = Json { prettyPrint = true }
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

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash, "device")
        val ballots = RandomBallotProvider(manifest, nBallots).ballots()
        val codeSeed = UInt256.random()
        val eballots = encryptor.encryptChain(ballots, codeSeed)

        val confirmationCodeTestVector = BallotChainingTestVector(
            "Test ballot confirmation code chaining, section 3.4.3",
            publicKey.toHex(),
            extendedBaseHash.toHex(),
            ballots.map { it.publishJsonE() },
            eballots.map { it.publishJson() },
        )
        println(jsonFormat.encodeToString(confirmationCodeTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(confirmationCodeTestVector, out)
            out.close()
        }
    }

    fun readBallotChainingTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: BallotChainingTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<BallotChainingTestVector>(inp)
            }

        val publicKey = ElGamalPublicKey(group.base16ToElementModPsafe(testVector.joint_public_key))
        val extendedBaseHash = UInt256(testVector.extended_base_hash.fromHexSafe())
        val ballotsZipped = testVector.ballots.zip(testVector.expected_encrypted_ballots)

        var prevCode : ByteArray? = null
        ballotsZipped.forEach { (ballot, eballot) ->
            val manifest = PlaintextBallotJsonManifestFacade(ballot)
            val encryptor = Encryptor(group, manifest, publicKey, extendedBaseHash, "device")
            val ballotNonce = eballot.ballotNonce.import()
            val codeBaux : ByteArray = eballot.codeBaux.fromHexSafe()
            val cyberBallot = encryptor.encrypt(ballot.import(), codeBaux, ballotNonce)
            checkEquals(eballot, cyberBallot)
            if (prevCode != null) {
                assertTrue(prevCode.contentEquals(codeBaux))
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