package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging
private val logger = KotlinLogging.logger("ElectionRecordFromProto")

data class ElectionRecordFromProto(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.ElectionRecord?): ElectionRecord? {
        if (proto == null) {
            return null
        }

        val manifestConverter = ManifestFromProto(groupContext)
        val manifest = manifestConverter.translateFromProto(proto.manifest)

        val ciphertextTally: CiphertextTally? =
                CiphertextTallyConvert(groupContext).translateFromProto(proto.ciphertextTally)

        val decryptedTally: PlaintextTally? =
                PlaintextTallyConvert(groupContext).translateFromProto(proto.decryptedTally)

        val availableGuardians: List<AvailableGuardian>? =
            proto.availableGuardians.map { convertAvailableGuardian(it) }.noNullValuesOrNull()

        val constants = convertConstants(proto.constants)

        val electionContext = convertContext(proto.context)

        val guardianRecords = proto.guardianRecords.map { convertGuardianRecord(it) }.noNullValuesOrNull()

        if (constants == null ||
            manifest == null ||
            electionContext == null ||
            guardianRecords == null ||
            availableGuardians == null ||
            availableGuardians.isEmpty()) {
            logger.error { "Failed to translate election record from proto, missing fields" }
            return null
        }

        return ElectionRecord(
            proto.protoVersion,
            constants,
            manifest,
            electionContext,
            guardianRecords,
            proto.devices.map { convertDevice(it) },
            ciphertextTally,
            decryptedTally,
            null,
            null,
            availableGuardians
        )
    }

    private fun convertAvailableGuardian(proto: electionguard.protogen.AvailableGuardian?): AvailableGuardian? {
        if (proto == null) {
            return null
        }

        val lagrangeCoordinate = convertElementModQ(proto.lagrangeCoordinate, groupContext)

        if (lagrangeCoordinate == null) {
            logger.error { "lagrangeCoordinate was malformed or out of bounds" }
            return null
        }

        return AvailableGuardian(proto.guardianId, proto.xCoordinate, lagrangeCoordinate)
    }

    private fun convertConstants(constants: electionguard.protogen.ElectionConstants?): ElectionConstants? {
        if (constants == null) {
            return null
        }

        // TODO: do we have to worry about any of the fields of the deserialized protobuf being missing / null?

        return ElectionConstants(
            constants.largePrime.array,
            constants.smallPrime.array,
            constants.cofactor.array,
            constants.generator.array,
        )
    }

    private fun convertContext(context: electionguard.protogen.ElectionContext?): ElectionContext? {
        if (context == null) {
            return null
        }
        val jointPublicKey = convertElementModP(context.jointPublicKey, groupContext)
        val manifestHash = convertElementModQ(context.manifestHash, groupContext)
        val cryptoBaseHash = convertElementModQ(context.cryptoBaseHash, groupContext)
        val cryptoExtendedBaseHash = convertElementModQ(context.cryptoExtendedBaseHash, groupContext)
        val commitmentHash = convertElementModQ(context.commitmentHash, groupContext)

        if (jointPublicKey == null || manifestHash == null || cryptoBaseHash == null || cryptoExtendedBaseHash == null || commitmentHash == null) {
            logger.error { "Failed to translate election context from proto, missing fields" }
            return null
        }

        // TODO: do we have to worry about any of the fields of the deserialized protobuf being missing / null?

        return ElectionContext(
            context.numberOfGuardians,
            context.quorum,
            jointPublicKey,
            manifestHash,
            cryptoBaseHash,
            cryptoExtendedBaseHash,
            commitmentHash,
            context.extendedData.associate { it.key to it.value }
        )
    }

    private fun convertDevice(device: electionguard.protogen.EncryptionDevice): EncryptionDevice {

        // TODO: do we have to worry about any of the fields of the deserialized protobuf being missing / null?

        return EncryptionDevice(
            device.deviceId,
            device.sessionId,
            device.launchCode,
            device.location
        )
    }

    private fun convertGuardianRecord(guardianRecord: electionguard.protogen.GuardianRecord?): GuardianRecord? {
        if (guardianRecord == null) {
            return null
        }
        val electionPublicKey = convertElementModP(guardianRecord.electionPublicKey, groupContext)
        val coefficientCommitments = guardianRecord.coefficientCommitments.map { convertElementModP(it, groupContext)}.noNullValuesOrNull()
        val coefficientProofs = guardianRecord.coefficientProofs.map { convertSchnorrProof(it, groupContext)}.noNullValuesOrNull()

        // TODO: do we have to worry about any of the fields of the deserialized protobuf being missing / null?
        //   Or the coefficient lists being empty?

        if (electionPublicKey == null || coefficientCommitments == null || coefficientProofs == null) {
            logger.error { "Failed to translate guardian record from proto, missing fields" }
            return null
        }

        return GuardianRecord(
            guardianRecord.guardianId,
            guardianRecord.xCoordinate,
            electionPublicKey,
            coefficientCommitments,
            coefficientProofs,
        )
    }
}