package electionguard.json

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
            val group = productionGroup()
            checkAll(
                iterations = 33,
                elGamalKeypairs(group),
                Arb.int(0..100),
                elementsModQNoZero(group)) { kp, v, r ->
                    // first, we'll check that the keys serialize down to basic hex-strings
                    // rather than any fancier structure
                    assertEquals(
                        kp.publicKey,
                        jsonRoundTripWithStringPrimitive(kp.publicKey.publish()).import(group)
                    )

                    assertEquals(
                        kp.secretKey,
                        jsonRoundTripWithStringPrimitive(kp.secretKey.publish()).import(group)
                    )

                    // then, we'll check that the broader structure also does a successful
                    // roundtrip from JSON and back again
                    val kpAgain = jsonRoundTrip(kp.publish()).import(group)
                    assertEquals(kp, kpAgain)

                    val ciphertext = v.encrypt(keypair = kp, nonce = r)
                    val ciphertextAgain = jsonRoundTrip(ciphertext.publish()).import(group)
                    assertEquals(ciphertext, ciphertextAgain)
                }
        }
    }
}