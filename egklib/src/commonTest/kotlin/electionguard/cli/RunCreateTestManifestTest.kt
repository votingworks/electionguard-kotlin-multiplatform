package electionguard.cli

import kotlin.test.Test

class RunCreateTestManifestTest {

    @Test
    fun runCreateTestManifest() {
        RunCreateTestManifest.main(
            arrayOf(
                "-ncontests", "20",
                "-nselections", "5",
                "-out", "testOut/manifest/runCreateTestManifest",
            )
        )
    }

}

