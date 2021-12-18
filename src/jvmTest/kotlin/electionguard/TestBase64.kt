package electionguard

import electionguard.Base64.toBase64
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.positiveLong
import io.kotest.property.checkAll
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

class TestBase64 {
    @Test
    fun comparingBase64ToJava() {
        val encoder = java.util.Base64.getEncoder()
        runBlocking {
            checkAll(Arb.positiveLong()) { x ->
                val bytes = x.toBigInteger().toByteArray()
                val b64lib = bytes.toBase64()
                val j64lib = String(encoder.encode(bytes), StandardCharsets.ISO_8859_1)

                assertEquals(b64lib, j64lib)
            }
        }
    }
}