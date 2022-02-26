package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectionRecordConvertTest {

    @Test
    fun roundtripElectionRecord() {
        val context = tinyGroup()
        val electionRecord = generateElectionRecord(context)
        val convertTo = ElectionRecordToProto(context)
        val proto = convertTo.translateToProto(electionRecord)
        val convertFrom = ElectionRecordFromProto(context)
        val roundtrip = convertFrom.translateFromProto(proto)
        assertEquals(roundtrip.protoVersion, electionRecord.protoVersion)
        assertEquals(roundtrip.constants, electionRecord.constants)
        assertEquals(roundtrip.manifest, electionRecord.manifest)
        assertEquals(roundtrip.context, electionRecord.context)
        assertEquals(roundtrip.guardianRecords, electionRecord.guardianRecords)
        assertEquals(roundtrip.devices, electionRecord.devices)
        assertEquals(roundtrip.encryptedTally, electionRecord.encryptedTally)
        assertEquals(roundtrip.decryptedTally, electionRecord.decryptedTally)
        assertEquals(roundtrip.availableGuardians, electionRecord.availableGuardians)

        // LOOK the line below fails with "Out of memory. Java heap space" when electionRecord.acceptedBallots is not null
        //Task :jvmTest FAILED
        //FAILURE: Build failed with an exception.
        //* What went wrong:
        //Execution failed for task ':jvmTest'.
        //> Failed to notify test listener.
        //   > Java heap space
        // * What went wrong:
        //Out of memory. Java heap space
        // assertEquals(null, electionRecord.acceptedBallots)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }

    private fun generateElectionRecord(context: GroupContext): ElectionRecord {
        //     val version: String,
        //    val constants: ElectionConstants,
        //    val manifest: Manifest,
        //    val context: ElectionContext,
        //    val guardianRecords: List<GuardianRecord>,
        //    val devices: List<EncryptionDevice>,
        //    val encryptedTally: CiphertextTally?,
        //    val decryptedTally: PlaintextTally?,
        //    val acceptedBallots: Iterable<SubmittedBallot>?,
        //    val spoiledBallots: Iterable<PlaintextTally>?,
        //    val availableGuardians: List<AvailableGuardian>?
        return ElectionRecord(
            "version",
            generateElectionConstants(),
            ManifestConvertTest.generateFakeManifest(context),
            generateElectionContext(context),
            List(3) { generateGuardianRecord(it, context) },
            List(1) { generateEncryptionDevice(it) },
            CiphertextTallyConvertTest.generateFakeTally(context),
            PlaintextTallyConvertTest.generateFakeTally(0, context),
            List(3) { generateAvailableGuardian(it, context) },
        )
    }

    private fun generateElectionConstants(): ElectionConstants {
        return ElectionConstants(
            "fake",
            ByteArray(4) {42},
            ByteArray(4) {-43},
            ByteArray(4) {44},
            ByteArray(4) {-45}
        )
    }

    //    val numberOfGuardians: Int,
    //    val quorum: Int,
    //    val jointPublicKey: ElementModP,
    //    val manifestHash: ElementModQ,
    //    val cryptoBaseHash: ElementModQ,
    //    val cryptoExtendedBaseHash: ElementModQ,
    //    val commitmentHash: ElementModQ,
    //    val extendedData: Map<String, String>?
    private fun generateElectionContext(context: GroupContext): ElectionContext {
        return ElectionContext(
            42,
            24,
            generateElementModP(context),
            generateElementModQ(context),
            generateElementModQ(context),
            generateElementModQ(context),
            generateElementModQ(context),
            mapOf("you" to "me", "me" to "you", "had" to "to"),
        )
    }

    //     val guardianId: String,
    //    val xCoordinate: Int,
    //    val electionPublicKey: ElementModP,
    //    val coefficientCommitments: List<ElementModP>,
    //    val coefficientProofs: List<SchnorrProof>
    private fun generateGuardianRecord(seq: Int, context: GroupContext): GuardianRecord {
        return GuardianRecord(
            "guardian $seq",
            seq + 1,
            generateElementModP(context),
            List(3) { generateElementModP(context) },
            List(3) { generateSchnorrProof(context) },
        )
    }

    //    val deviceId: Long,
    //    val sessionId: Long,
    //    val launchCode: Long,
    //    val location: String,
    private fun generateEncryptionDevice(seq: Int): EncryptionDevice {
        return EncryptionDevice(
            Long.MAX_VALUE,
            seq + 1L,
            seq + 42L,
            "location $seq",
        )
    }

    private fun generateAvailableGuardian(seq : Int, context: GroupContext): AvailableGuardian {
        return AvailableGuardian(
            "aguardian $seq",
            seq + 1,
            generateElementModQ(context),
        )
    }

}