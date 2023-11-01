package electionguard.protoconvert

import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.UInt256
import electionguard.util.ErrorMessages

fun electionguard.protogen.ElectionInitialized.import(group: GroupContext, errs: ErrorMessages): ElectionInitialized? {
    val electionConfig = this.config?.import(errs.nested("ElectionConfig")) ?: errs.add("missing ElectionConfig")
    val jointPublicKey = group.importElementModP(this.jointPublicKey) ?: (errs.addNull("malformed jointPublicKey") as ElementModP?)
    val extendedBaseHash = importUInt256(this.extendedBaseHash) ?: (errs.addNull("malformed extendedBaseHash") as UInt256?)

    val guardians = this.guardians.map { it.import(group, errs.nested("Guardian ${it.guardianId}")) }

    return if (jointPublicKey == null || extendedBaseHash == null || errs.hasErrors()) null
        else ElectionInitialized(
            electionConfig.unwrap(),
            jointPublicKey,
            extendedBaseHash,
            guardians.filterNotNull(), // no errors means no nulls
            this.metadata.associate { it.key to it.value }
        )
}

private fun electionguard.protogen.Guardian.import(group: GroupContext, errs: ErrorMessages) : Guardian? {
    val coefficientProofs : List<SchnorrProof?> = this.coefficientProofs.mapIndexed { idx, it ->
            group.importSchnorrProof(it, errs.nested("SchnorrProof $idx"))
        }
    if (errs.hasErrors()) {
        return null
    }
    return Guardian(
            this.guardianId,
            this.xCoordinate,
            coefficientProofs.filterNotNull(), // no errs means no nulls
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