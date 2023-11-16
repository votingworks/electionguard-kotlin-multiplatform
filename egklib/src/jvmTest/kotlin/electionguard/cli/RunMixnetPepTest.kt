package electionguard.cli

import kotlin.test.Test

class RunMixnetPepTest {

    @Test
    fun testRunMixnetBlindTrustPep() {
        RunMixnetPep.main(
            arrayOf(
                "-in", "src/commonTest/data/mixnet/working/eg/encryption",
                "--mixnetFile", "src/commonTest/data/mixnet/working/vf/after-mix-2-ciphertexts.json",
                "-trustees", "src/commonTest/data/mixnet/working/eg/trustees",
                "-out", "testOut/testRunMixnetPep/",
                "-nthreads", "25",
            )
        )
    }

}

