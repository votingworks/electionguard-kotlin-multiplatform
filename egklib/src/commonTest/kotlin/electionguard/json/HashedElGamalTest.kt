package electionguard.json

import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.elGamalKeypairs
import electionguard.core.elementsModQ
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.productionGroup
import electionguard.core.propTestFastConfig
import electionguard.core.runTest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class HashedElGamalTest {

    @Test
    fun testRoundtrip() {
        runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.byteArray(Arb.int(min = 1, max = 5).map { it * 32 }, Arb.byte()),
                elGamalKeypairs(group),
                elementsModQ(group, minimum = 2)
            ) { bytes, kp, nonce ->
                val ciphertext = bytes.hashedElGamalEncrypt(kp, nonce)

                assertEquals(ciphertext, ciphertext.publish().import(group))
                assertEquals(ciphertext, jsonRoundTrip(ciphertext.publish()).import(group))
            }
        }
    }
}