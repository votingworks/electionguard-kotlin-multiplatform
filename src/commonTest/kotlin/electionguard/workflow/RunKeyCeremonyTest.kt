@file:OptIn(ExperimentalCli::class)

package electionguard.workflow

import electionguard.core.productionGroup
import electionguard.keyceremony.runKeyCeremony
import kotlinx.cli.ExperimentalCli
import kotlin.test.Test

class RunKeyCeremonyTest {
    @Test
    fun runFakeKeyCeremonyTrusteeTest() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/FakeKeyCeremonyTrusteeTest"
        val trusteeDir = "testOut/FakeKeyCeremonyTrusteeTest/private_data"

        runKeyCeremony(group, configDir, outputDir, trusteeDir, null)
    }
}

