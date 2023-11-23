package electionguard.cli

import kotlin.test.Test

class RunAccumulateTallyTest {

    @Test
    fun testAccumulateTally() {
        RunAccumulateTally.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/eg",
                "-eballots", "../testOut/rave/EB",
                "-out", "../testOut/rave/eg"
            )
        )
    }

}

