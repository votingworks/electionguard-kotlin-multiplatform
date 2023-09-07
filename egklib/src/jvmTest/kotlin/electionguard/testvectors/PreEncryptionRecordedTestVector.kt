package electionguard.testvectors

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.encrypt.cast
import electionguard.input.ManifestBuilder
import electionguard.json2.*
import electionguard.preencrypt.*
import electionguard.preencrypt.MarkedPreEncryptedBallot
import electionguard.preencrypt.MarkedPreEncryptedContest
import electionguard.protoconvert.import
import electionguard.protoconvert.publishProto
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
import kotlin.io.use
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** Generate the election record information from a Pre-encrypted Ballot that has been voted. */
class PreEncryptionRecordedTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private var outputFile = "testOut/testvectors/PreEncryptionRecordedTestVector.json"

    val group = productionGroup()

    @Serializable
    data class PreEncryptionRecordedTestVector(
        val desc: String,
        val manifest: ManifestJson,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val primary_nonce: UInt256Json,
        val selected_codes: Map<String, List<String>>, // contest id to list of selection short codes
        val task: String,
        val expected_recorded_ballot: EncryptedBallotJson,
    )

    @Test
    fun testPreEncryptionRecordedTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("PreEncryptionRecordedTestVector.json").toString()
        makePreEncryptionRecordedTestVector()
        readPreEncryptionRecordedTestVector()
    }

    fun makePreEncryptionRecordedTestVector() {
        val publicKey = group.gPowP(group.randomElementModQ())
        val extendedBaseHash = UInt256.random()

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("contestOneVote")
                .addSelection("selection1", "candidate1")
                .addSelection("selection2", "candidate2")
                .addSelection("selection3", "candidate3")
                .done()
            .addContest("contestTwoVotes")
                .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
                .addSelection("selection4", "candidate4")
                .addSelection("selection5", "candidate5")
                .addSelection("selection6", "candidate6")
                .addSelection("selection7", "candidate7")
                .done()
            .build()

        val primaryNonce = UInt256.random()
        val preEncryptor = PreEncryptor(group, manifest, publicKey, extendedBaseHash, ::sigma)
        val pballot: PreEncryptedBallot = preEncryptor.preencrypt("ballot_id", "ballotStyle", primaryNonce)

        // vote
        val markedBallot = markBallotToLimit(pballot)
        val votedFor = markedBallot.contests.associate { it.contestId to it.selectedCodes }

        // record
        val recorder = Recorder(group, manifest, publicKey, extendedBaseHash, "device", ::sigma)
        val (recordedBallot, ciphertextBallot) = with(recorder) {
            markedBallot.record(primaryNonce)
        }

        // roundtrip through the proto, combines the recordedBallot
        val encryptedBallot = ciphertextBallot.cast()
        val proto = encryptedBallot.publishProto(recordedBallot)
        val fullEncryptedBallot = proto.import(group).unwrap()

        val preEncryptionRecordedTestVector = PreEncryptionRecordedTestVector(
            "Test recording a pre-encrypted ballot to election record",
            manifest.publishJson(),
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            primaryNonce.publishJson(),
            votedFor,
            "Compute the encrypted ballot from the ballot primary nonce and the selections voted for, spec 1.9 section 4.3",
            fullEncryptedBallot.publishJson(),
        )
        println(jsonFormat.encodeToString(preEncryptionRecordedTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(preEncryptionRecordedTestVector, out)
            out.close()
        }
    }

    fun readPreEncryptionRecordedTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: PreEncryptionRecordedTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<PreEncryptionRecordedTestVector>(inp)
            }

        val manifest = testVector.manifest.import()
        val publicKey = testVector.joint_public_key.import(group)
        val extendedBaseHash = testVector.extended_base_hash.import()
        val primaryNonce = testVector.primary_nonce.import()

        val preEncryptor = PreEncryptor(group, manifest, publicKey, extendedBaseHash, ::sigma)
        val pballot: PreEncryptedBallot = preEncryptor.preencrypt("ballot_id", "ballotStyle", primaryNonce)

        // vote
        val markedBallot = markBallotFromVotedFor(pballot, testVector.selected_codes)

        // record
        val recorder = Recorder(group, manifest, publicKey, extendedBaseHash, "device", ::sigma)
        val (recordedBallot, ciphertextBallot) = with(recorder) {
            markedBallot.record(primaryNonce)
        }

        // roundtrip through the proto, combines the recordedBallot
        val encryptedBallot = ciphertextBallot.cast()
        val proto = encryptedBallot.publishProto(recordedBallot)
        val fullEncryptedBallot = proto.import(group).unwrap()

        checkEquals(testVector.expected_recorded_ballot, fullEncryptedBallot)
    }

    fun checkEquals(expect : EncryptedBallotJson, actual : EncryptedBallot) {
        assertEquals(expect.ballot_id, actual.ballotId)
        assertEquals(expect.ballot_style_id, actual.ballotStyleId)
        assertEquals(expect.confirmation_code.import(), actual.confirmationCode)
        assertTrue(expect.code_baux.toByteArray().contentEquals(actual.codeBaux))
        // assertEquals(expect.timestamp, actual.timestamp)
        // assertEquals(expect.state, actual.state)
        assertEquals(expect.is_preencrypt, actual.isPreencrypt)

        assertEquals(expect.contests.size, actual.contests.size)
        expect.contests.zip(actual.contests).forEach { (expectContest, actualContest) ->
            assertEquals(expectContest.contest_id, actualContest.contestId)
            assertEquals(expectContest.sequence_order, actualContest.sequenceOrder)
//            assertEquals(expectContest.contest_hash.import(), actualContest.contestHash)
            checkEquals(expectContest.pre_encryption!!, actualContest.preEncryption!!)

            assertEquals(expectContest.selections.size, actualContest.selections.size)
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.selection_id, actualSelection.selectionId)
                assertEquals(expectSelection.sequence_order, actualSelection.sequenceOrder)
                assertEquals(expectSelection.encrypted_vote.import(group), actualSelection.encryptedVote)
            }

        }
        println("ballot ${actual.ballotId} is ok")
    }

    fun checkEquals(expectPre : PreEncryptionJson, actualPre : EncryptedBallot.PreEncryption) {
        assertEquals(expectPre.preencryption_hash.import(), actualPre.preencryptionHash)

        assertEquals(expectPre.all_selection_hashes.size, actualPre.allSelectionHashes.size)
        expectPre.all_selection_hashes.zip(actualPre.allSelectionHashes).forEach { (expectHash, actualHash) ->
            assertEquals(expectHash.import(), actualHash)
        }

        assertEquals(expectPre.selected_vectors.size, actualPre.selectedVectors.size)
        expectPre.selected_vectors.zip(actualPre.selectedVectors).forEach { (expectVector, actualVector) ->
            assertEquals(expectVector.selection_hash.import(), actualVector.selectionHash)
            assertEquals(expectVector.short_code, actualVector.shortCode)

            assertEquals(expectVector.encryptions.size, actualVector.encryptions.size)
            expectVector.encryptions.zip(actualVector.encryptions).forEach { (expectEncryption, actualEncryption) ->
                assertEquals(expectEncryption.import(group), actualEncryption)
            }
        }

    }

    // pick all selections 0..limit-1
    internal fun markBallotToLimit(pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
        val pcontests = mutableListOf<MarkedPreEncryptedContest>()
        for (pcontest in pballot.contests) {
            val shortCodes = mutableListOf<String>()
            val selections = mutableListOf<String>()
            val nselections = pcontest.selections.size
            val doneIdx = mutableSetOf<Int>()

            while (doneIdx.size < pcontest.votesAllowed) {
                val idx = Random.nextInt(nselections)
                if (!doneIdx.contains(idx)) {
                    shortCodes.add(sigma(pcontest.selections[idx].selectionHash.toUInt256()))
                    selections.add(pcontest.selections[idx].selectionId)
                    doneIdx.add(idx)
                }
            }

            pcontests.add(
                MarkedPreEncryptedContest(
                    pcontest.contestId,
                    shortCodes,
                    selections,
                )
            )
        }

        return MarkedPreEncryptedBallot(
            pballot.ballotId,
            pballot.ballotStyleId,
            pcontests,
        )
    }

    // male a MarkedPreEncryptedBallot from the voted_for
    internal fun markBallotFromVotedFor(pballot: PreEncryptedBallot, voted_for: Map<String, List<String>>): MarkedPreEncryptedBallot {
        val pcontests = mutableListOf<MarkedPreEncryptedContest>()
        for (pcontest in pballot.contests) {
            val selections = mutableListOf<String>()

            pcontests.add(
                MarkedPreEncryptedContest(
                    pcontest.contestId,
                    voted_for[pcontest.contestId] ?: emptyList(),
                    selections,
                )
            )
        }

        return MarkedPreEncryptedBallot(
            pballot.ballotId,
            pballot.ballotStyleId,
            pcontests,
        )
    }
}