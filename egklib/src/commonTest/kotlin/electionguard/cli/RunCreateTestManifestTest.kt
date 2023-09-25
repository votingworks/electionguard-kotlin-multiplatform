package electionguard.cli

import electionguard.core.productionGroup
import electionguard.publish.readAndCheckManifestBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunCreateTestManifestTest {

    @Test
    fun runCreateTestManifest() {
        createTestManifest(
            arrayOf(
                "-ncontests", "20",
                "-nselections", "5",
                "-out",
                "testOut/manifest/runCreateTestManifest",
            )
        )

        RunConvertManifest.main(
            arrayOf(
                "-manifest",
                "testOut/manifest/runCreateTestManifest",
                "-out",
                "testOut/manifest/testConvertManifestFromJsonToProto",
            )
        )

        RunConvertManifest.main(
            arrayOf(
                "-manifest",
                "testOut/manifest/testConvertManifestFromJsonToProto",
                "-out",
                "testOut/manifest/testConvertManifestFromProtoToJson",
            )
        )

        // get all and see if they compare equal
        val group = productionGroup()
        val (isJsonOrg, manifestOrg, _) = readAndCheckManifestBytes(group, "testOut/manifest/runCreateTestManifest")
        assertTrue(isJsonOrg)

        val (isJsonProto, manifestProto, _) = readAndCheckManifestBytes(group, "testOut/manifest/testConvertManifestFromJsonToProto")
        assertFalse(isJsonProto)
        assertEquals(manifestOrg, manifestProto)

        val (isJsonRoundTrip, manifestRoundtrip, _) = readAndCheckManifestBytes(group, "testOut/manifest/testConvertManifestFromProtoToJson")
        assertTrue(isJsonRoundTrip)
        assertEquals(manifestProto, manifestRoundtrip)

        assertEquals(manifestOrg, manifestRoundtrip)
    }

}

