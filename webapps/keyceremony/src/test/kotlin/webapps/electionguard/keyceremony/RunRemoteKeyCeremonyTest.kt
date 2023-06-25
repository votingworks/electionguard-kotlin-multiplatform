package webapps.electionguard.keyceremony

import kotlin.test.Test

class RunRemoteKeyCeremonyTest {
    private val configDir = "/home/snake/dev/github/electionguard-kotlin-multiplatform/egklib/src/commonTest/data/startConfigProto"

    @Test
    fun testRemoteKeyCeremonyMain() {
        main(
            arrayOf(
                "-in",
                configDir,
                "-out",
                "/home/snake/tmp/electionguard/RunRemoteKeyCeremonyTest",
                "-remoteUrl",
                "https://localhost:11183",
                "-keystore",
                "../keystore.jks",
                "-kpwd",
                "ksPassword",
                "-epwd",
                "egPassword",
            )
        )
    }

}

