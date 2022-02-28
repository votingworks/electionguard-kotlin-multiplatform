package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import pbandk.ByteArr

data class ElectionRecordToProto(val groupContext: GroupContext) {

    fun translateToProto(election: ElectionRecord): electionguard.protogen.ElectionRecord {
        val manifestConverter = ManifestToProto(groupContext)
        val manifest = manifestConverter.translateToProto(election.manifest)

        val ciphertextTally: electionguard.protogen.CiphertextTally? =
            if (election.encryptedTally == null) { null } else {
                CiphertextTallyConvert(groupContext).translateToProto(election.encryptedTally)
            }
        val decryptedTally: electionguard.protogen.PlaintextTally? =
            if (election.decryptedTally == null) { null } else {
                PlaintextTallyConvert(groupContext).translateToProto(election.decryptedTally)
            }
        val guardianRecords: List<electionguard.protogen.GuardianRecord> =
            if (election.guardianRecords == null || election.guardianRecords.isEmpty()) { emptyList() } else {
                election.guardianRecords.map { convertGuardianRecord(it) }
            }
        val availableGuardians: List<electionguard.protogen.AvailableGuardian> =
            if (election.availableGuardians == null || election.availableGuardians.isEmpty()) { emptyList() } else {
                election.availableGuardians.map { convertAvailableGuardian(it) }
            }

        return electionguard.protogen.ElectionRecord(
            election.protoVersion,
            convertConstants(election.constants),
            manifest,
            convertContext(election.context),
            guardianRecords,
            election.devices.map { convertDevice(it) },
            ciphertextTally,
            decryptedTally,
            availableGuardians,
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
            convertElementModP(guardianRecord.electionPublicKey),
            guardianRecord.coefficientCommitments.map { convertElementModP(it)},
            guardianRecord.coefficientProofs.map { convertSchnorrProof(it)},
        )
    }
}