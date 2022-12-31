package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.ballot.makeGuardian
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.UInt256
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.toElementModQ
import electionguard.core.toUInt256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeyCeremonyTest {

    @Test
    fun testKeyCeremony() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 2)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 2)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 2)
        val trustees = listOf(trustee1, trustee2, trustee3)

        val result = keyCeremonyExchange(trustees)
        assertTrue(result is Ok, result.getError())
        val kc = result.value
        assertEquals(3, kc.publicKeys.size)
        // note sorting
        val keys: List<ElementModP> = trustees.map {it.electionPublicKey() }
        val expected: List<ElementModP> = kc.publicKeys.map {it.publicKey().key}
        assertEquals(expected, keys)

        trustees.forEach {
            assertEquals(2, it.otherPublicKeys.size)
        }

        // makeElectionInitialized
        val manifestHash: UInt256 = 42U.toUInt256()
        val fakeManifest = makeFakeManifest(manifestHash)
        val config = ElectionConfig(
            group.constants,
            fakeManifest,
            3,
            2,
        )
        val init: ElectionInitialized = kc.makeElectionInitialized(config)

        val strustees = trustees.sortedBy { it.xCoordinate }
        val skeys: List<ElementModP> = strustees.map {it.electionPublicKey() }
        val expectedPublicKey: ElementModP =
            skeys.reduce { a, b -> a * b }

        val commitments: MutableList<ElementModP> = mutableListOf()
        strustees.forEach { commitments.addAll(it.coefficientCommitments()) }
        val expectedExtendedBaseHash: UInt256 = hashElements(init.cryptoBaseHash, init.jointPublicKey, commitments)

        assertEquals(config, init.config)
        assertEquals(expectedPublicKey, init.jointPublicKey)
        assertEquals(manifestHash, init.manifestHash)
        assertNotNull(init.cryptoBaseHash)
        assertEquals(expectedExtendedBaseHash, init.cryptoExtendedBaseHash)
        assertEquals(strustees.map { makeGuardian(it) }, init.guardians)
        assertNotNull(init.metadata["CreatedBy"])

        assertEquals(fakeManifest, init.manifest())
        assertEquals(ElGamalPublicKey(expectedPublicKey), init.jointPublicKey())
        assertEquals(expectedExtendedBaseHash.toElementModQ(group), init.cryptoExtendedBaseHash())
        assertEquals(config.numberOfGuardians, init.numberOfGuardians())
    }

    @Test
    fun testKeyCeremonyFailQuorum() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 2)
        val trustees = listOf(trustee1, trustee2, trustee3)

        val result = keyCeremonyExchange(trustees)
        println("result = ${result.getError()}")
        assertTrue(result is Err, result.getError())
        assertTrue(result.toString().contains("keyCeremonyExchange trustees have different quorums"))
    }

    @Test
    fun testKeyCeremonyFailTrusteeIdDuplicate() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id1", 2, 3)
        val trustees = listOf(trustee1, trustee2, trustee3)

        val result = keyCeremonyExchange(trustees)
        println("result = ${result.getError()}")
        assertTrue(result is Err, result.getError())
        assertTrue(result.toString().contains("keyCeremonyExchange trustees have non-unique ids"))
    }

    @Test
    fun testKeyCeremonyFailTrusteeCoordDuplicate() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 3, 3)
        val trustees = listOf(trustee1, trustee2, trustee3)

        val result = keyCeremonyExchange(trustees)
        println("result = ${result.getError()}")
        assertTrue(result is Err, result.getError())
        assertTrue(result.toString().contains("keyCeremonyExchange trustees have non-unique xcoordinates"))
    }

}

fun makeFakeManifest(cryptoHash: UInt256): Manifest {
    return Manifest(
        "electionScopeId",
        "specVersion",
        Manifest.ElectionType.general,
        "startDate", "endDate",
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null,
        cryptoHash
    )
}
