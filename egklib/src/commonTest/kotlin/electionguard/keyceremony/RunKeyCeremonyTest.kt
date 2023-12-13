package electionguard.keyceremony

import electionguard.cli.RunTrustedKeyCeremony
import electionguard.cli.RunVerifier
import electionguard.core.productionGroup
import kotlin.test.Test

class RunKeyCeremonyTest {

    @Test
    fun testKeyCeremonyJson() {
        RunTrustedKeyCeremony.main(
            arrayOf(
                "-in", "src/commonTest/data/startConfigJson",
                "-trustees", "testOut/keyceremony/testKeyCeremonyJson/private_data/trustees",
                "-out", "testOut/keyceremony/testKeyCeremonyJson",
            )
        )
        RunVerifier.runVerifier(productionGroup(), "testOut/keyceremony/testKeyCeremonyJson", 1, true)
    }

}

