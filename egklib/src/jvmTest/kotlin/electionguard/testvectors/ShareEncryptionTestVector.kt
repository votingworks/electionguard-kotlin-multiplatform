package electionguard.testvectors

import electionguard.core.*
import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.keyceremony.*
import electionguard.keyceremony.PrivateKeyShare
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.FileOutputStream
import java.nio.file.FileSystems
import kotlin.io.use
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareEncryptionTestVector {
    private val jsonFormat = Json { prettyPrint = true }
    private val outputFile = "testOut/testvectors/ShareEncryptionTestVector.json"

    val numberOfGuardians = 3
    val quorum = 3
    val group = productionGroup()

    @Serializable
    data class GuardianJson(
        val name: String,
        val coordinate: Int,
        val polynomial_coefficients: List<ElementModQJson>,
    )

    @Serializable
    data class GuardianSharesJson(
        val name: String,
        val share_nonces: Map<String, ElementModQJson>,
        val task1: String,
        val expected_shares: List<KeyShareJson>,
        val task2: String,
        val expected_my_share_of_secret: ElementModQJson,
    )

    internal fun GuardianSharesJson.import(group : GroupContext) =
            GuardianShares(
                name,
                share_nonces.mapValues { it.value.import(group) },
                expected_shares.map { it.import(group) },
                expected_my_share_of_secret.import(group),
            )

    internal data class GuardianShares(
        val name: String,
        val shareNonces: Map<String, ElementModQ>,
        val expected_shares: List<PrivateKeyShare>,
        val expected_my_share: ElementModQ,
    )

    @Serializable
    data class KeyShareJson(
        val ownerXcoord: Int, // guardian i xCoordinate
        val polynomialOwner: String, // guardian i name
        val secretShareFor: String, // guardian l name
        val yCoordinate: ElementModQJson, // ElementModQ, // my polynomial's y value at the other's x coordinate = Pi(ℓ)
        val encryptedCoordinate: HashedElGamalCiphertextJson, // El(Pi_(ℓ))
    )

    internal fun PrivateKeyShare.publishJson() =
        KeyShareJson(
            this.ownerXcoord,
            this.polynomialOwner,
            this.secretShareFor,
            this.yCoordinate.publishJson(),
            this.encryptedCoordinate.publishJson(),
    )

    internal fun KeyShareJson.import(group : GroupContext) =
        PrivateKeyShare(
            ownerXcoord,
            polynomialOwner,
            secretShareFor,
            encryptedCoordinate.import(group),
            yCoordinate.import(group),
        )

    @Serializable
    data class HashedElGamalCiphertextJson(
        val c0: ElementModPJson, // ElementModP,
        val c1: String, // ByteArray,
        val c2: UInt256Json, // UInt256,
        val numBytes: Int
    )

    fun HashedElGamalCiphertext.publishJson() =
            HashedElGamalCiphertextJson(this.c0.publishJson(), this.c1.toHex(), this.c2.publishJson(), this.numBytes)

    fun HashedElGamalCiphertextJson.import(group : GroupContext) =
        HashedElGamalCiphertext(
            c0.import(group),
            c1.fromHex()!!,
            c2.import(),
            numBytes,
        )

    @Serializable
    data class ShareEncryptionTestVector(
        val desc : String,
        val guardians : List<GuardianJson>,
        val expected_guardian_shares : List<GuardianSharesJson>,
    )

    @Test
    fun testShareEncryptionTestVector() {
        makeShareEncryptionTestVector()
        readShareEncryptionTestVector()
    }

    fun makeShareEncryptionTestVector() {

        val trustees: List<KeyCeremonyTrusteeSaveNonces> = List(numberOfGuardians) {
            val seq = it + 1
            KeyCeremonyTrusteeSaveNonces(group, "guardian$seq", seq, quorum)
        }.sortedBy { it.xCoordinate }

        keyCeremonyExchange(trustees)

        val guardians = trustees.map { trustee ->
            GuardianJson(
                trustee.id,
                trustee.xCoordinate,
                trustee.polynomial.coefficients.map { it.publishJson() },
            )
        }

        val guardianShares = trustees.map { trustee ->
            GuardianSharesJson(
                trustee.id,
                trustee.shareNonces.mapValues { it.value.publishJson()},
                "Generate this guardian's shares for other guardians (Pi(ℓ) = yCoordinate, El(Pi(ℓ) = encryptedCoordinate), eq 17",
                trustee.myShareOfOthers.values.map { it.publishJson() },
                "Generate this guardian's share of the secret key, eq 66",
                trustee.keyShare().publishJson(),
            )
        }

        val shareEncryptionTestVector = ShareEncryptionTestVector(
            "Test partial key (aka share) exchange during the KeyCeremony",
            guardians,
            guardianShares,
        )
        println(jsonFormat.encodeToString(shareEncryptionTestVector))

        FileOutputStream(outputFile).use { out ->
            jsonFormat.encodeToStream(shareEncryptionTestVector, out)
            out.close()
        }
    }

    fun readShareEncryptionTestVector() {
        val fileSystem = FileSystems.getDefault()
        val fileSystemProvider = fileSystem.provider()
        val shareEncryptionTestVector: ShareEncryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<ShareEncryptionTestVector>(inp)
            }

        val guardians = shareEncryptionTestVector.guardians
        val guardianShares = shareEncryptionTestVector.expected_guardian_shares.map { it.import(group) }
        assertTrue(guardians.size == guardianShares.size)

        val trustees = guardians.zip(guardianShares).map { (guardianJson, share) ->
            val coefficients = guardianJson.polynomial_coefficients.map { it.import(group) }
            val polynomial = group.regeneratePolynomial(
                guardianJson.name,
                guardianJson.coordinate,
                coefficients,
            )

            KeyCeremonyTrusteeWithNonces(
                group,
                guardianJson.name,
                guardianJson.coordinate,
                quorum,
                polynomial,
                share.shareNonces
            )
        }

        keyCeremonyExchange(trustees)

        guardianShares.forEach { guardianShare ->
            guardianShare.expected_shares.forEach { expectedShare ->
                val actualShare = expectedShare.findMatchingShare(trustees)
                assertEquals(expectedShare.yCoordinate, actualShare.yCoordinate)
                assertEquals(expectedShare.encryptedCoordinate, actualShare.encryptedCoordinate)
            }
        }

        trustees.zip(guardianShares).map { (trustee, share) ->
            assertEquals(share.expected_my_share, trustee.keyShare())
        }
    }

    internal fun PrivateKeyShare.findMatchingShare(trustees : List<KeyCeremonyTrustee>) : PrivateKeyShare {
        val trustee = trustees.find { it.id == this.secretShareFor }!!
        return trustee.myShareOfOthers[this.polynomialOwner]!!
    }
}

