package electionguard.core

import java.util.stream.IntStream
import kotlin.streams.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DLogParallelTest {
    private val big = 20_000

    @Test
    fun streamParallelism() {
        // We'll use Java Streams as a cheap and easy way to generate concurrency
        // for stressing out the reentrancy of dLog().
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val input = IntStream.range(big + 10, big + 100).toList().shuffled()
            val results =
                input.parallelStream()
                    .map {
                        val q = it.toElementModQ(context)
                        context.gPowP(q).dLogG() ?: fail("Unexpected failure for $it")
                    }
                    .toList()

            assertEquals(input, results)
        }
    }
}