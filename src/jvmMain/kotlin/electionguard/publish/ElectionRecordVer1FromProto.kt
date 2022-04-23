package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.noNullValuesOrNull
import electionguard.protoconvert.importElementModP
import electionguard.protoconvert.importManifest
import electionguard.protoconvert.importSchnorrProof
import electionguard.protoconvert.importUInt256
import pbandk.decodeFromStream
import java.io.FileInputStream

fun GroupContext.readElectionRecordVer1(filename: String): ElectionInitialized {
    var proto: electionguard.protogen.ElectionRecord
    FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionRecord.decodeFromStream(inp) }
    return proto.importElectionRecord(this)
}

fun electionguard.protogen.ElectionRecord.importElectionRecord(
    groupContext: GroupContext
): ElectionInitialized {
    val electionConstants = this.constants?.let { convertConstants(this.constants) }
    val manifest = importManifest(this.manifest).getOrThrow { IllegalStateException( "importManifest") }
    val electionContext = this.context?.importContext(groupContext) ?: throw IllegalStateException("context missing");
    val guardianRecords = this.guardianRecords.map { it.importGuardianRecord(groupContext) }

    if (electionConstants == null) {
        logger.error { "Failed to translate election record from proto, missing fields" }
        throw IllegalStateException(
            "Failed to translate election record from proto, missing fields"
        )
    }

    //     val protoVersion: String,
    //    val constants: ElectionConstants,
    //    val manifest: Manifest,
    //    /** The number of guardians necessary to generate the public key. */
    //    val numberOfGuardians: Int,
    //    /** The quorum of guardians necessary to decrypt an election. Must be <= number_of_guardians. */
    //    val quorum: Int,
    //    /** arbitrary key/value metadata. */
    //    val metadata: Map<String, String> = emptyMap(),
    val config = ElectionConfig(
        this.protoVersion,
        electionConstants,
        manifest,
        electionContext.numberOfGuardians,
        electionContext.quorum,
    )

    //     val config: ElectionConfig,
    //    /** The joint public key (K) in the ElectionGuard Spec. */
    //    val jointPublicKey: ElementModP,
    //    val manifestHash: UInt256, // matches Manifest.cryptoHash
    //    val cryptoExtendedBaseHash: UInt256, // qbar
    //    val guardians: List<Guardian>,
    //    val metadata: Map<String, String> = emptyMap(),
    return ElectionInitialized(
        config,
        electionContext.jointPublicKey,
        electionContext.manifestHash,
        electionContext.cryptoExtendedBaseHash,
        guardianRecords,
    )
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

data class ElectionContext(
    val numberOfGuardians : Int,
    val quorum : Int,
    val jointPublicKey: ElementModP,
    val manifestHash: UInt256,
    val cryptoExtendedBaseHash: UInt256
)

private fun electionguard.protogen.ElectionContext.importContext(
    groupContext: GroupContext
): ElectionContext {

    val jointPublicKey = groupContext.importElementModP(this.jointPublicKey)
    val manifestHash = importUInt256(this.manifestHash)
    val cryptoBaseHash = importUInt256(this.cryptoBaseHash)
    val cryptoExtendedBaseHash = importUInt256(this.cryptoExtendedBaseHash)
    val commitmentHash = importUInt256(this.commitmentHash)

    if (jointPublicKey == null || manifestHash == null || cryptoBaseHash == null ||
        cryptoExtendedBaseHash == null || commitmentHash == null
    ) {
        throw IllegalStateException("Failed to importContext")
    }

    // TODO: do we have to worry about any of the fields of the deserialized protobuf being
    //  missing / null?

    return ElectionContext(
        this.numberOfGuardians,
        this.quorum,
        jointPublicKey,
        manifestHash,
        cryptoExtendedBaseHash,
    )
}

private fun electionguard.protogen.GuardianRecord.importGuardianRecord(
    groupContext: GroupContext
): Guardian {

    val electionPublicKey = groupContext.importElementModP(this.guardianPublicKey)
    val coefficientCommitments =
        this.coefficientCommitments.map { groupContext.importElementModP(it) }.noNullValuesOrNull()
    val coefficientProofs =
        this.coefficientProofs.map { groupContext.importSchnorrProof(it) }.noNullValuesOrNull()

    // TODO: do we have to worry about any of the fields of the deserialized protobuf being
    //  missing / null?
    //   Or the coefficient lists being empty?

    if (electionPublicKey == null || coefficientCommitments == null || coefficientProofs == null) {
        throw IllegalStateException("Failed to importGuardianRecord")
    }

    return Guardian(
        this.guardianId,
        this.xCoordinate,
        coefficientCommitments,
        coefficientProofs,
    )
}