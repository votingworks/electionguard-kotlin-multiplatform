package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.getSystemDate
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
    return proto.importElectionRecord(this, filename)
}

fun electionguard.protogen.ElectionRecord.importElectionRecord(
    groupContext: GroupContext,
    filename: String
): ElectionInitialized {
    val electionConstants = this.constants?.let { convertConstants(this.constants) }
    val manifest = importManifest(this.manifest).getOrThrow { IllegalStateException(it) }
    val electionContext = groupContext.importContext(this.context)
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
    val metadata: MutableMap<String, String> = mutableMapOf()
    metadata.put("CreatedBy", "ElectionRecordVer1FromProto")
    metadata.put("CreatedOn", getSystemDate().toString())
    metadata.put("CreatedFrom", filename)

    val config = ElectionConfig(
        ElectionRecordPath.PROTO_VERSION,
        electionConstants,
        manifest,
        electionContext.numberOfGuardians,
        electionContext.quorum,
        metadata
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
        metadata
    )
}

private fun convertConstants(
    constants: electionguard.protogen.ElectionConstants?
): ElectionConstants? {
    if (constants == null) {
        return null
    }

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

private fun GroupContext.importContext(context: electionguard.protogen.ElectionContext?): ElectionContext {

    if (context == null) {
        throw IllegalStateException("Missing context")
    }

    val jointPublicKey = this.importElementModP(context.jointPublicKey)
    val manifestHash = importUInt256(context.manifestHash)
    val cryptoBaseHash = importUInt256(context.cryptoBaseHash)
    val cryptoExtendedBaseHash = importUInt256(context.cryptoExtendedBaseHash)
    val commitmentHash = importUInt256(context.commitmentHash)

    if (jointPublicKey == null || manifestHash == null || cryptoBaseHash == null ||
        cryptoExtendedBaseHash == null || commitmentHash == null
    ) {
        throw IllegalStateException("Failed to importContext")
    }

    return ElectionContext(
        context.numberOfGuardians,
        context.quorum,
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

    if (electionPublicKey == null || coefficientCommitments == null || coefficientProofs == null) {
        throw IllegalStateException("Failed to importGuardianRecord")
    }

    return Guardian(
        this.guardianId,
        this.xCoordinate.toUInt(),
        coefficientCommitments,
        coefficientProofs,
    )
}