package electionguard.keyceremony

import electionguard.core.productionGroup
import electionguard.verifier.runVerifier
import kotlin.test.Test

class RunKeyCeremonyTest {

    @Test
    fun testKeyCeremonyJson() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/startConfigJson",
                "-trustees",
                "testOut/keyceremony/testKeyCeremonyJson/private_data/trustees",
                "-out",
                "testOut/keyceremony/testKeyCeremonyJson",
            )
        )
        runVerifier(productionGroup(), "testOut/keyceremony/testKeyCeremonyJson", 1, true)
    }

    @Test
    fun testKeyCeremonyProto() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/startConfigProto",
                "-trustees",
                "testOut/keyceremony/testKeyCeremonyProto/private_data/trustees",
                "-out",
                "testOut/keyceremony/testKeyCeremonyProto",
            )
        )
        runVerifier(productionGroup(), "testOut/keyceremony/testKeyCeremonyProto", 1, true)
    }

}

