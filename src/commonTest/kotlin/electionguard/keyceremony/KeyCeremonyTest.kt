package electionguard.keyceremony

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.ballot.makeManifest
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
            assertEquals(2, it.otherSharesForMe.size)
            assertEquals(2, it.mySharesForOther.size)
            assertEquals(3, it.guardianPublicKeys.size) // doesnt really need its own
        }

        // makeElectionInitialized
        val manifestHash: UInt256 = 42U.toUInt256()
        val fakeManifest = makeManifest(manifestHash)
        val config = ElectionConfig(
            "protoVersion",
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
        val commitmentsHash = hashElements(commitments)
        val expectedExtendedBaseHash: UInt256 = hashElements(init.cryptoBaseHash, init.jointPublicKey, commitmentsHash)

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
}

fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
    val publicKeys = trustee.sendPublicKeys().unwrap()
    return Guardian(
        trustee.id,
        trustee.xCoordinate,
        publicKeys.coefficientCommitments,
        publicKeys.coefficientProofs,
    )
}