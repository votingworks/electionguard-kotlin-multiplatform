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
        val proto = electionRecord.publishProto()
        val roundtrip = proto.import(context).getOrThrow { IllegalStateException(it) }
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
    val config = generateElectionConfig(6, 4)
    return ElectionInitialized(
        config,
        generateElementModP(context),
        config.manifest.cryptoHash,
        generateUInt256(context),
        generateUInt256(context),
        List(6) { generateGuardian(it, context) },
    )
}

//     val guardianId: String,
//    val xCoordinate: Int,
//    val electionPublicKey: ElementModP,
//    val coefficientCommitments: List<ElementModP>,
//    val coefficientProofs: List<SchnorrProof>
fun generateGuardian(seq: Int, context: GroupContext): Guardian {
    return Guardian(
        "guardian $seq",
        (seq + 1),
        List(3) { generateSchnorrProof(context) },
    )
}
