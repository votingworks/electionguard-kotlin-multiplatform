package electionguard

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
            forAll(elementsModP(context), elementsModQ(context)) { p, q ->
                val h1 = context.hashElements(p, q)
                val h2 = context.hashElements(p, q)
                h1 == h2
            }
        }
    }

    @Test
    fun basicHashProperties() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            checkAll(elementsModQ(context), elementsModQ(context)) { q1, q2 ->
                val h1 = context.hashElements(q1)
                val h2 = context.hashElements(q2)
                if (q1 == q2) assertEquals(h1, h2) else assertNotEquals(h1, h2)
            }
        }
    }
}