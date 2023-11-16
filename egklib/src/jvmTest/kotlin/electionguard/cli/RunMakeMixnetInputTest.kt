package electionguard.cli

import kotlin.test.Test

class RunMakeMixnetInputTest {

    @Test
    fun testMakeMixnetInput() {
        RunMakeMixnetInput.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/someAvailableJson",
                "--outputFile", "testOut/testMakeMixnetInput/mixnetInput.json",
            )
        )
    }

}

