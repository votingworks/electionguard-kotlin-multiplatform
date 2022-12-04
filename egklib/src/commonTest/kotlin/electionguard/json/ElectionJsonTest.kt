package electionguard.json

import electionguard.core.productionGroup
import kotlin.test.Test
import kotlin.test.assertEquals

class ElectionJsonTest {

    @Test
    fun testConstants() {
        val group = productionGroup()
        val constants = group.constants
        val constantsJson = constants.publish()
        val roundtrip = constantsJson.import()
        assertEquals(constants, roundtrip)
        assertEquals(constants, jsonRoundTrip(constants.publish()).import())
        println("${constantsJson}")
    }

}