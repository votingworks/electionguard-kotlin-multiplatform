package electionguard.cli

import kotlin.test.Test

class RunCreateInputBallotsTest {

    @Test
    fun testCreateInputBallots() {
        RunCreateInputBallots.main(
            arrayOf(
                "-manifest", "src/commonTest/data/rave/eg",
                "-out", "../testOut/rave/inputBallots",
                "-n", "22"
            )
        )
    }

}

