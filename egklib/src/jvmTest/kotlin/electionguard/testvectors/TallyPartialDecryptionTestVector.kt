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

class TallyPartialDecryptionTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private val outputFile = "testOut/testvectors/TallyPartialDecryptionTestVector.json"

    val group = productionGroup()
    val numberOfGuardians = 4
    val quorum = 3
    val nBallots = 11
    val missingCoordinate = 2

    @Serializable
    data class TrusteeJson(
        val id: String,
        val xCoordinate: Int,
        val polynomial_coefficients: List<ElementModQJson>,
        val keyShare: ElementModQJson,
        val missing: Boolean,
    )

    fun KeyCeremonyTrustee.publishJson(): TrusteeJson {
        return TrusteeJson(
            this.id,
            this.xCoordinate,
            this.polynomial.coefficients.map { it.publishJson() },
            this.keyShare().publishJson(),
            this.xCoordinate == missingCoordinate,
        )
    }

    fun TrusteeJson.import(group: GroupContext): KeyCeremonyTrustee {
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

    fun TrusteeJson.importTrustee(group: GroupContext): DecryptingTrusteeDoerre {
        return DecryptingTrusteeDoerre(this.id,
            this.xCoordinate,
            group.gPowP(this.polynomial_coefficients[0].import(group)),
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
    data class TallyPartialDecryptionTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val trustees : List<TrusteeJson>, // KeyCeremonyTrusteeJson and DecryptingTrustee
        val encrypted_tally : EncryptedTallyJson,
        val expected_decrypted_tally : DecryptedTallyJson,
    )

    @Test
    fun testTallyPartialDecryptionTestVector() {
        makeTallyPartialDecryptionTestVector()
        readTallyPartialDecryptionTestVector()
    }

    fun makeTallyPartialDecryptionTestVector() {
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

        val trusteesAll = keyCeremonyTrustees.map { DecryptingTrusteeDoerre(it.id, it.xCoordinate, it.electionPublicKey(), it.keyShare()) }
        // leave out one of the trustees to make it a partial decryption
        val trusteesMinus1 = trusteesAll.filter { it.xCoordinate != missingCoordinate }

        val guardians = keyCeremonyTrustees.map { Guardian(it.id, it.xCoordinate, it.coefficientProofs()) }
        val guardiansWrapper = Guardians(group, guardians)

        val decryptor = DecryptorDoerre(group,
            extendedBaseHash,
            ElGamalPublicKey(publicKey),
            guardiansWrapper,
            trusteesMinus1,
        )
        val decryptedTally = with(decryptor) { encryptedTally.decrypt() }

        val tallyPartialDecryptionTestVector = TallyPartialDecryptionTestVector(
            "Test tally partial decryption",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            keyCeremonyTrustees.map { it.publishJson() },
            encryptedTally.publishJson(),
            decryptedTally.publishJson(),
        )
        println(jsonFormat.encodeToString(tallyPartialDecryptionTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(tallyPartialDecryptionTestVector, out)
            out.close()
        }
    }

    fun readTallyPartialDecryptionTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: TallyPartialDecryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<TallyPartialDecryptionTestVector>(inp)
            }

        val extendedBaseHash = testVector.extended_base_hash.import()
        val publicKey = ElGamalPublicKey(testVector.joint_public_key.import(group))
        val encryptedTally = testVector.encrypted_tally.import(group)

        val keyCeremonyTrustees =  testVector.trustees.map { it.import(group) }
        val trusteesAll = testVector.trustees.map { it.importTrustee(group) }
        // leave out one of the trustees to make it a partial decryption
        val trusteesMinus1 = trusteesAll.filter { it.xCoordinate != missingCoordinate }
        val guardians = keyCeremonyTrustees.map { Guardian(it.id, it.xCoordinate, it.coefficientProofs()) }
        val guardiansWrapper = Guardians(group, guardians)

        val decryptor = DecryptorDoerre(group,
            extendedBaseHash,
            publicKey,
            guardiansWrapper,
            trusteesMinus1,
        )
        val decryptedTally = with(decryptor) { encryptedTally.decrypt() }

        testVector.expected_decrypted_tally.contests.zip(decryptedTally.contests).forEach { (expectContest, actualContest) ->
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.expected_decrypted_vote, actualSelection.tally)
            }
        }
    }

}