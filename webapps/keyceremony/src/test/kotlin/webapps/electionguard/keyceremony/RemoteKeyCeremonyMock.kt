package webapps.electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.productionGroup
import electionguard.core.toUInt256
import electionguard.keyceremony.EncryptedKeyShare
import electionguard.keyceremony.KeyShare
import electionguard.keyceremony.keyCeremonyExchange

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.spyk
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.text.toByteArray

private val remoteUrl = "http://0.0.0.0:11180"
private val group = productionGroup()

class RemoteKeyCeremonyMock() {

    @Test
    fun testRemoteKeyCeremonyMockOk() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(client, remoteUrl, "id1", 1, 3)
        val trustee2 = RemoteKeyTrusteeProxy(client, remoteUrl, "id2", 2, 3)
        val trustee3 = RemoteKeyTrusteeProxy(client, remoteUrl, "id3", 3, 3)
        val spy3 = spyk(trustee3)

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3))
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Ok)
    }

    @Test
    fun testBadEncryptedShare() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(client, remoteUrl, "id1", 1, 3)
        val trustee2 = RemoteKeyTrusteeProxy(client, remoteUrl, "id2", 2, 3)
        val trustee3 = RemoteKeyTrusteeProxy(client, remoteUrl, "id3", 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), false)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Err)
    }

    @Test
    fun testAllowBadEncryptedShare() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(client, remoteUrl, "id1", 1, 3)
        val trustee2 = RemoteKeyTrusteeProxy(client, remoteUrl, "id2", 2, 3)
        val trustee3 = RemoteKeyTrusteeProxy(client, remoteUrl, "id3", 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), true)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Ok)
    }

    @Test
    fun testBadKeySharesAllowTrue() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(client, remoteUrl, "id1", 1, 3)
        val trustee2 = RemoteKeyTrusteeProxy(client, remoteUrl, "id2", 2, 3)
        val trustee3 = RemoteKeyTrusteeProxy(client, remoteUrl, "id3", 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        every { spy3.keyShareFor(trustee1.id()) } answers {
            // bad KeyShare
            Ok(KeyShare(spy3.id(), trustee1.id(), group.TWO_MOD_Q, group.TWO_MOD_Q))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), true)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Err)
    }

    @Test
    fun testBadKeySharesAllowFalse() {
        val client = HttpClient(Java) {
            install(ContentNegotiation) {
                json()
            }
        }
        val trustee1 = RemoteKeyTrusteeProxy(client, remoteUrl, "id1", 1, 3)
        val trustee2 = RemoteKeyTrusteeProxy(client, remoteUrl, "id2", 2, 3)
        val trustee3 = RemoteKeyTrusteeProxy(client, remoteUrl, "id3", 3, 3)
        val spy3 = spyk(trustee3)
        every { spy3.encryptedKeyShareFor(trustee1.id()) } answers {
            trustee3.encryptedKeyShareFor(trustee1.id()) // trustee needs to cache
            // bad EncryptedShare
            Ok(EncryptedKeyShare(spy3.id(), trustee1.id(), generateHashedCiphertext(group)))
        }
        every { spy3.keyShareFor(trustee1.id()) } answers {
            // bad KeyShare
            Ok(KeyShare(spy3.id(), trustee1.id(), group.TWO_MOD_Q, group.TWO_MOD_Q))
        }

        val exchangeResult = keyCeremonyExchange(listOf(trustee1, trustee2, spy3), false)
        if (exchangeResult is Err) {
            println(exchangeResult.error)
        }
        assertTrue(exchangeResult is Err)
    }
}

fun generateHashedCiphertext(group: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(group.TWO_MOD_P, "what".toByteArray(), group.TWO_MOD_Q.toUInt256(), 42)
}
