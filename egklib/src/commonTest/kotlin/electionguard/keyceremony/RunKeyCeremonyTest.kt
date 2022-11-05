package electionguard.keyceremony

import kotlin.test.Test

class RunKeyCeremonyTest {

    @Test
    fun testKeyCeremonyMain() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/start",
                "-trustees",
                "testOut/RunKeyCeremonyTest/private_data/trustees",
                "-out",
                "testOut/RunKeyCeremonyTest",
            )
        )
    }

}

