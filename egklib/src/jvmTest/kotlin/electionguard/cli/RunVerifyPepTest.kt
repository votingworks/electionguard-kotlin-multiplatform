package electionguard.cli

import kotlin.test.Test

class RunVerifyPepTest {

    @Test
    fun testRunVerifyPep() {
        RunVerifyPep.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/working/eg/encryption",
                "--pepBallotDir", "testOut/testRunMixnetBlindTrustPep",
            )
        )
    }
}