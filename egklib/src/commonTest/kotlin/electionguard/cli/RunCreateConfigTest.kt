package electionguard.cli

import kotlin.test.Test

class RunCreateConfigTest {

    @Test
    fun testCreateConfigDirectoryProto() {
        createConfig(
            arrayOf(
                "-manifest",
                "testOut/manifest/testConvertManifestFromJsonToProto",
                "-nguardians", "3",
                "-quorum", "3",
                "-createdBy", "3",
                "-out",
                "testOut/config/testCreateFromDirectoryProto",
            )
        )
    }

    @Test
    fun testCreateConfigDirectoryJson() {
        createConfig(
            arrayOf(
                "-manifest",
                "testOut/manifest/testConvertManifestFromProtoToJson",
                "-nguardians", "3",
                "-quorum", "3",
                "-createdBy", "3",
                "-out",
                "testOut/config/testCreateConfigDirectoryJson",
            )
        )
    }

    @Test
    fun testCreateConfigProto() {
        createConfig(
            arrayOf(
                "-manifest",
                "testOut/manifest/testConvertManifestFromJsonToProto/manifest.protobuf",
                "-nguardians", "3",
                "-quorum", "3",
                "-out",
                "testOut/config/testCreateConfigProto",
            )
        )
    }

    @Test
    fun testCreateConfigJson() {
        createConfig(
            arrayOf(
                "-manifest",
                "testOut/manifest/testConvertManifestFromProtoToJson/manifest.json",
                "-nguardians", "3",
                "-quorum", "3",
                "-out",
                "testOut/config/testCreateConfigJson",
            )
        )
    }

}

