package electionguard

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.forAll
import kotlin.test.Test

class DLogTest {
    private val small = 2_000

    @Test
    fun basics() {
        runProperty {
            forAll(Arb.int(min=0, max=small)) {
                val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)
                it == context.dLog(context.gPowP(it.toElementModQ(context)))
            }
        }
    }
}
