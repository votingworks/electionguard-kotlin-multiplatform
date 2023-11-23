package electionguard.cli

import kotlin.test.Test

class RunMixnetPepTest {

    @Test
    fun testRunMixnetBlindTrustPep() {
        RunMixnetPep.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/eg",
                "--mixnetFile", "src/commonTest/data/rave/vf/after-mix-2-ciphertexts.json",
                "-trustees", "src/commonTest/data/rave/eg/trustees",
                "-out", "testOut/testRunMixnetPep/",
                "-nthreads", "25",
            )
        )
    }

}

