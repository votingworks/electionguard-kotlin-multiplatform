package electionguard.keyceremony

import kotlin.test.Test

class RunKeyCeremonyTest {

    @Test
    fun testKeyCeremonyJson() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/startJson",
                "-trustees",
                "testOut/keyceremony/testKeyCeremonyJson/private_data/trustees",
                "-out",
                "testOut/keyceremony/testKeyCeremonyJson",
            )
        )
    }

    @Test
    fun testKeyCeremonyProto() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/start",
                "-trustees",
                "testOut/keyceremony/testKeyCeremonyProto/private_data/trustees",
                "-out",
                "testOut/keyceremony/testKeyCeremonyProto",
            )
        )
    }

}