private class KeyCeremonyTrusteeSaveNonces(
    group: GroupContext,
    id: String,
    xCoordinate: Int,
    quorum: Int,
    polynomial : ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum)
) : KeyCeremonyTrustee(group, id, xCoordinate, quorum, polynomial) {
    val shareNonces = mutableMapOf<String, ElementModQ>()

    override fun shareEncryption(
        Pil: ElementModQ,
        other: PublicKeys,
        nonce: ElementModQ // An overriding function is not allowed to specify default values for its parameters
    ) : HashedElGamalCiphertext {
        // val nonce: ElementModQ = group.randomElementModQ(minimum = 2)
        shareNonces[other.guardianId] = nonce
        return super.shareEncryption(Pil, other, nonce)
    }
}

private class KeyCeremonyTrusteeWithNonces(
    group: GroupContext,
    id: String,
    xCoordinate: Int,
    quorum: Int,
    polynomial : ElectionPolynomial = group.generatePolynomial(id, xCoordinate, quorum),
    val shareNonces: Map<String, ElementModQ>,
) : KeyCeremonyTrustee(group, id, xCoordinate, quorum, polynomial) {

    override fun shareEncryption(
        Pil: ElementModQ,
        other: PublicKeys,
        nonce: ElementModQ, // ignored
    ) : HashedElGamalCiphertext {
        val useNonce: ElementModQ = shareNonces[other.guardianId]!!
        return super.shareEncryption(Pil, other, useNonce)
    }
}