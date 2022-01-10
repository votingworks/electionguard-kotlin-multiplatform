package electionguard

import kotlin.test.Test

class RandomTest {
    @Test
    fun randomnessIsRandom() {
        // we'll go with 128-bit numbers, so coincidences are unlikely
        runTest {
            val firstBytes = randomBytes(16)

            for (i in 0..100) {
                val moreBytes = randomBytes(16)
                assertContentNotEquals(firstBytes, moreBytes)
            }
        }
    }
}