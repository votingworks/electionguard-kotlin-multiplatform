package electionguard.testvectors

import electionguard.ballot.Manifest
import electionguard.ballot.ManifestIF
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.core.Base16.fromHex
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor
import electionguard.input.ManifestBuilder
import electionguard.input.RandomBallotProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream
import java.nio.file.FileSystems
import kotlin.test.Test
import kotlin.test.assertEquals

class BallotEncryptionTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private val outputFile = "testOut/testvectors/BallotEncryptionTestVector.json"

    val group = productionGroup()
    val nBallots = 1

    @Serializable
    data class BallotJson(
        val ballotId: String,
        val ballotStyle: String,
        val contests: List<ContestJson>,
    )

    @Serializable
    data class ContestJson(
        val contestId: String,
        val sequenceOrder: Int,
        val votesAllowed: Int,
        val selections: List<SelectionJson>,
    )

    @Serializable
    data class SelectionJson(
        val selectionId: String,
        val sequenceOrder: Int,
        val vote: Int,
    )

    fun PlaintextBallot.publishJson(): BallotJson {
        val contests = this.contests.map { contest ->
            ContestJson(contest.contestId, contest.sequenceOrder, 1,
                contest.selections.map { SelectionJson(it.selectionId, it.sequenceOrder, it.vote) })
        }
        return BallotJson(this.ballotId, this.ballotStyle, contests)
    }

    fun BallotJson.import(): PlaintextBallot {
        val contests = this.contests.map { contest ->
            PlaintextBallot.Contest(contest.contestId, contest.sequenceOrder,
                contest.selections.map { PlaintextBallot.Selection(it.selectionId, it.sequenceOrder, it.vote) })
        }
        return PlaintextBallot(this.ballotId, this.ballotStyle, contests)
    }

    @Serializable
    data class EncryptedBallotJson(
        val ballotId: String,
        val ballotNonce: UInt256Json,
        val contests: List<EncryptedContestJson>,
    )

    @Serializable
    data class EncryptedContestJson(
        val contestId: String,
        val sequenceOrder: Int,
        val expected_proof: RangeProofJson,
        val selections: List<EncryptedSelectionJson>,
    )

    @Serializable
    data class EncryptedSelectionJson(
        val selectionId: String,
        val sequenceOrder: Int,
        val expected_encrypted_vote: ElGamalCiphertextJson,
        val expected_proof: RangeProofJson,
        val expected_sequence_nonce: ElementModQJson,
    )

    @Serializable
    data class RangeProofJson(
        val proofs: List<ChaumPedersenJson>,
    )

    fun ChaumPedersenRangeProofKnownNonce.publishJson() = RangeProofJson( this.proofs.map { it.publishJson() })
    fun RangeProofJson.import(group: GroupContext) = ChaumPedersenRangeProofKnownNonce(this.proofs.map { it.import(group) })

    @Serializable
    data class ChaumPedersenJson(
        val challenge: ElementModQJson,
        val response: ElementModQJson,
        //val u_nonce: UInt256Json, // eq 23
        //val v_nonce: UInt256Json, // eq 24
    )

    fun ChaumPedersenProof.publishJson() = ChaumPedersenJson(this.c.publishJson(), this.r.publishJson())
    fun ChaumPedersenJson.import(group: GroupContext) = ChaumPedersenProof(this.challenge.import(group), this.response.import(group))

    @Serializable
    data class ElGamalCiphertextJson(
        val pad: ElementModPJson,
        val data: ElementModPJson
    )

    fun ElGamalCiphertext.publishJson() = ElGamalCiphertextJson(this.pad.publishJson(), this.data.publishJson())
    fun ElGamalCiphertextJson.import(group: GroupContext) = ElGamalCiphertext(this.pad.import(group), this.data.import(group))

    fun CiphertextBallot.publishJson(): EncryptedBallotJson {
        val contests = this.contests.map { pcontest ->
            EncryptedContestJson(pcontest.contestId, pcontest.sequenceOrder, pcontest.proof.publishJson(),
                pcontest.selections.map {
                    EncryptedSelectionJson(
                        it.selectionId,
                        it.sequenceOrder,
                        it.ciphertext.publishJson(),
                        it.proof.publishJson(),
                        it.selectionNonce.publishJson()
                    )
                })
        }
        return EncryptedBallotJson(this.ballotId, this.ballotNonce.publishJson(), contests)
    }

    @Serializable
    data class BallotEncryptionTestVector(
        val desc: String,
        val joint_public_key: String,
        val extended_base_hash: String,
        val ballots: List<BallotJson>,
        val expected_encrypted_ballots: List<EncryptedBallotJson>,
    )

    @Test
    fun testBallotEncryptionTestVector() {
        makeBallotEncryptionTestVector()
        readBallotEncryptionTestVector()
    }

    fun makeBallotEncryptionTestVector() {
        val publicKey = group.gPowP(group.randomElementModQ())
        val extendedBaseHash = UInt256.random()

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val ballots: List<PlaintextBallot> = RandomBallotProvider(manifest, nBallots).ballots()
        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash)

        val eballots = mutableListOf<CiphertextBallot>()
        ballots.forEach { ballot ->
            eballots.add(encryptor.encrypt(ballot))
        }

        val ballotEncryptionTestVector = BallotEncryptionTestVector(
            "Test ballot encryption",
            publicKey.toHex(),
            extendedBaseHash.toHex(),
            ballots.map { it.publishJson() },
            eballots.map { it.publishJson() },
        )
        println(jsonFormat.encodeToString(ballotEncryptionTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(ballotEncryptionTestVector, out)
            out.close()
        }
    }

    fun readBallotEncryptionTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: BallotEncryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<BallotEncryptionTestVector>(inp)
            }

        val publicKey = group.safeBase16ToElementModP(testVector.joint_public_key)
        val extendedBaseHash = UInt256(testVector.extended_base_hash.fromHex()!!)
        val ballotsZipped = testVector.ballots.zip(testVector.expected_encrypted_ballots)

        ballotsZipped.forEach { (ballot, eballot) ->
            val manifest = ManifestFacade(ballot)
            val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash)
            val ballotNonce = eballot.ballotNonce.import()
            val cyberBallot = encryptor.encrypt(ballot.import(), ballotNonce)
            checkEquals(eballot, cyberBallot)
        }
    }

    // TODO we are relying on the proof nonces being derived from ballotNonce
    //   change to pass the nonces and use them
    // TODO confirmation code
    fun checkEquals(expect : EncryptedBallotJson, actual : CiphertextBallot) {
        assertEquals(expect.ballotId, actual.ballotId)
        assertEquals(expect.contests.size, actual.contests.size)

        expect.contests.zip(actual.contests).forEach { (expectContest, actualContest) ->
            assertEquals(expectContest.contestId, actualContest.contestId)
            assertEquals(expectContest.sequenceOrder, actualContest.sequenceOrder)
            assertEquals(expectContest.expected_proof.import(group), actualContest.proof)
            assertEquals(expectContest.selections.size, actualContest.selections.size)

            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.selectionId, actualSelection.selectionId)
                assertEquals(expectSelection.sequenceOrder, actualSelection.sequenceOrder)
                assertEquals(expectSelection.expected_encrypted_vote.import(group), actualSelection.ciphertext)
                assertEquals(expectSelection.expected_sequence_nonce.import(group), actualSelection.selectionNonce)
                assertEquals(expectSelection.expected_proof.import(group), actualSelection.proof)
            }

        }
        println("ballot ${actual.ballotId} is ok")
    }

    class ManifestFacade(ballot : BallotJson) : ManifestIF {
        override val contests : List<ContestFacade>
        init {
            this.contests = ballot.contests.map { bc ->
                ContestFacade(
                    bc.contestId,
                    bc.sequenceOrder,
                    bc.votesAllowed,
                    bc.selections.map { SelectionFacade(it.selectionId, it.sequenceOrder)}
                )
            }
        }

        override fun contestsForBallotStyle(ballotStyle : String) = contests

        class ContestFacade(
            override val contestId: String,
            override val sequenceOrder: Int,
            override val votesAllowed: Int,
            override val selections: List<ManifestIF.Selection>,
        ) : ManifestIF.Contest

        class SelectionFacade(
            override val selectionId: String,
            override val sequenceOrder: Int) : ManifestIF.Selection

    }
}