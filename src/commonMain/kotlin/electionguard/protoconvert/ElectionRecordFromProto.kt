package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ElectionRecordFromProto")

fun electionguard.protogen.ElectionRecord.importElectionRecord(
    groupContext: GroupContext
): ElectionRecord {
    val electionConstants = this.constants?.let { convertConstants(this.constants) }

    val manifest = this.manifest?.let { this.manifest.importManifest() }

    val availableGuardians: List<AvailableGuardian>? =
        this.availableGuardians
            .map { it.importAvailableGuardian() }
            .noNullValuesOrNull()

    val electionContext = this.context?.let { this.context.importContext(groupContext) }

    val guardianRecords =
        this.guardianRecords.map { it.importGuardianRecord(groupContext) }.noNullValuesOrNull()

    if (electionConstants == null || manifest == null) {
        logger.error { "Failed to translate election record from proto, missing fields" }
        throw IllegalStateException(
            "Failed to translate election record from proto, missing fields"
        )
    }

    return ElectionRecord(
        this.protoVersion,
        electionConstants,
        manifest,
        electionContext,
        guardianRecords,
        this.devices.map { it.importDevice() },
        this.ciphertextTally?.let { this.ciphertextTally.importCiphertextTally(groupContext) },
        this.decryptedTally?.let { this.decryptedTally.importPlaintextTally(groupContext) },
        availableGuardians,
    )
}

private fun electionguard.protogen.AvailableGuardian.importAvailableGuardian(): AvailableGuardian {
    return AvailableGuardian(this.guardianId, this.xCoordinate, this.lagrangeCoordinateInt)
}

private fun convertConstants(
    constants: electionguard.protogen.ElectionConstants?
): ElectionConstants? {
    if (constants == null) {
        return null
    }

    // TODO: do we have to worry about any of the fields of the deserialized protobuf being
    //  missing / null?

    return ElectionConstants(
        constants.name,
        constants.largePrime.array,
        constants.smallPrime.array,
        constants.cofactor.array,
        constants.generator.array,
    )
}

private fun electionguard.protogen.ElectionContext.importContext(
    groupContext: GroupContext
): ElectionContext? {

    val jointPublicKey = groupContext.importElementModP(this.jointPublicKey)
    val manifestHash = importUInt256(this.manifestHash)
    val cryptoBaseHash = importUInt256(this.cryptoBaseHash)
    val cryptoExtendedBaseHash = importUInt256(this.cryptoExtendedBaseHash)
    val commitmentHash = importUInt256(this.commitmentHash)

    if (jointPublicKey == null || manifestHash == null || cryptoBaseHash == null ||
        cryptoExtendedBaseHash == null || commitmentHash == null
    ) {
        logger.error { "Failed to translate election context from proto, missing fields" }
        return null
    }

    // TODO: do we have to worry about any of the fields of the deserialized protobuf being
    //  missing / null?

    return ElectionContext(
        this.numberOfGuardians,
        this.quorum,
        jointPublicKey,
        manifestHash,
        cryptoBaseHash,
        cryptoExtendedBaseHash,
        commitmentHash,
        this.extendedData.associate { it.key to it.value }
    )
}

private fun electionguard.protogen.EncryptionDevice.importDevice(): EncryptionDevice {
    // TODO: do we have to worry about any of the fields of the deserialized protobuf being
    //  missing / null?

    return EncryptionDevice(this.deviceId, this.sessionId, this.launchCode, this.location)
}

private fun electionguard.protogen.GuardianRecord.importGuardianRecord(
    groupContext: GroupContext
): GuardianRecord? {

    val electionPublicKey = groupContext.importElementModP(this.guardianPublicKey)
    val coefficientCommitments =
        this.coefficientCommitments.map { groupContext.importElementModP(it) }.noNullValuesOrNull()
    val coefficientProofs =
        this.coefficientProofs.map { groupContext.importSchnorrProof(it) }.noNullValuesOrNull()

    // TODO: do we have to worry about any of the fields of the deserialized protobuf being
    //  missing / null?
    //   Or the coefficient lists being empty?

    if (electionPublicKey == null || coefficientCommitments == null || coefficientProofs == null) {
        logger.error { "Failed to translate guardian record from proto, missing fields" }
        return null
    }

    return GuardianRecord(
        this.guardianId,
        this.xCoordinate,
        electionPublicKey,
        coefficientCommitments,
        coefficientProofs,
    )
}