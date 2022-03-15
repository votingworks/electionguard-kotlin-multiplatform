package electionguard.core

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
}