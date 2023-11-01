package electionguard.core

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import io.kotest.assertions.throwables.shouldThrow
import java.lang.RuntimeException
import kotlin.test.*

class Base16Test {

    ////// fromHex
    @Test
    fun emptyFieldTest() {
        val subject = "".fromHex()
        assertNotNull(subject)
        assertEquals(0, subject.size)
    }

    @Test
    fun goodCharsTest() {
        val subject = "ABCDEF012345670".fromHex()
        assertNotNull(subject)
        assertEquals(8, subject.size)
        assertEquals("[10, -68, -34, -16, 18, 52, 86, 112]", subject.contentToString())
    }

    @Test
    fun badCharsTest() {
        val subject = "ABCDEFG12345670".fromHex()
        assertNull(subject)
    }

    @Test
    fun oddCharsTest() {
        val subject = "ABCDEF12345670".fromHex()
        assertNotNull(subject)
        assertEquals(7, subject.size)
        assertEquals("[-85, -51, -17, 18, 52, 86, 112]", subject.contentToString())
    }

    ////// toHex
    @Test
    fun emptyFieldToTest() {
        val subject = ByteArray(0).toHex()
        assertNotNull(subject)
        assertTrue(subject.isEmpty())
    }

    @Test
    fun goodToTest() {
        val subject = byteArrayOf(-85, -51, -17, 18, 52, 86, 112).toHex()
        assertNotNull(subject)
        assertEquals("ABCDEF12345670", subject)
    }

}