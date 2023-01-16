package electionguard.keyceremony

import kotlin.test.Test

class RunKeyCeremonyTest {

    @Test
    fun testKeyCeremonyMain() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/start",
                "-trustees",
                "testOut/RunKeyCeremonyTest/private_data/trustees",
                "-out",
                "testOut/RunKeyCeremonyTest",
            )
        )
    }

    @Test
    fun testKeyCeremonyManifestDirectory() {
        main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/start/",
                "-nguardians", "3",
                "-quorum", "3",
                "-trustees",
                "testOut/RunKeyCeremonyTest/private_data/trustees",
                "-out",
                "testOut/RunKeyCeremonyTest",
            )
        )
    }

    @Test
    fun testKeyCeremonyManifestProto() {
        main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/start/manifest.protobuf",
                "-nguardians", "3",
                "-quorum", "3",
                "-trustees",
                "testOut/RunKeyCeremonyTest/private_data/trustees",
                "-out",
                "testOut/RunKeyCeremonyTest",
            )
        )
    }


    @Test
    fun testKeyCeremonyManifestJson() {
        main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/start/manifest.json",
                "-nguardians", "3",
                "-quorum", "3",
                "-trustees",
                "testOut/RunKeyCeremonyTest/private_data/trustees",
                "-out",
                "testOut/RunKeyCeremonyTest",
            )
        )
    }

}

