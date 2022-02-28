package electionguard.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ConstantsTest {
    @Test
    fun compatibilityChecks() {
        runTest {
            val tinyGroup = tinyGroup()
            val productionGroup1 =
                productionGroup(
                    acceleration = PowRadixOption.NO_ACCELERATION,
                    mode = ProductionMode.Mode4096
                )
            val productionGroup2 =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode4096
                )
            val productionGroup3 =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )

            val tinyDesc = tinyGroup.constants
            val productionDesc1 = productionGroup1.constants
            val productionDesc2 = productionGroup2.constants
            val productionDesc3 = productionGroup3.constants

            shouldNotThrowAny { tinyDesc.requireCompatible(tinyDesc) }
            shouldNotThrowAny { productionDesc1.requireCompatible(productionDesc1) }
            shouldNotThrowAny { productionDesc1.requireCompatible(productionDesc2) }
            shouldThrow<RuntimeException> { tinyDesc.requireCompatible(productionDesc1) }
            shouldThrow<RuntimeException> { productionDesc1.requireCompatible(productionDesc3) }
            shouldThrow<RuntimeException> { productionDesc3.requireCompatible(productionDesc1) }
        }
    }
}