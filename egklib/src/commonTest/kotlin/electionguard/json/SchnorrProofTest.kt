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
            val group = productionGroup()
            checkAll(
                iterations = 33,
                elGamalKeypairs(group),
                elementsModQ(group),
            ) { kp, nonce ->
                val goodProof = kp.schnorrProof(nonce)
                assertTrue(goodProof.validate() is Ok)

                assertEquals(goodProof, goodProof.publish().import(group))
                assertEquals(goodProof, jsonRoundTrip(goodProof.publish()).import(group))
            }
        }
    }
}