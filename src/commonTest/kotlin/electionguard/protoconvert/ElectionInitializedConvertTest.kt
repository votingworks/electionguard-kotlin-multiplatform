package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElectionInitializedConvertTest {

    @Test
    fun roundtripElectionInitialized() {
        val context = tinyGroup()
        val electionRecord = generateElectionInitialized(context)
        val proto = electionRecord.publishElectionInitialized()
        val roundtrip = context.importElectionInitialized(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        //     val config: ElectionConfig,
        //    /** The joint public key (K) in the ElectionGuard Spec. */
        //    val jointPublicKey: ElementModP,
        //    val manifestHash: UInt256, // matches Manifest.cryptoHash
        //    val cryptoExtendedBaseHash: UInt256, // qbar
        //    val guardians: List<Guardian>,
        //    val metadata: Map<String, String>
        assertEquals(roundtrip.config, electionRecord.config)
        assertEquals(roundtrip.jointPublicKey, electionRecord.jointPublicKey)
        assertEquals(roundtrip.manifestHash, electionRecord.manifestHash)
        assertEquals(roundtrip.cryptoExtendedBaseHash, electionRecord.cryptoExtendedBaseHash)
        assertEquals(roundtrip.guardians, electionRecord.guardians)
        assertEquals(roundtrip.metadata, electionRecord.metadata)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateElectionInitialized(context: GroupContext): ElectionInitialized {
    //     val config: ElectionConfig,
    //    /** The joint public key (K) in the ElectionGuard Spec. */
    //    val jointPublicKey: ElementModP,
    //    val manifestHash: UInt256, // matches Manifest.cryptoHash
    //    val cryptoExtendedBaseHash: UInt256, // qbar
    //    val guardians: List<Guardian>,
    //    val metadata: Map<String, String>
    return ElectionInitialized(
        generateElectionConfig(),
        generateElementModP(context),
        generateUInt256(context),
        generateUInt256(context),
        generateUInt256(context),
        List(3) { generateGuardian(it, context) },
    )
}

//     val guardianId: String,
//    val xCoordinate: Int,
//    val electionPublicKey: ElementModP,
//    val coefficientCommitments: List<ElementModP>,
//    val coefficientProofs: List<SchnorrProof>
private fun generateGuardian(seq: Int, context: GroupContext): Guardian {
    return Guardian(
        "guardian $seq",
        (seq + 1).toUInt(),
        List(3) { generateElementModP(context) },
        List(3) { generateSchnorrProof(context) },
    )
}
