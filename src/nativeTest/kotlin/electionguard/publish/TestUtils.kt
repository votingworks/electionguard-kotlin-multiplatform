package electionguard.publish

import electionguard.core.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TestUtils {

    // failing
    fun testMakeDirectories() {
        runTest {
            val dir = "testOut/native/runBatchEncryption"
            val ok = createDirectories(dir)
            assertTrue(ok)
            assertTrue(exists(dir))
        }
    }

}