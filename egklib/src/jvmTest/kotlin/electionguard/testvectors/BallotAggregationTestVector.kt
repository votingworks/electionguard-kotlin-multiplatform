@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.testvectors

import electionguard.ballot.*
import electionguard.core.*
import electionguard.json2.*
import electionguard.encrypt.Encryptor
import electionguard.encrypt.cast
import electionguard.cli.ManifestBuilder
import electionguard.input.RandomBallotProvider
import electionguard.tally.AccumulateTally
import kotlinx.serialization.ExperimentalSerializationApi
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

class BallotAggregationTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private var outputFile = "testOut/testvectors/BallotAggregationTestVector.json"

    val group = productionGroup()
    val nBallots = 11

    @Serializable
    data class BallotAggregationTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val encrypted_ballots: List<EncryptedBallotJsonV>,
        val task: String,
        val expected_encrypted_tally : EncryptedTallyJson,
    )

    @Test
    fun BallotAggregationTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("BallotAggregationTestVector.json").toString()
        makeBallotAggregationTestVector()
        readBallotAggregationTestVector()
    }

    fun makeBallotAggregationTestVector() {
        val publicKey = group.gPowP(group.randomElementModQ())
        val extendedBaseHash = UInt256.random()

        val ebuilder = ManifestBuilder("makeBallotAggregationTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash, "device")
        val eballots = RandomBallotProvider(manifest, nBallots).ballots().map { ballot ->
            encryptor.encrypt(ballot, ByteArray(0))
        }

        val accumulator = AccumulateTally(group, manifest, "makeBallotAggregationTestVector")
        eballots.forEach { eballot ->
            accumulator.addCastBallot(eballot.cast())
        }
        val tally = accumulator.build()

        val ballotAggregationTestVector = BallotAggregationTestVector(
            "Test ballot aggregation",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            eballots.map { it.publishJsonE() },
            "Compute tally over all encrypted ballots, eq 63",
            tally.publishJson()
        )
        println(jsonFormat.encodeToString(ballotAggregationTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(ballotAggregationTestVector, out)
            out.close()
        }
    }

    fun readBallotAggregationTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: BallotAggregationTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<BallotAggregationTestVector>(inp)
            }

        val eballots: List<EncryptedBallotIF> = testVector.encrypted_ballots.map { it.import(group) }
        val manifest = EncryptedBallotJsonManifestFacade(testVector.encrypted_ballots[0])

        val accumulator = AccumulateTally(group, manifest, "makeBallotAggregationTestVector")
        eballots.forEach { eballot -> accumulator.addCastBallot(eballot) }
        val tally = accumulator.build()

        testVector.expected_encrypted_tally.contests.zip(tally.contests).forEach { (expectContest, actualContest) ->
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.encrypted_vote.import(group), actualSelection.encryptedVote)
            }
        }
    }

}