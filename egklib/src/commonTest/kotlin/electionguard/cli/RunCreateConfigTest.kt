package electionguard.cli

import kotlin.test.Test

class RunCreateConfigTest {

    @Test
    fun testCreateConfigProto() {
        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/startManifestProto/manifest.protobuf",
                "-nguardians", "3",
                "-quorum", "3",
                "-out",
                "testOut/config/startConfigProto",
                "-device",
                "device information",
            )
        )
    }

    @Test
    fun testCreateConfigJson() {
        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/startManifestJson/manifest.json",
                "-nguardians", "3",
                "-quorum", "3",
                "-out",
                "testOut/config/startConfigJson",
                "-device",
                "device information",
            )
        )
    }

    @Test
    fun testCreateConfigDirectoryProto() {
        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/startManifestProto",
                "-nguardians", "3",
                "-quorum", "3",
                "-createdBy", "testCreateConfigDirectoryProto",
                "-out",
                "testOut/config/testCreateFromDirectoryProto",
                "-device",
                "device information"
            )
        )
    }

    @Test
    fun testCreateConfigDirectoryJson() {
        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest",
                "src/commonTest/data/startManifestJson",
                "-nguardians", "3",
                "-quorum", "3",
                "-createdBy", "testCreateConfigDirectoryJson",
                "-out",
                "testOut/config/testCreateConfigDirectoryJson",
                "-device",
                "device information",
            )
        )
    }

}

