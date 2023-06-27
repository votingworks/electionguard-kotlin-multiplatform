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
import electionguard.json2.*
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
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

class TallyDecryptionTest {

    @Test
    fun testTallyDecryptionTestVector() {
        // all trustees present
        val testVector1 = TallyDecryptionTestVector(3, 3, listOf(), "testOut/testvectors/TallyDecryptionTestVector.json")
        testVector1.makeTallyPartialDecryptionTestVector()
        testVector1.readTallyPartialDecryptionTestVector()

        // 3 of 4 trustees present
        val testVector2 = TallyDecryptionTestVector(4, 3, listOf(1), "testOut/testvectors/TallyPartialDecryptionTestVector.json")
        testVector2.makeTallyPartialDecryptionTestVector()
        testVector2.readTallyPartialDecryptionTestVector()
    }
}

class TallyDecryptionTestVector(
    val numberOfGuardians: Int,
    val quorum: Int,
    val missingCoordinates: List<Int>,
    val outputFile: String
) {
    private val jsonFormat = Json { prettyPrint = true }
    val group = productionGroup()
    val nBallots = 11

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
        val trusteesMinus1 = trusteesAll.filter { !missingCoordinates.contains(it.xCoordinate) }

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
            keyCeremonyTrustees.map { it.publishJson( !missingCoordinates.contains(it.xCoordinate) ) },
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

        val keyCeremonyTrustees =  testVector.trustees.map { it.importKeyCeremonyTrustee(group) }
        val trusteesAll = testVector.trustees.map { it.importDecryptingTrustee(group) }
        // leave out one of the trustees to make it a partial decryption
        val trusteesMinus1 = trusteesAll.filter { !missingCoordinates.contains(it.xCoordinate) }
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