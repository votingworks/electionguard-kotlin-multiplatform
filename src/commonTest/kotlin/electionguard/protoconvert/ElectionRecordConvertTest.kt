package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ElectionRecordConvertTest {

    @Test
    fun roundtripElectionRecord() {
        runTest {
            val context = productionGroup()
            val electionRecord = generateElectionRecord(context)
            val convertTo = ElectionRecordToProto(context)
            val proto = convertTo.translateToProto(electionRecord)
            val convertFrom = ElectionRecordFromProto(context)
            val roundtrip = convertFrom.translateFromProto(proto)
            assertEquals(roundtrip.version, electionRecord.version)
            assertEquals(roundtrip.constants, electionRecord.constants)
            assertEquals(roundtrip.manifest, electionRecord.manifest)
            assertEquals(roundtrip.context, electionRecord.context)
            assertEquals(roundtrip.guardianRecords, electionRecord.guardianRecords)
            assertEquals(roundtrip.devices, electionRecord.devices)
            assertEquals(roundtrip.encryptedTally, electionRecord.encryptedTally)
            assertEquals(roundtrip.decryptedTally, electionRecord.decryptedTally)
            assertEquals(roundtrip.spoiledBallots, electionRecord.spoiledBallots)
            assertEquals(roundtrip.availableGuardians, electionRecord.availableGuardians)
            assertEquals(roundtrip, electionRecord)
        }
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
            List(11) { generateGuardianRecord(it, context) },
            List(12) { generateEncryptionDevice(it) },
            CiphertextTallyConvertTest.generateFakeTally(context),
            PlaintextTallyConvertTest.generateFakeTally(0, context),
            List(13) { SubmittedBallotConvertTest.generateSubmittedBallot(it, context) },
            List(9) { PlaintextTallyConvertTest.generateFakeTally(it, context) },
            List(8) { generateAvailableGuardian(it, context) },
        )
    }

    private fun generateElectionConstants(): ElectionConstants {
        return ElectionConstants(
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
            List(13) { generateElementModP(context) },
            List(13) { generateSchnorrProof(context) },
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