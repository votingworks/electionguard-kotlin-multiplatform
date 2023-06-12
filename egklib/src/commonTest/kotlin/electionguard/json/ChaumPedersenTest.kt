package electionguard.json

import electionguard.core.ChaumPedersenProof
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
            val group = productionGroup()
            checkAll(
                iterations = 33,
                elementsModQ(group),
                elementsModQ(group),
            ) { challenge, response ->
                val goodProof = ChaumPedersenProof(challenge, response)
                assertEquals(goodProof, goodProof.publish().import(group))
                assertEquals(goodProof, jsonRoundTrip(goodProof.publish()).import(group))
            }
        }
    }
}