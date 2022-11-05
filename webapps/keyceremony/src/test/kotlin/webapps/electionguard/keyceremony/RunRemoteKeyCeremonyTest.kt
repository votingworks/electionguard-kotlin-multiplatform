package webapps.electionguard.keyceremony

import kotlin.test.Test

class RunRemoteKeyCeremonyTest {

    @Test
    fun testRemoteKeyCeremonyMain() {
        main(
            arrayOf(
                "-in",
                "/home/snake/tmp/electionguard/kickstart/start",
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

