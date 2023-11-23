package electionguard.cli

import kotlin.test.Test

class RunMixnetBlindTrustPepTest {

    @Test
    fun testRunMixnetBlindTrustPep() {
        RunMixnetBlindTrustPep.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/eg",
                "-eballots", "src/commonTest/data/rave/bb/PB",
                "--mixnetFile", "src/commonTest/data/rave/vf/after-mix-2-ciphertexts.json",
                "-trustees", "src/commonTest/data/rave/eg/trustees",
                "-out", "../testOut/rave/pep/",
                "-nthreads", "33",
            )
        )
    }

}

