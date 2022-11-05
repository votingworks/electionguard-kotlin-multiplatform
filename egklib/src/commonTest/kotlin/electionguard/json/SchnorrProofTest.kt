package electionguard.json

import com.github.michaelbull.result.Ok
import electionguard.core.elGamalKeypairs
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.core.schnorrProof
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchnorrProofTest {
    @Test
    fun testRoundtrip() {
        runTest {
            val context = productionGroup()
            checkAll(
                iterations = 33,
                elGamalKeypairs(context),
                elementsModQ(context),
            ) { kp, nonce ->
                val goodProof = kp.schnorrProof(nonce)
                assertTrue(goodProof.validate() is Ok)

                assertEquals(goodProof, context.importSchnorrProof(goodProof.publish()))
                assertEquals(goodProof, context.importSchnorrProof(jsonRoundTrip(goodProof.publish())))
            }
        }
    }
}