package electionguard.testvectors

import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.cli.ManifestBuilder
import electionguard.json2.*
import electionguard.preencrypt.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.io.TempDir
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/** Generate Pre-encrypted Ballot */
class PreEncryptionTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private var outputFile = "testOut/testvectors/PreEncryptionTestVector.json"

    val group = productionGroup()

    @Serializable
    data class PreEncryptedBallotJson(
        val ballotId: String,
        val ballotStyleId: String,
        val primaryNonce: UInt256Json,
        val contests: List<PreEncryptedContestJson>,
        val task: String,
        val expected_confirmationCode: UInt256Json,
    )

    @Serializable
    data class PreEncryptedContestJson(
        val contestId: String,
        val sequenceOrder: Int,
        val votesAllowed: Int,
        val selections: List<PreEncryptedSelectionJson>,
        val task: String,
        val expected_preencryptionHash: UInt256Json,
    )

    @Serializable
    data class PreEncryptedSelectionJson(
        val selectionId: String,
        val sequenceOrder: Int,
        val shortCode: String,
        val task: String,
        val expected_selectionHash: ElementModQJson,
        val expected_selectionVector: List<ElGamalCiphertextJson>,
        val expected_selectionNonces: List<ElementModQJson>,
    )

    fun PreEncryptedBallot.publishJson(): PreEncryptedBallotJson {
        val contests = this.contests.map { pcontest ->

            PreEncryptedContestJson(
                pcontest.contestId,
                pcontest.sequenceOrder,
                pcontest.votesAllowed,
                pcontest.selections.map {
                    PreEncryptedSelectionJson(
                        it.selectionId,
                        it.sequenceOrder,
                        it.shortCode,
                        "Compute selection hash (eq 92,93), vector (eq 92), and nonces (eq 96)",
                        it.selectionHash.publishJson(),
                        it.selectionVector.map { it.publishJson() },
                        it.selectionNonces.map { it.publishJson() },
                    )
                },
                "Compute contest preencryptionHash, eq 94",
                pcontest.preencryptionHash.publishJson(),
            )
        }
        return PreEncryptedBallotJson(this.ballotId, this.ballotStyleId, this.primaryNonce.publishJson(),
            contests,
            "Compute ballot confirmation code, eq 95",
            this.confirmationCode.publishJson())
    }

    @Serializable
    data class PreEncryptionTestVector(
        val desc: String,
        val manifest: ManifestJson,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val primary_nonce: UInt256Json,
        val preencrypted_ballot: PreEncryptedBallotJson,
        // val expected_encrypted_ballots: List<EncryptedBallotJson>,
    )

    @Test
    fun testPreEncryptionTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("PreEncryptionTestVector.json").toString()
        makePreEncryptionTestVector()
        readPreEncryptionTestVector()
    }

    fun makePreEncryptionTestVector() {
        val publicKey = group.gPowP(group.randomElementModQ())
        val extendedBaseHash = UInt256.random()

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val primaryNonce = UInt256.random()

        val preEncryptor = PreEncryptor(group, manifest, publicKey, extendedBaseHash, ::sigma)
        val pballot: PreEncryptedBallot = preEncryptor.preencrypt("ballot_id", "ballotStyle", primaryNonce)

        val preEncryptionTestVector = PreEncryptionTestVector(
            "Test ballot pre-encryption",
            manifest.publishJson(),
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            primaryNonce.publishJson(),
            pballot.publishJson(),
        )
        println(jsonFormat.encodeToString(preEncryptionTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(preEncryptionTestVector, out)
            out.close()
        }
    }

    fun readPreEncryptionTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: PreEncryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<PreEncryptionTestVector>(inp)
            }

        val manifest = testVector.manifest.import()
        val publicKey = testVector.joint_public_key.import(group) ?: throw IllegalArgumentException("readPreEncryptionTestVector malformed joint_public_key")
        val extendedBaseHash = testVector.extended_base_hash.import() ?: throw IllegalArgumentException("readPreEncryptionTestVector malformed extended_base_hash")
        val primaryNonce = testVector.primary_nonce.import() ?: throw IllegalArgumentException("readPreEncryptionTestVector malformed primary_nonce")

        val preEncryptor = PreEncryptor(group, manifest, publicKey, extendedBaseHash, ::sigma)
        val pballot: PreEncryptedBallot = preEncryptor.preencrypt("ballot_id", "ballotStyle", primaryNonce)

        checkEquals(testVector.preencrypted_ballot, pballot)
    }

    fun checkEquals(expect : PreEncryptedBallotJson, actual : PreEncryptedBallot) {
        assertEquals(expect.ballotId, actual.ballotId)
        assertEquals(expect.ballotStyleId, actual.ballotStyleId)
        assertEquals(expect.primaryNonce.import(), actual.primaryNonce)
        assertEquals(expect.expected_confirmationCode.import(), actual.confirmationCode)
        assertEquals(expect.contests.size, actual.contests.size)

        expect.contests.zip(actual.contests).forEach { (expectContest, actualContest) ->
            assertEquals(expectContest.contestId, actualContest.contestId)
            assertEquals(expectContest.sequenceOrder, actualContest.sequenceOrder)
            assertEquals(expectContest.votesAllowed, actualContest.votesAllowed)
            assertEquals(expectContest.expected_preencryptionHash.import(), actualContest.preencryptionHash)

            assertEquals(expectContest.selections.size, actualContest.selections.size)
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.selectionId, actualSelection.selectionId)
                assertEquals(expectSelection.sequenceOrder, actualSelection.sequenceOrder)
                assertEquals(expectSelection.expected_selectionHash.import(group), actualSelection.selectionHash)
                expectSelection.expected_selectionVector.zip(actualSelection.selectionVector).forEach { (expectSelectionVector, actualSelectionVector) ->
                    assertEquals(expectSelectionVector.import(group), actualSelectionVector) }
                expectSelection.expected_selectionNonces.zip(actualSelection.selectionNonces).forEach { (expectSelectionNonce, actualSelectionNonce) ->
                    assertEquals(expectSelectionNonce.import(group), actualSelectionNonce) }
            }

        }
        println("ballot ${actual.ballotId} is ok")
    }
}