@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.testvectors

import electionguard.ballot.*
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.json2.*
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.cli.ManifestBuilder
import electionguard.input.RandomBallotProvider
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.util.ErrorMessages
import kotlinx.serialization.ExperimentalSerializationApi
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

class DecryptBallotTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private var outputFile = "testOut/testvectors/DecryptBallotTestVector.json"

    val group = productionGroup()
    val nBallots = 1
    val numberOfGuardians = 4
    val quorum = 4

    @Serializable
    data class DecryptBallotTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val trustees : List<TrusteeJson>, // KeyCeremonyTrusteeJson and DecryptingTrustee
        val encrypted_ballot: EncryptedBallotJson,
        val task: String,
        val expected_decrypted_ballot : DecryptedTallyOrBallotJson,
    )

    @Test
    fun testDecryptBallotTestVector(@TempDir tempDir : Path) {
        outputFile = tempDir.resolve("DecryptBallotTestVector.json").toString()
        makeDecryptBallotTestVector()
        readDecryptBallotTestVector()
    }

    fun makeDecryptBallotTestVector() {
        // run the whole workflow
        val keyCeremonyTrustees: List<KeyCeremonyTrustee> = List(numberOfGuardians) {
            val seq = it + 1
            KeyCeremonyTrustee(group, "guardian$seq", seq, numberOfGuardians, quorum)
        }.sortedBy { it.xCoordinate }

        keyCeremonyExchange(keyCeremonyTrustees)

        val publicKeys = mutableListOf<ElementModP>()
        keyCeremonyTrustees.forEach { trustee ->
            publicKeys.add(trustee.guardianPublicKey())
        }

        val electionBaseHash = UInt256.random()
        val publicKey = publicKeys.reduce { a, b -> a * b }
        val extendedBaseHash = electionExtendedHash(electionBaseHash, publicKey)

        val ebuilder = ManifestBuilder("makeDecryptBallotTestVector")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        val encryptor = Encryptor(group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash, "device")
        val ciphertextBallot = RandomBallotProvider(manifest, nBallots).ballots().map { ballot ->
            encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("makeDecryptBallotTestVector"))!!
        }.first()
        val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

        val trusteesAll = keyCeremonyTrustees.map {
            DecryptingTrusteeDoerre(it.id, it.xCoordinate, it.guardianPublicKey(), it.computeSecretKeyShare())
        }

        val guardians = keyCeremonyTrustees.map { Guardian(it.id, it.xCoordinate, it.coefficientProofs()) }
        val guardiansWrapper = Guardians(group, guardians)

        val decryptor = DecryptorDoerre(group,
            extendedBaseHash,
            ElGamalPublicKey(publicKey),
            guardiansWrapper,
            trusteesAll,
        )
        val decryptedBallot = decryptor.decryptBallot(encryptedBallot, ErrorMessages(""))!!

        val decryptBallotTestVector = DecryptBallotTestVector(
            "Test decrypt ballot with trustees",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            keyCeremonyTrustees.map { it.publishJsonE(false) },
            encryptedBallot.publishJson(),
            "Decrypt ballot with trustees",
            decryptedBallot.publishJson()
        )
        println(jsonFormat.encodeToString(decryptBallotTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(decryptBallotTestVector, out)
            out.close()
        }
    }

    fun readDecryptBallotTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: DecryptBallotTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<DecryptBallotTestVector>(inp)
            }

        val extendedBaseHash = testVector.extended_base_hash.import() ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed extended_base_hash")
        val publicKey = ElGamalPublicKey(testVector.joint_public_key.import(group) ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed joint_public_key"))
        val encryptedBallot = testVector.encrypted_ballot.import(group, errs = ErrorMessages("readDecryptBallotTestVector"))

        val keyCeremonyTrustees =  testVector.trustees.map { it.importKeyCeremonyTrustee(group, numberOfGuardians) }
        val trusteesAll = testVector.trustees.map { it.importDecryptingTrustee(group) }
        val guardians = keyCeremonyTrustees.map { Guardian(it.id, it.xCoordinate, it.coefficientProofs()) }
        val guardiansWrapper = Guardians(group, guardians)

        val decryptor = DecryptorDoerre(group,
            extendedBaseHash,
            publicKey,
            guardiansWrapper,
            trusteesAll,
        )
        val decryptedBallot = decryptor.decryptBallot(encryptedBallot!!, ErrorMessages(""))!!

        // to compare the proofs, we need the nonces. so just validate them.
        testVector.expected_decrypted_ballot.contests.zip(decryptedBallot.contests).forEach { (expectContest, actualContest) ->
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.tally, actualSelection.tally)
                assertEquals(expectSelection.b_over_m.import(group), actualSelection.bOverM)
                assertEquals(expectSelection.encrypted_vote.import(group), actualSelection.encryptedVote)
                assertTrue(actualSelection.proof.verifyDecryption(extendedBaseHash, publicKey.key, actualSelection.encryptedVote, actualSelection.bOverM))
                assertTrue(actualSelection.proof.verifyDecryption(
                    extendedBaseHash,
                    publicKey.key,
                    expectSelection.encrypted_vote.import(group) ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed encrypted_vote"),
                    expectSelection.b_over_m.import(group) ?: throw IllegalArgumentException("readDecryptBallotTestVector malformed b_over_m"))
                )
            }

            val expectedDecryptedContestData = expectContest.decrypted_contest_data!!.import(group, ErrorMessages("readDecryptBallotTestVector"))!!
            val actualDecryptedContestData = actualContest.decryptedContestData!!
            assertEquals(expectedDecryptedContestData.contestData, actualDecryptedContestData.contestData)
            assertTrue(expectedDecryptedContestData.proof.verifyContestDataDecryption(publicKey.key, extendedBaseHash, expectedDecryptedContestData.beta, expectedDecryptedContestData.encryptedContestData))
        }
    }

}