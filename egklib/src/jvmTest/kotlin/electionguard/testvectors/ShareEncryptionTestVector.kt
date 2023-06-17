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
        val polynomial_coefficients: List<String>,
    )

    @Serializable
    data class GuardianSharesJson(
        val name: String,
        val share_nonces: Map<String, String>,
        val task1: String,
        val expected_shares: List<KeyShareJson>,
        val task2: String,
        val expected_my_share_of_secret: String,
    ) {
        internal fun makeGuardianShares(group : GroupContext) =
            GuardianShares(
                name,
                share_nonces.mapValues { group.base16ToElementModQ(it.value)!! },
                expected_shares.map { it.makePrivateKeyShare(group) },
                group.base16ToElementModQ(expected_my_share_of_secret)!!,
            )
    }

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
        val yCoordinate: String, // ElementModQ, // my polynomial's y value at the other's x coordinate = Pi(ℓ)
        val encryptedCoordinate: HashedElGamalCiphertextJson, // El(Pi_(ℓ))
    ) {
        internal constructor(keyShare : PrivateKeyShare) :
            this(
                keyShare.ownerXcoord,
                keyShare.polynomialOwner,
                keyShare.secretShareFor,
                keyShare.yCoordinate.toHex(),
                HashedElGamalCiphertextJson(keyShare.encryptedCoordinate),
        )

        internal fun makePrivateKeyShare(group : GroupContext) =
            PrivateKeyShare(
                ownerXcoord,
                polynomialOwner,
                secretShareFor,
                encryptedCoordinate.makeHashedElGamalCiphertext(group),
                group.base16ToElementModQ(yCoordinate)!!,
            )
    }

    @Serializable
    data class HashedElGamalCiphertextJson(
        val c0: String, // ElementModP,
        val c1: String, // ByteArray,
        val c2: String, // UInt256,
        val numBytes: Int
    ) {
        constructor(org : HashedElGamalCiphertext) :
            this(org.c0.toHex(), org.c1.toHex(), org.c2.toHex(), org.numBytes)

        internal fun makeHashedElGamalCiphertext(group : GroupContext) =
            HashedElGamalCiphertext(
                group.base16ToElementModP(c0)!!,
                c1.fromHex()!!,
                UInt256(c2.fromHex()!!),
                numBytes,
            )
    }

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
                trustee.polynomial.coefficients.map { it.toHex() },
            )
        }

        val guardianShares = trustees.map { trustee ->
            GuardianSharesJson(
                trustee.id,
                trustee.shareNonces.mapValues { it.value.toHex()},
                "Generate this guardian's shares for other guardians (Pi(ℓ) = yCoordinate, El(Pi(ℓ) = encryptedCoordinate), eq 17",
                trustee.myShareOfOthers.values.map { KeyShareJson(it) },
                "Generate this guardian's share of the secret key, eq 66",
                trustee.keyShare().toHex(),
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
        var fileSystem = FileSystems.getDefault()
        var fileSystemProvider = fileSystem.provider()
        val shareEncryptionTestVector: ShareEncryptionTestVector =
            fileSystemProvider.newInputStream(fileSystem.getPath(outputFile)).use { inp ->
                Json.decodeFromStream<ShareEncryptionTestVector>(inp)
            }

        val guardians = shareEncryptionTestVector.guardians
        val guardianShares = shareEncryptionTestVector.expected_guardian_shares.map { it.makeGuardianShares(group) }
        assertTrue(guardians.size == guardianShares.size)

        val trustees = guardians.zip(guardianShares).map { (guardianJson, share) ->
            val coefficients = guardianJson.polynomial_coefficients.map { group.safeBase16ToElementModQ(it) }
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
        nonce: ElementModQ,
    ) : HashedElGamalCiphertext {
        val nonce: ElementModQ = group.randomElementModQ(minimum = 2)
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
        nonce: ElementModQ,
    ) : HashedElGamalCiphertext {
        val nonce: ElementModQ = shareNonces[other.guardianId]!!
        return super.shareEncryption(Pil, other, nonce)
    }
}