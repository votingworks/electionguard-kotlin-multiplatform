package electionguard.testvectors

import electionguard.ballot.*
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.encrypt.Encryptor
import electionguard.encrypt.cast
import electionguard.input.ManifestBuilder
import electionguard.input.RandomBallotProvider
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.keyceremony.regeneratePolynomial
import electionguard.tally.AccumulateTally
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.FileOutputStream
import java.nio.file.FileSystems
import kotlin.test.Test

class TallyDecryptionTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private val outputFile = "testOut/testvectors/TallyDecryptionTestVector.json"

    val group = productionGroup()
    val numberOfGuardians = 3
    val quorum = 3
    val nBallots = 11

    @Serializable
    data class KeyCeremonyTrusteeJson(
        val id: String,
        val xCoordinate: Int,
        val polynomial_coefficients: List<ElementModQJson>,
        val proofs: List<SchnorrProofJson>,
        val keyShare: ElementModQJson,
    )

    //     val group: GroupContext,
    //    val id: String,
    //    val xCoordinate: Int,
    //    val quorum: Int,
    //    val polynomial : ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum)
    fun KeyCeremonyTrustee.publishJson(): KeyCeremonyTrusteeJson {
        return KeyCeremonyTrusteeJson(
            this.id,
            this.xCoordinate,
            this.polynomial.coefficients.map { it.publishJson() },
            this.coefficientProofs().map { it.publishJson() },
            this.keyShare().publishJson(),
            )
    }

    fun KeyCeremonyTrusteeJson.import(group: GroupContext): KeyCeremonyTrustee {
        return KeyCeremonyTrustee(
            group,
            this.id,
            this.xCoordinate,
            quorum,
            group.regeneratePolynomial(
                this.id,
                this.xCoordinate,
                this.polynomial_coefficients.map { it.import(group) },
            )
        )
    }

    fun KeyCeremonyTrusteeJson.importTrustee(group: GroupContext): DecryptingTrusteeDoerre {
        return DecryptingTrusteeDoerre(this.id,
            this.xCoordinate,
            this.proofs[0].public_key.import(group),
            this.keyShare.import(group))
    }


    @Serializable
    data class EncryptedTallyJson(
        val contests: List<EncryptedTallyContestJson>,
    )

    @Serializable
    data class EncryptedTallyContestJson(
        val contestId: String,
        val sequenceOrder: Int,
        val selections: List<EncryptedTallySelectionJson>,
    )

    @Serializable
    data class EncryptedTallySelectionJson(
        val selectionId: String,
        val sequenceOrder: Int,
        val encrypted_vote: ElGamalCiphertextJson,
    )

    fun EncryptedTally.publishJson(): EncryptedTallyJson {
        val contests = this.contests.map { pcontest ->

            EncryptedTallyContestJson(
                pcontest.contestId,
                pcontest.sequenceOrder,
                pcontest.selections.map {
                    EncryptedTallySelectionJson(
                        it.selectionId,
                        it.sequenceOrder,
                        it.ciphertext.publishJson(),
                    )
                })
        }
        return EncryptedTallyJson(contests)
    }

    fun EncryptedTallyJson.import(group: GroupContext): EncryptedTally {
        val contests = this.contests.map { pcontest ->

            EncryptedTally.Contest(
                pcontest.contestId,
                pcontest.sequenceOrder,
                pcontest.selections.map {
                    EncryptedTally.Selection(
                        it.selectionId,
                        it.sequenceOrder,
                        it.encrypted_vote.import(group),
                    )
                })
        }
        return EncryptedTally("tallyId", contests)
    }

    @Serializable
    data class DecryptedTallyJson(
        val contests: List<DecryptedTallyContestJson>,
    )

    @Serializable
    data class DecryptedTallyContestJson(
        val contestId: String,
        val selections: List<DecryptedTallySelectionJson>,
    )

    @Serializable
    data class DecryptedTallySelectionJson(
        val selectionId: String,
        val task: String,
        val expected_decrypted_vote: Int,
    )

    fun DecryptedTallyOrBallot.publishJson(): DecryptedTallyJson {
        val contests = this.contests.map { pcontest ->
            DecryptedTallyContestJson(
                pcontest.contestId,
                pcontest.selections.map {
                    DecryptedTallySelectionJson(
                        it.selectionId,
                        "Decrypt tally for this selection, eq 65",
                        it.tally,
                    )
                })
        }
        return DecryptedTallyJson(contests)
    }


    @Serializable
    data class TallyDecryptionTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val trustees : List<KeyCeremonyTrusteeJson>, // KeyCeremonyTrusteeJson and DecryptingTrustee
        val encrypted_tally : EncryptedTallyJson,
        val expected_decrypted_tally : DecryptedTallyJson,
    )

    @Test
    fun testTallyDecryptionTestVector() {
        makeTallyDecryptionTestVector()
        readTallyDecryptionTestVector()
    }

    fun makeTallyDecryptionTestVector() {
        // run the whole workflow
        val keyCeremonyTrustees: List<KeyCeremonyTrustee> = List(numberOfGuardians) {
            val seq = it + 1
            KeyCeremonyTrustee(group, "guardian$seq", seq, quorum)
        }.sortedBy { it.xCoordinate }

        keyCeremonyExchange(keyCeremonyTrustees)

        val publicKeys = mutableListOf<ElementModP>()
        val allCommitments = mutableListOf<ElementModP>()
        keyCeremonyTrustees.forEach { trustee ->
            publicKeys.add(trustee.electionPublicKey())
            allCommitments.addAll(trustee.coefficientCommitments())
        }

        val electionBaseHash = UInt256.random()
        val publicKey = publicKeys.reduce { a, b -> a * b }
        val extendedBaseHash = hashFunction(electionBaseHash.bytes, 0x12.toByte(), publicKey, allCommitments)

        val ebuilder = ManifestBuilder("makeBallotEncryptionTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash)
        val eballots = RandomBallotProvider(manifest, nBallots).ballots().map { ballot ->
            encryptor.encrypt(ballot)
        }

        val accumulator = AccumulateTally(group, manifest, "makeBallotAggregationTestVector")
        eballots.forEach { eballot ->
            accumulator.addCastBallot(eballot.cast())
        }
        val encryptedTally = accumulator.build()

        val trustees = keyCeremonyTrustees.map { DecryptingTrusteeDoerre(it.id, it.xCoordinate, it.electionPublicKey(), it.keyShare()) }
        val guardians = keyCeremonyTrustees.map { Guardian(it.id, it.xCoordinate, it.coefficientProofs()) }
        val guardiansWrapper = Guardians(group, guardians)

        val decryptor = DecryptorDoerre(group,
            extendedBaseHash,
            ElGamalPublicKey(publicKey),
            guardiansWrapper,
            trustees,
        )
        val decryptedTally = with(decryptor) { encryptedTally.decrypt() }

        val tallyDecryptionTestVector = TallyDecryptionTestVector(
            "Test tally decryption",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            keyCeremonyTrustees.map { it.publishJson() },
            encryptedTally.publishJson(),
            decryptedTally.publishJson(),
        )
        println(jsonFormat.encodeToString(tallyDecryptionTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(tallyDecryptionTestVector, out)
            out.close()
        }
    }

    fun readTallyDecryptionTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: TallyDecryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<TallyDecryptionTestVector>(inp)
            }

        val extendedBaseHash = testVector.extended_base_hash.import()
        val publicKey = ElGamalPublicKey(testVector.joint_public_key.import(group))
        val encryptedTally = testVector.encrypted_tally.import(group)

        val keyCeremonyTrustees =  testVector.trustees.map { it.import(group) }
        val trustees = testVector.trustees.map { it.importTrustee(group) }
        val guardians = keyCeremonyTrustees.map { Guardian(it.id, it.xCoordinate, it.coefficientProofs()) }
        val guardiansWrapper = Guardians(group, guardians)

        val decryptor = DecryptorDoerre(group,
            extendedBaseHash,
            publicKey,
            guardiansWrapper,
            trustees,
        )
        val decryptedTally = with(decryptor) { encryptedTally.decrypt() }

        testVector.expected_decrypted_tally.contests.zip(decryptedTally.contests).forEach { (expectContest, actualContest) ->
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.expected_decrypted_vote, actualSelection.tally)
            }
        }
    }

}