package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElectionConfigConvertTest {

    @Test
    fun roundtripElectionConfig() {
        val electionRecord = generateElectionConfig(6, 4)
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

fun generateElectionConfig(nguardians: Int, quorum: Int): ElectionConfig {
    return ElectionConfig(
        "version",
        generateElectionConstants(),
        ManifestConvertTest.generateFakeManifest(),
        nguardians, quorum
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