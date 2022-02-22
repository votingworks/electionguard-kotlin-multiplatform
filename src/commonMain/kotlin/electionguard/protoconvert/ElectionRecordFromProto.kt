package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext

data class ElectionRecordFromProto(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.ElectionRecord): ElectionRecord {
        val manifestConverter = ManifestFromProto(groupContext)
        val manifest = manifestConverter.translateFromProto(proto.manifest?: throw IllegalStateException("manifest cant be null"))


        val ciphertextTally: CiphertextTally? =
            if (proto.ciphertextTally == null) { null } else {
                CiphertextTallyConvert(groupContext).translateFromProto(proto.ciphertextTally)
            }
        val decryptedTally: PlaintextTally? =
            if (proto.decryptedTally == null) { null } else {
                PlaintextTallyConvert(groupContext).translateFromProto(proto.decryptedTally)
            }
        val availableGuardians: List<AvailableGuardian>? =
            if (proto.availableGuardians.isEmpty()) { null } else {
                proto.availableGuardians.map {convertAvailableGuardian(it)}
            }


        return ElectionRecord(
            proto.version,
            convertConstants(proto.constants?: throw IllegalStateException("constants cant be null")),
            manifest,
            convertContext(proto.context?: throw IllegalStateException("context cant be null")),
            proto.guardianRecords.map { convertGuardianRecord(it) },
            proto.devices.map { convertDevice(it) },
            ciphertextTally,
            decryptedTally,
            null,
            null,
            availableGuardians
        )
    }

    private fun convertAvailableGuardian(proto: electionguard.protogen.AvailableGuardian): AvailableGuardian {
        return AvailableGuardian(
            proto.guardianId,
            proto.xCoordinate,
            convertElementModQ(proto.lagrangeCoordinate?: throw IllegalStateException("context cant be null"), groupContext)
        )
    }

    private fun convertConstants(constants: electionguard.protogen.ElectionConstants): ElectionConstants {
        return ElectionConstants(
            constants.largePrime.array,
            constants.smallPrime.array,
            constants.cofactor.array,
            constants.generator.array,
        )
    }

    private fun convertContext(context: electionguard.protogen.ElectionContext): ElectionContext {
        return ElectionContext(
            context.numberOfGuardians,
            context.quorum,
            convertElementModP(context.jointPublicKey?: throw IllegalStateException("jointPublicKey cant be null"), groupContext),
            convertElementModQ(context.manifestHash?: throw IllegalStateException("manifestHash cant be null"), groupContext),
            convertElementModQ(context.cryptoBaseHash?: throw IllegalStateException("cryptoBaseHash cant be null"), groupContext),
            convertElementModQ(context.cryptoExtendedBaseHash?: throw IllegalStateException("cryptoExtendedBaseHash cant be null"), groupContext),
            convertElementModQ(context.commitmentHash?: throw IllegalStateException("commitmentHash cant be null"), groupContext),
            context.extendedData.associate{ it.key to it.value}
        )
    }

    private fun convertDevice(device: electionguard.protogen.EncryptionDevice): EncryptionDevice {
        return EncryptionDevice(
            device.deviceId,
            device.sessionId,
            device.launchCode,
            device.location
        )
    }

    private fun convertGuardianRecord(guardianRecord: electionguard.protogen.GuardianRecord): GuardianRecord {
         return GuardianRecord(
            guardianRecord.guardianId,
            guardianRecord.xCoordinate,
            convertElementModP(guardianRecord.electionPublicKey?: throw IllegalStateException("electionPublicKey cant be null"), groupContext),
            guardianRecord.coefficientCommitments.map { convertElementModP(it, groupContext)},
            guardianRecord.coefficientProofs.map { convertSchnorrProof(it, groupContext)},
        )
    }
}