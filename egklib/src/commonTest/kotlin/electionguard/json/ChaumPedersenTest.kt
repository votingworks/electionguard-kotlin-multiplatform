package electionguard.json

import electionguard.core.GenericChaumPedersenProof
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.runTest
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class ChaumPedersenTest {
    @Test
    fun testRoundtrip() {
        runTest {
            val context = productionGroup()
            checkAll(
                iterations = 33,
                elementsModQ(context),
                elementsModQ(context),
            ) { challenge, response ->
                val goodProof = GenericChaumPedersenProof(challenge, response)
                assertEquals(goodProof, context.importCP(goodProof.publish()))
                assertEquals(goodProof, context.importCP(jsonRoundTrip(goodProof.publish())))
            }
        }
    }
}