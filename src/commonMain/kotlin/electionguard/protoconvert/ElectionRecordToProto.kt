package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import pbandk.ByteArr

data class ElectionRecordToProto(val groupContext: GroupContext) {

    fun translateToProto(election: ElectionRecord): electionguard.protogen.ElectionRecord {
        return translateToProto(
            election.protoVersion,
            election.manifest,
            election.context,
            election.constants,
            election.guardianRecords,
            election.devices,
            election.encryptedTally,
            election.decryptedTally,
            election.availableGuardians,
        )
    }

    fun translateToProto(
        version : String,
        manifest: Manifest,
        context: ElectionContext,
        constants: ElectionConstants,
        guardianRecords: List<GuardianRecord>?,
        devices: Iterable<EncryptionDevice>,
        encryptedTally: CiphertextTally?,
        decryptedTally: PlaintextTally?,
        availableGuardians: List<AvailableGuardian>?,
    ): electionguard.protogen.ElectionRecord {
        val manifestConverter = ManifestToProto(groupContext)
        val manifestProto = manifestConverter.translateToProto(manifest)

        val ciphertextTallyProto: electionguard.protogen.CiphertextTally? =
            if (encryptedTally == null) { null } else {
                CiphertextTallyConvert(groupContext).translateToProto(encryptedTally)
            }
        val decryptedTallyProto: electionguard.protogen.PlaintextTally? =
            if (decryptedTally == null) { null } else {
                PlaintextTallyConvert(groupContext).translateToProto(decryptedTally)
            }
        val guardianRecordsProto: List<electionguard.protogen.GuardianRecord> =
            if (guardianRecords == null || guardianRecords.isEmpty()) { emptyList() } else {
                guardianRecords.map {convertGuardianRecord(it)}
            }
        val availableGuardiansProto: List<electionguard.protogen.AvailableGuardian> =
            if (availableGuardians == null || availableGuardians.isEmpty()) { emptyList() } else {
                availableGuardians.map {convertAvailableGuardian(it)}
            }

        return electionguard.protogen.ElectionRecord(
            version,
            convertConstants(constants),
            manifestProto,
            convertContext(context),
            guardianRecordsProto,
            devices.map { convertDevice(it) },
            ciphertextTallyProto,
            decryptedTallyProto,
            availableGuardiansProto,
        )
    }

    private fun convertAvailableGuardian(proto: AvailableGuardian): electionguard.protogen.AvailableGuardian {
        return electionguard.protogen.AvailableGuardian(
            proto.guardianId,
            proto.xCoordinate,
            convertElementModQ(proto.lagrangeCoordinate)
        )
    }

    private fun convertConstants(constants: ElectionConstants): electionguard.protogen.ElectionConstants {
        return electionguard.protogen.ElectionConstants(
            constants.name,
            ByteArr(constants.largePrime),
            ByteArr(constants.smallPrime),
            ByteArr(constants.cofactor),
            ByteArr(constants.generator),
        )
    }

    private fun convertContext(context: ElectionContext): electionguard.protogen.ElectionContext {
        val extendedData: List<electionguard.protogen.ElectionContext.ExtendedDataEntry> =
            if (context.extendedData == null) { emptyList() } else {
                context.extendedData.map{(key, value) -> convertExtendedData(key, value)}
            }


        return electionguard.protogen.ElectionContext(
            context.numberOfGuardians,
            context.quorum,
            convertElementModP(context.jointPublicKey),
            convertElementModQ(context.manifestHash),
            convertElementModQ(context.cryptoBaseHash),
            convertElementModQ(context.cryptoExtendedBaseHash),
            convertElementModQ(context.commitmentHash),
            extendedData
        )
    }

    private fun convertExtendedData(key : String, value : String): electionguard.protogen.ElectionContext.ExtendedDataEntry {
        return electionguard.protogen.ElectionContext.ExtendedDataEntry(key, value)
    }


    private fun convertDevice(device: EncryptionDevice): electionguard.protogen.EncryptionDevice {
        return electionguard.protogen.EncryptionDevice(
            device.deviceId,
            device.sessionId,
            device.launchCode,
            device.location
        )
    }

    private fun convertGuardianRecord(guardianRecord: GuardianRecord): electionguard.protogen.GuardianRecord {
         return electionguard.protogen.GuardianRecord(
            guardianRecord.guardianId,
            guardianRecord.xCoordinate,
            convertElementModP(guardianRecord.guardianPublicKey),
            guardianRecord.coefficientCommitments.map { convertElementModP(it)},
            guardianRecord.coefficientProofs.map { convertSchnorrProof(it)},
        )
    }
}