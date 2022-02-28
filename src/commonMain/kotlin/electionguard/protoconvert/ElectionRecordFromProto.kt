package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext

fun electionguard.protogen.ElectionRecord.importElectionRecord(): ElectionRecord {
    if (this.constants == null) {
        throw IllegalStateException("constants cant be null")
    }
    val electionConstants = ElectionConstants(
        this.constants.name,
        this.constants.largePrime.array,
        this.constants.smallPrime.array,
        this.constants.cofactor.array,
        this.constants.generator.array,
        )

    // LOOK how to do this ? @danwallach ??
    val groupContext : GroupContext.from(electionConstants)

    if (this.manifest == null) {
        throw IllegalStateException("manifest cant be null")
    }
    if (this.context == null) {
        throw IllegalStateException("context cant be null")
    }

    return ElectionRecord(
        this.protoVersion,
        electionConstants,
        this.manifest.importManifest(groupContext),
        this.context.importContext(groupContext),
        this.guardianRecords.map { it.importGuardianRecord(groupContext) },
        this.devices.map { it.importDevice() },
        this.ciphertextTally?.let { this.ciphertextTally.importCiphertextTally(groupContext) },
        this.decryptedTally?.let { this.decryptedTally.importPlaintextTally(groupContext) },
        this.availableGuardians.map { it.importAvailableGuardian(groupContext) },
    )
}

private fun electionguard.protogen.AvailableGuardian.importAvailableGuardian(groupContext : GroupContext): AvailableGuardian {
    if (this.lagrangeCoordinate == null) {
        throw IllegalStateException("lagrangeCoordinate cant be null")
    }
    return AvailableGuardian(
        this.guardianId,
        this.xCoordinate,
        this.lagrangeCoordinate.importElementModQ(groupContext),
    )
}

private fun electionguard.protogen.ElectionContext.importContext(groupContext : GroupContext): ElectionContext {
    if (this.jointPublicKey == null) {
        throw IllegalStateException("jointPublicKey cant be null")
    }
    if (this.manifestHash == null) {
        throw IllegalStateException("manifestHash cant be null")
    }
    if (this.cryptoBaseHash == null) {
        throw IllegalStateException("cryptoBaseHash cant be null")
    }
    if (this.cryptoExtendedBaseHash == null) {
        throw IllegalStateException("cryptoExtendedBaseHash cant be null")
    }
    if (this.commitmentHash == null) {
        throw IllegalStateException("commitmentHash cant be null")
    }
    return ElectionContext(
        this.numberOfGuardians,
        this.quorum,
        this.jointPublicKey.importElementModP(groupContext),
        this.manifestHash.importElementModQ(groupContext),
        this.cryptoBaseHash.importElementModQ(groupContext),
        this.cryptoExtendedBaseHash.importElementModQ(groupContext),
        this.commitmentHash.importElementModQ(groupContext),
        this.extendedData.associate { it.key to it.value }
    )
}

private fun electionguard.protogen.EncryptionDevice.importDevice(): EncryptionDevice {
    return EncryptionDevice(
        this.deviceId,
        this.sessionId,
        this.launchCode,
        this.location
    )
}

private fun electionguard.protogen.GuardianRecord.importGuardianRecord(groupContext : GroupContext): GuardianRecord {
    if (this.electionPublicKey == null) {
        throw IllegalStateException("electionPublicKey cant be null")
    }
    return GuardianRecord(
        this.guardianId,
        this.xCoordinate,
        this.electionPublicKey.importElementModP(groupContext),
        this.coefficientCommitments.map { it.importElementModP(groupContext) },
        this.coefficientProofs.map { it.importSchnorrProof(groupContext) },
    )
}