package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.GroupContext

fun GroupContext.importTallyResult(tally: electionguard.protogen.TallyResult?):
        Result<TallyResult, String> {

    if (tally == null) {
        return Err("Null TallyResult")
    }
    val electionInitialized: Result<ElectionInitialized, String> = this.importElectionInitialized(tally.electionInit)
    val encryptedTally: Result<EncryptedTally, String> = this.importEncryptedTally(tally.encryptedTally)

    val errors = getAllErrors(electionInitialized, encryptedTally)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(TallyResult(
        this,
        electionInitialized.unwrap(),
        encryptedTally.unwrap(),
        tally.ballotIds,
        tally.tallyIds,
        tally.metadata.associate { it.key to it.value }
    ))
}

fun GroupContext.importDecryptionResult(decrypt: electionguard.protogen.DecryptionResult):
        Result<DecryptionResult, String> {
    val tallyResult = this.importTallyResult(decrypt.tallyResult)
    val decryptedTally = this.importDecryptedTallyOrBallot(decrypt.decryptedTally)

    val (guardians, gerrors) =
        decrypt.lagrangeCoordinates.map { importLagrangeCoefficient(it) }.partition()

    val errors = getAllErrors(tallyResult, decryptedTally) + gerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptionResult(
        tallyResult.unwrap(),
        decryptedTally.unwrap(),
        guardians,
        decrypt.metadata.associate { it.key to it.value }
    ))
}

private fun GroupContext.importLagrangeCoefficient(guardian: electionguard.protogen.LagrangeCoordinate):
        Result<LagrangeCoordinate, String> {
    val lagrangeCoefficient = this.importElementModQ(guardian.lagrangeCoefficient)
        ?: return Err("Failed to translate LagrangeCoordinate from proto")

    return Ok(
        LagrangeCoordinate(
            guardian.guardianId,
            guardian.xCoordinate,
            lagrangeCoefficient,
        )
    )
}

////////////////////////////////////////////////////////

fun TallyResult.publishTallyResult() =
    electionguard.protogen.TallyResult(
        this.electionInitialized.publishElectionInitialized(),
        this.encryptedTally.publishEncryptedTally(),
        this.ballotIds,
        this.tallyIds,
        this.metadata.entries.map { electionguard.protogen.TallyResult.MetadataEntry(it.key, it.value) }
    )

fun DecryptionResult.publishDecryptionResult() =
    electionguard.protogen.DecryptionResult(
        this.tallyResult.publishTallyResult(),
        this.decryptedTally.publishDecryptedTallyOrBallot(),
        this.lagrangeCoordinates.map { it.publishLagrangeCoordinate() },
        this.metadata.entries.map { electionguard.protogen.DecryptionResult.MetadataEntry(it.key, it.value) }
    )

private fun LagrangeCoordinate.publishLagrangeCoordinate() =
    electionguard.protogen.LagrangeCoordinate(
        this.guardianId,
        this.xCoordinate,
        this.lagrangeCoefficient.publishElementModQ(),
    )