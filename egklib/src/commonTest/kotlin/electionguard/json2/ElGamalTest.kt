package electionguard.json2

import electionguard.core.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.*

class ElGamalTest {
    @Test
    fun importExportForElGamal() {
        runTest {
            val group = productionGroup()
            checkAll(
                iterations = 33,
                elGamalKeypairs(group),
                Arb.int(0..100),
                elementsModQNoZero(group)) { kp, v, r ->
                    // first, we'll check that the keys serialize down to basic hex-strings
                    // rather than any fancier structure
                    assertEquals(
                        kp.publicKey.key,
                        jsonRoundTripWithStringPrimitive(kp.publicKey.key.publishJson()).import(group)
                    )

                    assertEquals(
                        kp.secretKey.key,
                        jsonRoundTripWithStringPrimitive(kp.secretKey.key.publishJson()).import(group)
                    )

                    val ciphertext = v.encrypt(keypair = kp, nonce = r)
                    val ciphertextAgain = jsonRoundTrip(ciphertext.publishJson()).import(group)
                    assertEquals(ciphertext, ciphertextAgain)
                }
        }
    }
}