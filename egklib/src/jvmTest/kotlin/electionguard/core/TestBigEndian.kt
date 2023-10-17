package electionguard.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestBigEndian {

    @Test
    fun testBigEndian() {
        val result : ByteArray = intToByteArray(1)
        if (isBigEndian()) {
            repeat(4) { assertEquals(if (it == 0) 1 else 0, result[it]) }
        } else {
            repeat(4) { assertEquals(if (it == 3) 1 else 0, result[it]) }
        }
    }

}