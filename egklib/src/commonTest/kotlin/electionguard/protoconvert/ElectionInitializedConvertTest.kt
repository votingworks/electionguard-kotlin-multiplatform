package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import electionguard.publish.Publisher
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElectionInitializedConvertTest {
    val outputDir = "testOut/protoconvert/ElectionInitializedConvertTest"
    val publisher = makePublisher(outputDir, true)

    @Test
    fun roundtripElectionInitialized() {
        val context = tinyGroup()
        val electionRecord = generateElectionInitialized(publisher, context)
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
        assertEquals(roundtrip.extendedBaseHash, electionRecord.extendedBaseHash)
        assertEquals(roundtrip.guardians, electionRecord.guardians)
        assertEquals(roundtrip.metadata, electionRecord.metadata)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }
}

fun generateElectionInitialized(publisher: Publisher, context: GroupContext): ElectionInitialized {
    //     val config: ElectionConfig,
    //    /** The joint public key (K) in the ElectionGuard Spec. */
    //    val jointPublicKey: ElementModP,
    //    val manifestHash: UInt256, // matches Manifest.cryptoHash
    //    val cryptoExtendedBaseHash: UInt256, // qbar
    //    val guardians: List<Guardian>,
    //    val metadata: Map<String, String>
    val (manifest, config) = generateElectionConfig(publisher, 6, 4)
    return ElectionInitialized(
        config,
        generateElementModP(context),
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
        "guardian$seq",
        (seq + 1),
        List(3) { generateSchnorrProof(context) },
    )
}
