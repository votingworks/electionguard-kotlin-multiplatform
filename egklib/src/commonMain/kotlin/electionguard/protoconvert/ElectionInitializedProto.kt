package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.GroupContext

fun electionguard.protogen.ElectionInitialized.import(group: GroupContext):
        Result<ElectionInitialized, String> {

    val electionConfig = this.config?.import() ?: Err("Null ElectionConfig")
    val jointPublicKey = group.importElementModP(this.jointPublicKey)
        .toResultOr { "ElectionInitialized jointPublicKey was malformed or missing" }
    val cryptoExtendedBaseHash = importUInt256(this.extendedBaseHash)
        .toResultOr { "ElectionInitialized cryptoExtendedBaseHash was malformed or missing" }

    val (guardians, gerrors) = this.guardians.map { it.import(group) }.partition()

    val errors = getAllErrors(electionConfig, jointPublicKey, cryptoExtendedBaseHash) + gerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(ElectionInitialized(
        electionConfig.unwrap(),
        jointPublicKey.unwrap(),
        cryptoExtendedBaseHash.unwrap(),
        guardians,
        this.metadata.associate { it.key to it.value }
    ))
}

private fun electionguard.protogen.Guardian.import(group: GroupContext):
        Result<Guardian, String> {
    val (coefficientProofs, perrors) =
        this.coefficientProofs.map {
            group.importSchnorrProof(it)
                 .toResultOr { "Guardian ${this.guardianId} coefficientProof was malformed or missing" }
        }.partition()

    if (perrors.isNotEmpty()) {
        return Err(perrors.joinToString("\n"))
    }

    return Ok(
        Guardian(
            this.guardianId,
            this.xCoordinate,
            coefficientProofs,
        )
    )
}

////////////////////////////////////////////////////////

fun ElectionInitialized.publishProto() =
    electionguard.protogen.ElectionInitialized(
        this.config.publishProto(),
        this.jointPublicKey.publishProto(),
        this.extendedBaseHash.publishProto(),
        this.guardians.map { it.publishProto() },
        this.metadata.entries.map { electionguard.protogen.ElectionInitialized.MetadataEntry(it.key, it.value) }
    )

private fun Guardian.publishProto() =
    electionguard.protogen.Guardian(
        this.guardianId,
        this.xCoordinate,
        this.coefficientProofs.map { it.publishProto() },
    )