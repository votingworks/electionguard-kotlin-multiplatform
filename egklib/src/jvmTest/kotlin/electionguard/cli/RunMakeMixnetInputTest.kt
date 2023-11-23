package electionguard.cli

import kotlin.test.Test

class RunMakeMixnetInputTest {

    @Test
    fun testMakeMixnetInput() {
        RunMakeMixnetInput.main(
            arrayOf(
                "-eballots", "../testOut/rave/EB",
                "--outputFile", "../testOut/rave/vg/input-ciphertexts.json",
                "-json"
            )
        )
    }

}

