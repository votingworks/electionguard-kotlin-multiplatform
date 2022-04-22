package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapResult
import com.github.michaelbull.result.partition
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
    val manifestHash = importUInt256(init.manifestHash)
    val cryptoExtendedBaseHash = importUInt256(init.cryptoExtendedBaseHash)

    val (guardians, errors) = init.guardians.map { this.importGuardian(it) }.partition()
    if (!errors.isEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    if (electionConfig is Err || jointPublicKey == null || manifestHash == null || cryptoExtendedBaseHash == null) {
        return Err("Failed to translate election record from proto, missing fields")
    }

    return Ok(ElectionInitialized(
        electionConfig.unwrap(),
        jointPublicKey,
        manifestHash,
        cryptoExtendedBaseHash,
        guardians,
        init.metadata.associate {it.key to it.value}
    ))
}

private fun GroupContext.importGuardian(
    guardian: electionguard.protogen.Guardian
): Result<Guardian, String> {

    val coefficientCommitments =
        guardian.coefficientCommitments.mapResult {
            val modP = this.importElementModP(it)
            if (modP != null) Ok(modP) else Err("null coefficientCommitments")
        }

    val coefficientProofs =
        guardian.coefficientProofs.mapResult {
            val proof = this.importSchnorrProof(it)
            if (proof != null) Ok(proof) else Err("null coefficientProofs")
        }

    if (coefficientCommitments is Err || coefficientProofs is Err) {
        return Err("Failed to translate guardian record from proto, missing fields")
    }

    return Ok(Guardian(
        guardian.guardianId,
        guardian.xCoordinate,
        coefficientCommitments.unwrap(),
        coefficientProofs.unwrap(),
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