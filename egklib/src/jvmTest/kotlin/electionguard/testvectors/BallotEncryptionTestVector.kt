package electionguard.testvectors

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor
import electionguard.input.BallotInputValidation
import electionguard.cli.ManifestBuilder
import electionguard.input.RandomBallotProvider
import electionguard.json2.*
import electionguard.util.ErrorMessages
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

class BallotEncryptionTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private var outputFile = "testOut/testvectors/BallotEncryptionTestVector.json"

    val group = productionGroup()
    val nBallots = 1

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
        val task: String,
        val expected_proof: RangeProofJson,
        val selections: List<EncryptedSelectionJson>,
    )

    @Serializable
    data class EncryptedSelectionJson(
        val selectionId: String,
        val sequenceOrder: Int,
        val task: String,
        val expected_selection_nonce: ElementModQJson,
        val expected_encrypted_vote: ElGamalCiphertextJson,
        val expected_proof: RangeProofJson,
    )

    fun CiphertextBallot.publishJson(): EncryptedBallotJson {
        val contests = this.contests.map { pcontest ->

            // we rely on deterministic generation of the contest proof nonces, to publish
            val climit = pcontest.proof.proofs.size
            val nonces: Iterable<ElementModQ> = pcontest.selections.map { it.selectionNonce }
            val aggNonce: ElementModQ = with(group) { nonces.addQ() }
            val uc_nonces = Nonces(aggNonce, "range-chaum-pedersen-proof").take(climit + 1)
            val cc_nonces = Nonces(aggNonce, "range-chaum-pedersen-proof-constants").take(climit + 1)

            EncryptedContestJson(
                pcontest.contestId,
                pcontest.sequenceOrder,
                "Compute contest range proof, section 3.3.8",
                pcontest.proof.publishJsonE(uc_nonces, cc_nonces),
                pcontest.selections.map {
                    // we rely on deterministic generation of the proof nonces, to publish
                    val limit = it.proof.proofs.size
                    val u_nonces = Nonces(it.selectionNonce, "range-chaum-pedersen-proof").take(limit + 1)
                    val c_nonces = Nonces(it.selectionNonce, "range-chaum-pedersen-proof-constants").take(limit + 1)

                    EncryptedSelectionJson(
                        it.selectionId,
                        it.sequenceOrder,
                        "Compute selection nonce (eq 25), encrypted vote (eq 24), and associated range proof (Section 3.3.5)",
                        it.selectionNonce.publishJson(),
                        it.ciphertext.publishJson(),
                        it.proof.publishJsonE(u_nonces, c_nonces),
                    )
                })
        }
        return EncryptedBallotJson(this.ballotId, this.ballotNonce.publishJson(), contests)
    }

    @Serializable
    data class BallotEncryptionTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val ballots: List<PlaintextBallotJsonV>,
        val expected_encrypted_ballots: List<EncryptedBallotJson>,
    )

    @Test
    fun testBallotEncryptionTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("BallotEncryptionTestVector.json").toString()
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

        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash, "device")
        val validator = BallotInputValidation(manifest)

        val useBallots = mutableListOf<PlaintextBallot>()
        val eballots = mutableListOf<CiphertextBallot>()
        RandomBallotProvider(manifest, nBallots).ballots().forEach { ballot ->
            val msgs = validator.validate(ballot)
            println(msgs)
            if ( !msgs.hasErrors() ) {
                eballots.add(encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("makeBallotEncryptionTestVector"))!!)
                useBallots.add(ballot)
            }
        }

        val ballotEncryptionTestVector = BallotEncryptionTestVector(
            "Test ballot encryption",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            useBallots.map { it.publishJsonE() },
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

        val publicKey = ElGamalPublicKey(testVector.joint_public_key.import(group) ?: throw IllegalArgumentException("readBallotEncryptionTestVector malformed joint_public_key"))
        val extendedBaseHash = testVector.extended_base_hash.import() ?: throw IllegalArgumentException("readBallotEncryptionTestVector malformed extended_base_hash")
        val ballotsZipped = testVector.ballots.zip(testVector.expected_encrypted_ballots)

        ballotsZipped.forEach { (ballot, eballot) ->
            val manifest = PlaintextBallotJsonManifestFacade(ballot)
            val encryptor = Encryptor(group, manifest, publicKey, extendedBaseHash, "device")
            val ballotNonce = eballot.ballotNonce.import()
            val cyberBallot = encryptor.encrypt(ballot.import(), ByteArray(0), ErrorMessages("readBallotEncryptionTestVector"), ballotNonce)!!
            checkEquals(eballot, cyberBallot)
            checkProofsEquals(publicKey, extendedBaseHash, ballot, eballot, cyberBallot)
        }
    }

    fun checkEquals(expect : EncryptedBallotJson, actual : CiphertextBallot) {
        assertEquals(expect.ballotId, actual.ballotId)
        assertEquals(expect.contests.size, actual.contests.size)

        expect.contests.zip(actual.contests).forEach { (expectContest, actualContest) ->
            assertEquals(expectContest.contestId, actualContest.contestId)
            assertEquals(expectContest.sequenceOrder, actualContest.sequenceOrder)
            // assertEquals(expectContest.expected_proof.import(group), actualContest.proof)

            assertEquals(expectContest.selections.size, actualContest.selections.size)
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.selectionId, actualSelection.selectionId)
                assertEquals(expectSelection.sequenceOrder, actualSelection.sequenceOrder)
                assertEquals(expectSelection.expected_encrypted_vote.import(group), actualSelection.ciphertext)
                assertEquals(expectSelection.expected_selection_nonce.import(group), actualSelection.selectionNonce)
                // assertEquals(expectSelection.expected_proof.import(group), actualSelection.proof)
            }

        }
        println("ballot ${actual.ballotId} is ok")
    }

    // now check the proofs using the passed in nonces, dont depend on deterministic
    fun checkProofsEquals(publicKey: ElGamalPublicKey, extendedBaseHash: UInt256, plain : PlaintextBallotJsonV, expect : EncryptedBallotJson, actual : CiphertextBallot) {

        plain.contests.zip(expect.contests).zip(actual.contests).forEach { (pair: Pair<PlaintextContestJsonV, EncryptedContestJson>, actualContest: CiphertextBallot.Contest) ->
            val plainContest: PlaintextContestJsonV = pair.first
            val expectContest: EncryptedContestJson = pair.second

            val randomUj = expectContest.expected_proof.proofs.map { it.u_nonce.import(group) ?: throw IllegalArgumentException("readBallotEncryptionTestVector malformed u_nonce") }
            val randomCj = expectContest.expected_proof.proofs.map { it.c_nonce.import(group) ?: throw IllegalArgumentException("readBallotEncryptionTestVector malformed c_nonce") }

            val ciphertexts: List<ElGamalCiphertext> = actualContest.selections.map { it.ciphertext }
            val contestAccumulation: ElGamalCiphertext = ciphertexts.encryptedSum()?: 0.encrypt(publicKey)
            val nonces: Iterable<ElementModQ> = actualContest.selections.map { it.selectionNonce }
            val aggNonce: ElementModQ = with(group) { nonces.addQ() }
            val totalVotes: Int = plainContest.selections.map { it.vote }.sum()

            val proofWithNonces: ChaumPedersenRangeProofKnownNonce = contestAccumulation.makeChaumPedersenWithNonces(
                totalVotes,
                aggNonce,
                publicKey,
                extendedBaseHash,
                randomUj,
                randomCj
            )

            assertEquals(expectContest.expected_proof.import(group), proofWithNonces)

            plainContest.selections.zip(expectContest.selections).zip(actualContest.selections).forEach { (pair2, actualSelection) ->
                val plainSelection: PlaintextSelectionJsonV = pair2.first
                val expectSelection: EncryptedSelectionJson = pair2.second

                val randomUjSel = expectSelection.expected_proof.proofs.map { it.u_nonce.import(group) ?: throw IllegalArgumentException("readBallotEncryptionTestVector malformed u_nonce") }
                val randomCjSel = expectSelection.expected_proof.proofs.map { it.c_nonce.import(group) ?: throw IllegalArgumentException("readBallotEncryptionTestVector malformed u_nonce") }

                val proofWithNoncesSel: ChaumPedersenRangeProofKnownNonce = actualSelection.ciphertext.makeChaumPedersenWithNonces(
                    plainSelection.vote,
                    actualSelection.selectionNonce, // encryption nonce ξ for which (α, β) is an encryption of ℓ.
                    publicKey,
                    extendedBaseHash,
                    randomUjSel,
                    randomCjSel
                )

                assertEquals(expectSelection.expected_proof.import(group), proofWithNoncesSel)
            }

        }
        println("ballot ${actual.ballotId} proofs are ok")
    }
}