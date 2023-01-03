package webapps.electionguard.keyceremony

import kotlin.test.Test

class RunRemoteKeyCeremonyTest {
    private val configDir = "/home/snake/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/start"

    @Test
    fun testRemoteKeyCeremonyMain() {
        main(
            arrayOf(
                "-in",
                configDir,
                "-trustees",
                "/home/snake/tmp/electionguard/RunRemoteKeyCeremonyTest/private_data/trustees",
                "-out",
                "/home/snake/tmp/electionguard/RunRemoteKeyCeremonyTest",
                "-remoteUrl",
                "http://0.0.0.0:11180"
            )
        )
    }

}

