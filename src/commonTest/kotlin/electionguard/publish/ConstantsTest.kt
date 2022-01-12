package electionguard.publish

import electionguard.core.*
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class ConstantsTest {
    @Test
    fun compatibilityChecks() {
        runTest {
            val tinyGroup = tinyGroup()
            val productionGroup1 = productionGroup(PowRadixOption.NO_ACCELERATION)
            val productionGroup2 = productionGroup(PowRadixOption.LOW_MEMORY_USE)

            val tinyDesc = tinyGroup.constants
            val productionDesc1 = productionGroup1.constants
            val productionDesc2 = productionGroup2.constants

            assertDoesNotThrow { tinyDesc.requireCompatible(Json.encodeToJsonElement(tinyDesc)) }
            assertDoesNotThrow {
                productionDesc1.requireCompatible(Json.encodeToJsonElement(productionDesc2))
            }
            assertThrows<RuntimeException> {
                tinyDesc.requireCompatible(Json.encodeToJsonElement(productionDesc1))
            }
        }
    }
}