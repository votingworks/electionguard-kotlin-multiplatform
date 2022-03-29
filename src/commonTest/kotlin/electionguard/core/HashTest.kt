package electionguard.core

import electionguard.core.Base16.fromSafeHex
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HashTest {
    @Test
    fun sameAnswerTwiceInARow() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            forAll(propTestFastConfig, elementsModP(context), elementsModQ(context)) { p, q ->
                val h1 = hashElements(p, q)
                val h2 = hashElements(p, q)
                h1 == h2
            }
        }
    }

    @Test
    fun basicHashProperties() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            checkAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { q1, q2 ->
                val h1 = hashElements(q1)
                val h2 = hashElements(q2)
                if (q1 == q2) assertEquals(h1, h2) else assertNotEquals(h1, h2)
            }
        }
    }

    @Test
    fun basicHmacProperties() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            checkAll(propTestFastConfig, uint256s(), elementsModQ(context), elementsModQ(context))
                { key, q1, q2 ->
                    val hmac = HmacProcessor(key)
                    val h1 = hmac.hmacElements(q1)
                    val h2 = hmac.hmacElements(q2)
                    if (q1 == q2) assertEquals(h1, h2) else assertNotEquals(h1, h2)
                }
        }
    }

    @Test
    fun testAgainstJava() {
        runTest {
            val h1 = hashElements("barchi-hallaren-selection", 0, "barchi-hallaren")
            val expect = "c49a1e8053fba95f6b7cd3f3b30b101cdd595c435a46aecf2872f47f1c601206".fromSafeHex()
                .toUInt256()
            assertEquals(expect, h1)
        }
    }

    @Test
    fun testNonce() {
        runTest {
            val group = productionGroup()
            val contestDescriptionHashQ = "00C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromSafeHex()
                .toUInt256().toElementModQ(group)
            println(" contestDescriptionHashQ = $contestDescriptionHashQ hex = ${contestDescriptionHashQ.cryptoHashString()}")
            val ballotNonce = "13E7A2F4253E6CCE42ED5576CF7B01A06BE07835227E7AFE5F538FB94E9A9B73".fromSafeHex()
                .toUInt256().toElementModQ(group)
            val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
            val nonce0 = nonceSequence[0]
            println(" nonce seed in hex = ${nonceSequence.internalSeed.cryptoHashString()}")
            println(" nonce0 in hex = ${nonce0.cryptoHashString()}")
            val expect = "9AD1E8A7127EFFB627C4A8E65818C846BD3FB854B384910098E85E1F6BAF4D2B".fromSafeHex()
                .toUInt256().toElementModQ(group)
            assertEquals(expect, nonceSequence[0])
        }
    }

    @Test
    fun testHexString() {
        runTest {
            val group = productionGroup()
            show("A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("9A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("0C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("00C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("000C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
            show("0000C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206", group)
        }
    }

    fun show(s1 : String, group : GroupContext) {
        val s1u = s1.fromSafeHex().toUInt256().toString()
        val s1q = s1.fromSafeHex().toUInt256().toElementModQ(group)
        println(" len = ${s1.length} s1u = ${s1u} s1q = ${s1q.cryptoHashString()}")
    }

    @Test
    fun testElementModQ() {
        runTest {
            val group = productionGroup()
            val s1q = "C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromSafeHex()
                .toUInt256().toElementModQ(group)
            val s2q = "000C49A1E8053FBA95F6B7CD3F3B30B101CDD595C435A46AECF2872F47F1C601206".fromSafeHex()
                .toUInt256().toElementModQ(group)
            assertEquals(s1q, s2q)
            assertEquals(hashElements(s1q), hashElements(s2q))
            assertEquals(s1q.cryptoHashString(), s2q.cryptoHashString())
            println("  len = ${s1q.cryptoHashString().length} hex = ${s1q.cryptoHashString()}")
            assertEquals(64, s1q.cryptoHashString().length)
        }
    }

    @Test
    fun testElementModQToHex() {
        runTest {
            val group = productionGroup()
            val subject = group.TWO_MOD_Q
            println(" len = ${subject.cryptoHashString().length} hex = '${subject.cryptoHashString()}'")
            assertEquals(64, subject.cryptoHashString().length)
        }
    }

    @Test
    fun testIterable() {
        runTest {
            val group = productionGroup()
            val h1 = hashElements("hay1", listOf("hey2", "hey3"))
            val h2 = hashElements("hay1", "hey2", "hey3")
            println(" h1 = ${h1}")
            println(" h2 = ${h2}")
            assertNotEquals(h1, h2)

            val expect1 = "FA059A112CDC05E6554073659B6B3D67E7C4678BDFAE7E69D66ECB7AE3344F53".fromSafeHex()
                .toUInt256()
            assertEquals(expect1, h1)

            val expect2 = "E90FF27925C3CCF7A024BAD4B7406A6F2F0A86EB11273CDA35DECE815B5B382F".fromSafeHex()
                .toUInt256()
            assertEquals(expect2, h2)
        }
    }
}