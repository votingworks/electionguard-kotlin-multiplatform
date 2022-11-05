package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.productionGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeyCeremonyTrusteeTest {

    @Test
    fun testKeyCeremonyTrustee() {
        val group = productionGroup()
        val trustee = KeyCeremonyTrustee(group, "id", 42, 4)
        assertEquals("id", trustee.id())
        assertEquals(42, trustee.xCoordinate())
        assertNotNull(trustee.electionPublicKey())
        assertNotNull(trustee.electionPrivateKey())
        assertEquals(4, trustee.coefficientCommitments().size)
        assertEquals(trustee.electionPublicKey(), trustee.coefficientCommitments()[0])
        val result = trustee.sendPublicKeys()
        assertTrue(result is Ok)
        val keys = result.unwrap()
        assertEquals(trustee.id(), keys.guardianId)
        assertEquals(trustee.xCoordinate(), keys.guardianXCoordinate)
        assertEquals(trustee.coefficientCommitments(), keys.coefficientCommitments())
        assertEquals(4, keys.coefficientProofs.size)
    }

    @Test
    fun testKeyCeremonyTrusteeShares() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 4)

        val result1 = trustee1.receivePublicKeys(trustee2.sendPublicKeys().unwrap())
        assertTrue(result1 is Ok)

        /** Create trustee1 SecretKeyShare for trustee2. */
        val result2 = trustee1.sendSecretKeyShare(trustee2.id())
        assertTrue(result2 is Ok)
        val ss2 = result2.unwrap()
        assertEquals(trustee1.id(), ss2.generatingGuardianId)
        assertEquals(trustee2.id(), ss2.designatedGuardianId)
        assertEquals(trustee2.xCoordinate(), ss2.designatedGuardianXCoordinate)

        val result3 = trustee2.receiveSecretKeyShare(ss2)
        assertTrue(result3 is Err)
        assertEquals("Trustee id2 does not have public keys for  id1", result3.error)

        trustee2.receivePublicKeys(trustee1.sendPublicKeys().unwrap())
        val result4 = trustee2.receiveSecretKeyShare(ss2)
        assertTrue(result4 is Ok)
    }
}