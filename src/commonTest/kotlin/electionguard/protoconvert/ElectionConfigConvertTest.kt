package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElectionConfigConvertTest {

    @Test
    fun roundtripElectionConfig() {
        val context = tinyGroup()
        val electionRecord = generateElectionConfig()
        val proto = electionRecord.publishElectionConfig()
        val roundtrip = importElectionConfig(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        assertEquals(roundtrip.protoVersion, electionRecord.protoVersion)
        assertEquals(roundtrip.constants, electionRecord.constants)
        assertEquals(roundtrip.manifest, electionRecord.manifest)
        assertEquals(roundtrip.numberOfGuardians, electionRecord.numberOfGuardians)
        assertEquals(roundtrip.quorum, electionRecord.quorum)
        assertEquals(roundtrip.metadata, electionRecord.metadata)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateElectionConfig(): ElectionConfig {
    //     val protoVersion: String,
    //    val constants: ElectionConstants,
    //    val manifest: Manifest,
    //    /** The number of guardians necessary to generate the public key. */
    //    val numberOfGuardians: Int,
    //    /** The quorum of guardians necessary to decrypt an election. Must be <= number_of_guardians. */
    //    val quorum: Int,
    //    /** arbitrary key/value metadata. */
    //    val metadata: Map<String, String>,
    return ElectionConfig(
        "version",
        generateElectionConstants(),
        ManifestConvertTest.generateFakeManifest(),
        6, 4
    )
}

fun generateElectionConstants(): ElectionConstants {
    return ElectionConstants(
        "fake",
        ByteArray(4) { 42 },
        ByteArray(4) { -43 },
        ByteArray(4) { 44 },
        ByteArray(4) { -45 }
    )
}