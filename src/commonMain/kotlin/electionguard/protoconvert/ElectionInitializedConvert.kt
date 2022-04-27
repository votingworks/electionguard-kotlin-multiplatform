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

fun GroupContext.importElectionInitialized(init : electionguard.protogen.ElectionInitialized?):
        Result<ElectionInitialized, String> {
    if (init == null) {
        return Err("Null ElectionInitialized")
    }

    val electionConfig = importElectionConfig(init.config)
    val jointPublicKey = this.importElementModP(init.jointPublicKey)
        .toResultOr {"ElectionInitialized jointPublicKey was malformed or missing"}
    val manifestHash = importUInt256(init.manifestHash)
        .toResultOr {"ElectionInitialized manifestHash was malformed or missing"}
    val cryptoExtendedBaseHash = importUInt256(init.cryptoExtendedBaseHash)
        .toResultOr {"ElectionInitialized cryptoExtendedBaseHash was malformed or missing"}

    val (guardians, gerrors) = init.guardians.map { this.importGuardian(it) }.partition()

    val errors = getAllErrors(electionConfig, jointPublicKey, manifestHash, cryptoExtendedBaseHash) + gerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(ElectionInitialized(
        electionConfig.unwrap(),
        jointPublicKey.unwrap(),
        manifestHash.unwrap(),
        cryptoExtendedBaseHash.unwrap(),
        guardians,
        init.metadata.associate {it.key to it.value}
    ))
}

private fun GroupContext.importGuardian(
    guardian: electionguard.protogen.Guardian
): Result<Guardian, String> {

    val (coefficientCommitments, cerrors) =
        guardian.coefficientCommitments.map {
            this.importElementModP(it)
                .toResultOr {"Guardian ${guardian.guardianId} coefficientCommitment was malformed or missing"}
        }.partition()

    val (coefficientProofs, perrors) =
        guardian.coefficientProofs.map {
            this.importSchnorrProof(it)
                .toResultOr {"Guardian ${guardian.guardianId} coefficientProof was malformed or missing"}
        }.partition()

    val errors = cerrors + perrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(Guardian(
        guardian.guardianId,
        guardian.xCoordinate,
        coefficientCommitments,
        coefficientProofs,
    ))
}

////////////////////////////////////////////////////////

fun ElectionInitialized.publishElectionInitialized(): electionguard.protogen.ElectionInitialized {
    return electionguard.protogen.ElectionInitialized(
        this.config.publishElectionConfig(),
        this.jointPublicKey.publishElementModP(),
        this.manifestHash.publishUInt256(),
        this.cryptoExtendedBaseHash.publishUInt256(),
        this.guardians.map { it.publishGuardian() },
        this.metadata.entries.map { electionguard.protogen.ElectionInitialized.MetadataEntry(it.key, it.value)}
    )
}

private fun Guardian.publishGuardian(): electionguard.protogen.Guardian {
    val coefficientCommitments =
        this.coefficientCommitments.map { it.publishElementModP() }
    val coefficientProofs =
        this.coefficientProofs.map { it.publishSchnorrProof() }

    return electionguard.protogen
        .Guardian(
            this.guardianId,
            this.xCoordinate,
            coefficientCommitments,
            coefficientProofs,
        )
}