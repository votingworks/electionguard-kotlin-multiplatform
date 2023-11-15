package electionguard.cli

import kotlin.test.Test

class RunMakeMixnetInputTest {

    @Test
    fun testMakeMixnetInput() {
        RunMakeMixnetInput.main(
            arrayOf(
                "-in", "src/commonTest/data/workflow/someAvailableJson",
                "-out", "testOut/testMakeMixnetInput/mixnetInput.json",
            )
        )
    }

}

