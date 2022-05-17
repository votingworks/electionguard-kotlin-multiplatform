package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.GroupContext

fun GroupContext.importTallyResult(tally : electionguard.protogen.TallyResult?):
        Result<TallyResult, String> {

    if (tally == null) {
        return Err("Null TallyResult")
    }
    val electionInitialized: Result<ElectionInitialized, String> = this.importElectionInitialized(tally.electionInit)
    val ciphertextTally: Result<CiphertextTally, String> = this.importCiphertextTally(tally.ciphertextTally)

    val errors = getAllErrors(electionInitialized, ciphertextTally)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(TallyResult(
        this,
        electionInitialized.unwrap(),
        ciphertextTally.unwrap(),
        tally.ballotIds,
        tally.tallyIds,
    ))
}

fun GroupContext.importDecryptionResult(decrypt : electionguard.protogen.DecryptionResult):
        Result<DecryptionResult, String>  {
    val tallyResult = this.importTallyResult(decrypt.tallyResult)
    val decryptedTally = this.importPlaintextTally(decrypt.decryptedTally)

    val (guardians, gerrors) =
        decrypt.decryptingGuardians.map { importAvailableGuardian(it) }.partition()

    val errors = getAllErrors(tallyResult, decryptedTally) + gerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptionResult(
        tallyResult.unwrap(),
        decryptedTally.unwrap(),
        guardians,
        decrypt.metadata.associate {it.key to it.value}
    ))
}

private fun GroupContext.importAvailableGuardian(guardian: electionguard.protogen.DecryptingGuardian):
        Result<DecryptingGuardian, String> {
    val lagrangeCoefficient = this.importElementModQ(guardian.lagrangeCoefficient)

    if (lagrangeCoefficient == null) {
        return Err("Failed to translate AvailableGuardian from proto, missing lagrangeCoefficient")
    }
    return Ok(DecryptingGuardian(
        guardian.guardianId,
        guardian.xCoordinate,
        lagrangeCoefficient,
    ))
}

////////////////////////////////////////////////////////

fun TallyResult.publishTallyResult(): electionguard.protogen.TallyResult {
    return electionguard.protogen.TallyResult(
        this.electionIntialized.publishElectionInitialized(),
        this.ciphertextTally.publishCiphertextTally(),
        this.ballotIds,
        this.tallyIds,
    )
}

fun DecryptionResult.publishDecryptionResult(): electionguard.protogen.DecryptionResult {
    return electionguard.protogen.DecryptionResult(
        this.tallyResult.publishTallyResult(),
        this.decryptedTally.publishPlaintextTally(),
        this.availableGuardians.map { it.publishAvailableGuardian() },
        this.metadata.entries.map { electionguard.protogen.DecryptionResult.MetadataEntry(it.key, it.value)}
    )
}

private fun DecryptingGuardian.publishAvailableGuardian(): electionguard.protogen.DecryptingGuardian {
    return electionguard.protogen.DecryptingGuardian(
        this.guardianId,
        this.xCoordinate,
        this.lagrangeCoordinate.publishElementModQ(),
    )
}