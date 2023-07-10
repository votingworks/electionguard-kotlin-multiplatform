package electionguard.testvectors

import com.github.michaelbull.result.Err
import electionguard.ballot.*
import electionguard.core.*
import electionguard.decryptBallot.DecryptWithNonce
import electionguard.json2.*
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.input.ManifestBuilder
import electionguard.input.RandomBallotProvider
import electionguard.tally.AccumulateTally
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream
import java.nio.file.FileSystems
import kotlin.test.Test
import kotlin.test.assertEquals

class DecryptWithNonceTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private val outputFile = "testOut/testvectors/DecryptWithNonceTestVector.json"

    val group = productionGroup()
    val nBallots = 1

    @Serializable
    data class DecryptWithNonceTestVector(
        val desc: String,
        val joint_public_key: ElementModPJson,
        val extended_base_hash: UInt256Json,
        val primary_nonce: UInt256Json,
        val encrypted_ballot: EncryptedBallotJson,
        val task: String,
        val expected_decrypted_ballot : PlaintextBallotJson,
    )

    @Test
    fun DecryptWithNonceTestVector() {
        makeDecryptWithNonceTestVector()
        readDecryptWithNonceTestVector()
    }

    fun makeDecryptWithNonceTestVector() {
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
        val ciphertextBallot = RandomBallotProvider(manifest, nBallots).ballots().map { ballot ->
            encryptor.encrypt(ballot)
        }.first()
        val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

        val decryptionWithPrimaryNonce = DecryptWithNonce(group, ElGamalPublicKey(publicKey), extendedBaseHash)
        val decryptResult = with (decryptionWithPrimaryNonce) { encryptedBallot.decrypt(ciphertextBallot.ballotNonce) }
        if (decryptResult is Err) {
            throw RuntimeException("encrypted ballot fails decryption = $decryptResult" )
        }
        val decryptedBallot: PlaintextBallot = decryptResult.component1()!!

        val decryptWithNonceTestVector = DecryptWithNonceTestVector(
            "Test decrypt ballot with nonce",
            publicKey.publishJson(),
            extendedBaseHash.publishJson(),
            ciphertextBallot.ballotNonce.publishJson(),
            encryptedBallot.publishJson(),
            "Decrypt ballot with given primary nonce",
            decryptedBallot.publishJson()
        )
        println(jsonFormat.encodeToString(decryptWithNonceTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(decryptWithNonceTestVector, out)
            out.close()
        }
    }

    fun readDecryptWithNonceTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val testVector: DecryptWithNonceTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<DecryptWithNonceTestVector>(inp)
            }

        val extendedBaseHash = testVector.extended_base_hash.import()
        val publicKey = ElGamalPublicKey(testVector.joint_public_key.import(group))
        val encryptedBallot: EncryptedBallot = testVector.encrypted_ballot.import(group)

        val decryptionWithPrimaryNonce = DecryptWithNonce(group, publicKey, extendedBaseHash)
        val decryptResult = with (decryptionWithPrimaryNonce) { encryptedBallot.decrypt(testVector.primary_nonce.import()) }
        if (decryptResult is Err) {
            throw RuntimeException("encrypted ballot fails decryption = $decryptResult" )
        }
        val decryptedBallot: PlaintextBallot = decryptResult.component1()!!

        testVector.expected_decrypted_ballot.contests.zip(decryptedBallot.contests).forEach { (expectContest, actualContest) ->
            expectContest.selections.zip(actualContest.selections).forEach { (expectSelection, actualSelection) ->
                assertEquals(expectSelection.vote, actualSelection.vote)
            }
        }
    }

}