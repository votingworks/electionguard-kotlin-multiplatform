package electionguard.publish

import electionguard.core.productionGroup
import java.io.FileNotFoundException
import kotlin.test.*

class ReadAndCheckManifestTest {
    val group = productionGroup()

    @Test
    fun readAndCheckManifestJsonTest() {
        val (isJsonOrg, manifest, manifestBytes) = readAndCheckManifest(group, "src/commonTest/data/startConfigJson/manifest.json")
        assertTrue(isJsonOrg)
        assertNotNull(manifest)
        assertNotNull(manifestBytes)
        println("manifest = $manifest")
    }

    @Test
    fun readAndCheckManifestJsonDirTest() {
        val (isJsonOrg, manifest, manifestBytes) = readAndCheckManifest(group, "src/commonTest/data/startManifestJson")
        assertTrue(isJsonOrg)
        assertNotNull(manifest)
        assertNotNull(manifestBytes)
        println("manifest = $manifest")
    }

    @Test
    fun readAndCheckManifestJsonZipTest() {
        val (isJsonOrg, manifest, manifestBytes) = readAndCheckManifest(group, "src/commonTest/data/testElectionRecord/allAvailableJson.zip")
        assertTrue(isJsonOrg)
        assertNotNull(manifest)
        assertNotNull(manifestBytes)
        println("manifest = $manifest")
    }

    @Test
    fun readAndCheckManifestProtoTest() {
        val (isJsonOrg, manifest, manifestBytes) = readAndCheckManifest(group, "src/commonTest/data/startManifestProto/manifest.protobuf")
        assertFalse(isJsonOrg)
        assertNotNull(manifest)
        assertNotNull(manifestBytes)
        println("manifest = $manifest")
    }

    @Test
    fun readAndCheckManifestProtoDirTest() {
        val (isJsonOrg, manifest, manifestBytes) = readAndCheckManifest(group, "src/commonTest/data/startManifestProto")
        assertFalse(isJsonOrg)
        assertNotNull(manifest)
        assertNotNull(manifestBytes)
        println("manifest = $manifest")
    }

    @Test
    fun missingManifestTest() {
        val ex = assertFailsWith<FileNotFoundException>(
            block = { readAndCheckManifest(group, "src/commonTest/data/missing") }
        )
        assertContains(ex.message!!, "No such file or directory")
    }

    @Test
    fun badManifestJsonTest() {
        val ex = assertFailsWith<Exception>(
            block = { readAndCheckManifest(group, "src/commonTest/data/startConfigJson/constants.json") }
        )
        assertContains(ex.message!!, "Unexpected JSON token")
    }

    @Test
    fun badManifestProtoTest() {
        val ex = assertFailsWith<RuntimeException>(
            block = { readAndCheckManifest(group, "src/commonTest/data/startConfigProto/electionConfig.protobuf") }
        )
        assertContains(ex.message!!, "Unrecognized wire type")
    }

}

