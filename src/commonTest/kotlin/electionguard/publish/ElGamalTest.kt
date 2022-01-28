package electionguard.publish

import electionguard.core.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.*
import kotlinx.serialization.json.*

inline fun <reified T> jsonRoundTrip(value: T): T {
    val jsonT: JsonElement = Json.encodeToJsonElement(value)

    val jsonS = jsonT.toString()
    val backToJ: JsonElement = Json.parseToJsonElement(jsonS)
    val backToT: T = Json.decodeFromJsonElement(backToJ)
    return backToT
}

class ElGamalTest {
    @Test
    fun importExportForElGamal() {
        runTest {
            val context = tinyGroup()
            checkAll(elGamalKeypairs(context), Arb.int(0..100), elementsModQNoZero(context))
                { kp, v, r ->
                    // first, we'll check that the keys serialize down to basic hex-strings
                    // rather than any fancier structure
                    assertEquals(
                        kp.publicKey,
                        context.import(jsonRoundTripWithStringPrimitive(kp.publicKey.publish()))
                    )

                    assertEquals(
                        kp.secretKey,
                        context.import(jsonRoundTripWithStringPrimitive(kp.secretKey.publish()))
                    )

                    // then, we'll check that the broader structure also does a successful
                    // roundtrip from JSON and back again
                    val kpAgain = context.import(jsonRoundTrip(kp.publish()))
                    assertEquals(kp, kpAgain)

                    val ciphertext = v.encrypt(keypair = kp, nonce = r)
                    val ciphertextAgain = context.import(jsonRoundTrip(ciphertext.publish()))
                    assertEquals(ciphertext, ciphertextAgain)
                }
        }
    }
}