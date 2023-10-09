package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.*
import io.mockk.every
import io.mockk.spyk
import kotlin.test.Test

import kotlin.test.assertTrue
import kotlin.test.assertEquals

class KeyCeremonyMockTest {

    @Test
    fun testKeyCeremonyOk() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 3, 3)
        val spy3 = spyk(trustee3)
        val trustees = listOf(trustee1, trustee2, spy3)

        val result = keyCeremonyExchange(trustees)
        assertTrue(result is Ok)
    }

    @Test
    fun testKeyCeremonyMockOk() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            val result1 = trustee3.encryptedKeyShareFor(trustee1.id())
            assertTrue(result1 is Ok)
            val ss21 = result1.unwrap()
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), ss21.encryptedCoordinate))
        }
        val trustees = listOf(trustee1, trustee2, spy3)

        val result = keyCeremonyExchange(trustees)
        assertTrue(result is Ok)
    }

    @Test
    fun testAllowBadEncryptedShare() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        val trustees = listOf(trustee1, trustee2, spy3)
        val result = keyCeremonyExchange(trustees, true)
        println("result = $result")
        assertTrue(result is Ok)
    }

    @Test
    fun testBadEncryptedShare() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        val trustees = listOf(trustee1, trustee2, spy3)
        val result = keyCeremonyExchange(trustees, false)
        println("result = $result")
        assertTrue(result is Err)
        assertTrue(result.error.contains("Trustee 'id1' couldnt decrypt EncryptedKeyShare for missingGuardianId 'id3'"))
    }

    @Test
    fun testBadKeySharesAllowFalse() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        every { spy3.keyShareFor(trustee1.id()) } answers {
            // bad KeyShare
            Ok(KeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), group.TWO_MOD_Q))
        }
        val trustees = listOf(trustee1, trustee2, spy3)
        val result = keyCeremonyExchange(trustees, false)
        println("result = $result")
        assertTrue(result is Err)
        println(result)
        assertTrue(result.error.contains("keyCeremonyExchange not complete"))
    }

    @Test
    fun testBadKeySharesAllowTrue() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 1, 3, 3)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 3, 3, 3)
        val trustee3 = KeyCeremonyTrustee(group, "id3", 2, 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        every { spy3.keyShareFor(trustee1.id()) } answers {
            // bad KeyShare
            Ok(KeyShare(spy3.xCoordinate(), spy3.id(), trustee1.id(), group.TWO_MOD_Q))
        }
        val trustees = listOf(trustee1, trustee2, spy3)
        val result = keyCeremonyExchange(trustees, true)
        println("result = $result")
        assertTrue(result is Err)
        println(result)
        assertTrue(result.error.contains("keyCeremonyExchange not complete"))
    }

}
