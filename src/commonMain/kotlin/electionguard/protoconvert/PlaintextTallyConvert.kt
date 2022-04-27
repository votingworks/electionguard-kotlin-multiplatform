package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext

fun GroupContext.importPlaintextTally(tally: electionguard.protogen.PlaintextTally?):
        Result<PlaintextTally, String> {
    if (tally == null) {
        return Err("Null PlaintextTally")
    }

    if (tally.contests.isEmpty()) {
        return Err( "No contests in PlaintextTally")
    }

    val (contests, errors) = tally.contests.map { this.importContest(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(PlaintextTally(tally.tallyId, contests.associateBy { it.contestId }))
}

private fun GroupContext.importContest(contest: electionguard.protogen.PlaintextTallyContest):
        Result<PlaintextTally.Contest, String> {

    if (contest.selections.isEmpty()) {
        return Err( "No selections in PlaintextTallyContest")
    }

    val (selections, errors) = contest.selections.map { this.importSelection(it) }.partition()
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(PlaintextTally.Contest(contest.contestId, selections.associateBy{ it.selectionId }))
}

private fun GroupContext.importSelection(selection: electionguard.protogen.PlaintextTallySelection):
        Result<PlaintextTally.Selection, String> {
    val value = this.importElementModP(selection.value)
        .toResultOr {"PlaintextTallySelection ${selection.selectionId} value was malformed or missing"}
    val message = this.importCiphertext(selection.message)
        .toResultOr {"PlaintextTallySelection ${selection.selectionId} message was malformed or missing"}
    val (shares, serrors) = selection.partialDecryptions.map { this.importPartialDecryption(it) }.partition()

    val errors = getAllErrors(value, message) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    // TODO: can it ever occur that we have zero shares?
    if (shares.isEmpty()) {
        return Err("No shares in PlaintextTallySelection")
    }

    return Ok(PlaintextTally.Selection(
        selection.selectionId,
        selection.tally,
        value.unwrap(),
        message.unwrap(),
        shares))
}

private fun GroupContext.importPartialDecryption(partial: electionguard.protogen.PartialDecryption):
        Result<DecryptionShare.PartialDecryption, String> {

    if (partial.proof == null && partial.recoveredParts == null) {
        return Err("PartialDecryption ${partial.selectionId} missing both proof and recoveredParts")
    }

    val share = this.importElementModP(partial.share)
        .toResultOr {"PartialDecryption ${partial.selectionId} share was malformed or missing"}

    val proof: GenericChaumPedersenProof? =
        if (partial.proof != null) this.importChaumPedersenProof(partial.proof) else null

    val recoveredParts = partial.recoveredParts
    val (parts, perrors) =
        if (recoveredParts != null) {
            recoveredParts.fragments.map { this.importRecoveredPartialDecryption(it) }.partition()
        } else {
            Pair(null, emptyList())
        }

    val errors = getAllErrors(share) + perrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptionShare.PartialDecryption(
        partial.selectionId,
        partial.guardianId,
        share.unwrap(),
        proof,
        parts,
    ))
}

private fun GroupContext.importRecoveredPartialDecryption(parts: electionguard.protogen.RecoveredPartialDecryption):
        Result<DecryptionShare.RecoveredPartialDecryption, String> {
    val share = this.importElementModP(parts.share)
        .toResultOr {"RecoveredPartialDecryption ${parts.selectionId} share was malformed or missing"}
    val recoveryKey = this.importElementModP(parts.recoveryKey)
        .toResultOr {"RecoveredPartialDecryption ${parts.selectionId} recoveryKey was malformed or missing"}
    val proof = this.importChaumPedersenProof(parts.proof)
        .toResultOr {"RecoveredPartialDecryption ${parts.selectionId} proof was malformed or missing"}

    val errors = getAllErrors(share, recoveryKey, proof)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptionShare.RecoveredPartialDecryption(
        parts.guardianId,
        parts.missingGuardianId,
        share.unwrap(),
        recoveryKey.unwrap(),
        proof.unwrap(),
    ))
}

//////////////////////////////////////////////////////////////////////////////////////////////

fun PlaintextTally.publishPlaintextTally(): electionguard.protogen.PlaintextTally {
    return electionguard.protogen
        .PlaintextTally(this.tallyId, this.contests.values.map { it.publishContest() })
}

private fun PlaintextTally.Contest.publishContest(): electionguard.protogen.PlaintextTallyContest {
    return electionguard.protogen
        .PlaintextTallyContest(this.contestId, this.selections.values.map { it.publishSelection() })
}

private fun PlaintextTally.Selection.publishSelection():
    electionguard.protogen.PlaintextTallySelection {
        return electionguard.protogen
            .PlaintextTallySelection(
                this.selectionId,
                this.tally,
                this.value.publishElementModP(),
                this.message.publishCiphertext(),
                this.partialDecryptions.map { it.publishPartialDecryption(this.selectionId) }
            )
    }

private fun DecryptionShare.PartialDecryption.publishPartialDecryption(selectionId : String):
    electionguard.protogen.PartialDecryption {
        // either proof or recovered_parts is non null
        val proofOrParts: electionguard.protogen.PartialDecryption.ProofOrParts<*>?
        if (this.proof != null) {
            proofOrParts =
                electionguard.protogen
                    .PartialDecryption
                    .ProofOrParts
                    .Proof(this.proof.publishChaumPedersenProof())
        } else if (this.recoveredParts != null) {
            val pparts = this.recoveredParts.map { it.publishRecoveredPartialDecryption(selectionId) }
            proofOrParts =
                electionguard.protogen
                    .PartialDecryption
                    .ProofOrParts
                    .RecoveredParts(electionguard.protogen.RecoveredParts(pparts))
        } else {
            throw IllegalStateException(
                "CiphertextDecryptionSelection must have proof or recoveredParts"
            )
        }
        return electionguard.protogen
            .PartialDecryption(
                this.selectionId,
                this.guardianId,
                this.share.publishElementModP(),
                proofOrParts,
            )
    }

private fun DecryptionShare.RecoveredPartialDecryption.publishRecoveredPartialDecryption(selectionId : String):
    electionguard.protogen.RecoveredPartialDecryption {
        return electionguard.protogen
            .RecoveredPartialDecryption(
                selectionId,
                this.guardianId,
                this.missingGuardianId,
                this.share.publishElementModP(),
                this.recoveryKey.publishElementModP(),
                this.proof.publishChaumPedersenProof(),
            )
    }