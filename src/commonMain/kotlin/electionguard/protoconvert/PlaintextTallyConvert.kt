package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging
private val logger = KotlinLogging.logger("PlaintextTallyConvert")

fun GroupContext.importPlaintextTally(tally: electionguard.protogen.PlaintextTally?):
        Result<PlaintextTally, String> {
    if (tally == null) {
        return Err("Null PlaintextTally")
    }

    // TODO: can it ever occur that we have zero contests?
    if (tally.contests.isEmpty()) {
        return Err( "No contests in PlaintextTally")
    }

    val contestMap =
        tally.contests
            .associate { it.contestId to this.importContest(it) }
            .noNullValuesOrNull()

    if (contestMap == null) {
        return Err( "Failed to convert PlaintextTally")
    }

    return Ok(PlaintextTally(tally.tallyId, contestMap))
}

private fun GroupContext.importContest(contest: electionguard.protogen.PlaintextTallyContest): PlaintextTally.Contest? {

    // TODO: can it ever occur that we have zero selections?
    if (contest.selections.isEmpty()) {
        logger.error { "No selections in PlaintextTallyContest" }
        return null
    }

    val selectionMap =
        contest.selections
            .associate { it.selectionId to this.importSelection(it) }
            .noNullValuesOrNull()

    if (selectionMap == null) {
        logger.error { "Failed to convert PlaintextTallyContest" }
        return null
    }

    return PlaintextTally.Contest(contest.contestId, selectionMap)
}

private fun GroupContext.importSelection(selection: electionguard.protogen.PlaintextTallySelection): PlaintextTally.Selection? {
    val value = this.importElementModP(selection.value)
    val message = this.importCiphertext(selection.message)
    val shares = selection.shares.map { this.importShare(it) }.noNullValuesOrNull()

    if (value == null || message == null || shares == null) {
        logger.error { "Failed to convert PlaintextTallySelection" }
        return null
    }

    // TODO: can it ever occur that we have zero shares?
    if (shares.isEmpty()) {
        logger.error { "No shares in PlaintextTallySelection" }
        return null
    }

    return PlaintextTally.Selection(selection.selectionId, selection.tally, value, message, shares)
}

private fun GroupContext.importShare(sselection: electionguard.protogen.CiphertextDecryptionSelection): DecryptionShare.DecryptionShareSelection? {

    val share = this.importElementModP(sselection.share)
    val proof = this.importChaumPedersenProof(sselection.proof)
    val recoveredParts = sselection.recoveredParts
    val parts =
        if (recoveredParts != null) {
            recoveredParts.fragments
                .map { this.importRecoveredParts(it) }
                .noNullValuesOrNull()
        } else {
            null
        }

    if (share == null) {
        logger.error { "Missing CiphertextDecryptionSelection share" }
        return null
    }

    when {
        proof == null && parts == null -> {
            logger.error {
                "Failed to convert CiphertextDecryptionSelection: no proof or parts present"
            }
            return null
        }
        proof != null && parts != null -> {
            logger.error {
                "Failed to convert CiphertextDecryptionSelection: both proof *and* parts " +
                    "present"
            }
            return null
        }
    }

    // If we get here, one of proof or parts is non-null
    return DecryptionShare.DecryptionShareSelection(
        sselection.selectionId,
        sselection.guardianId,
        share,
        proof,
        parts,
    )
}

private fun GroupContext.importRecoveredParts(parts: electionguard.protogen.CiphertextCompensatedDecryptionSelection): DecryptionShare.DecryptionShareCompensatedSelection? {
    val share = this.importElementModP(parts.share)
    val recoveryKey = this.importElementModP(parts.recoveryKey)
    val proof = this.importChaumPedersenProof(parts.proof)

    if (share == null || recoveryKey == null || proof == null) {
        logger.error { "Failed to convert CiphertextCompensatedDecryptionSelection" }
        return null
    }

    return DecryptionShare.DecryptionShareCompensatedSelection(
        parts.guardianId,
        parts.missingGuardianId,
        share,
        recoveryKey,
        proof,
    )
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
                this.shares.map { it.publishShare(this.selectionId) }
            )
    }

private fun DecryptionShare.DecryptionShareSelection.publishShare(selectionId : String):
    electionguard.protogen.CiphertextDecryptionSelection {
        // either proof or recovered_parts is non null
        val proofOrParts: electionguard.protogen.CiphertextDecryptionSelection.ProofOrParts<*>?
        if (this.proof != null) {
            proofOrParts =
                electionguard.protogen
                    .CiphertextDecryptionSelection
                    .ProofOrParts
                    .Proof(this.proof.publishChaumPedersenProof())
        } else if (this.recoveredParts != null) {
            val pparts = this.recoveredParts.map { it.publishRecoveredParts(selectionId) }
            proofOrParts =
                electionguard.protogen
                    .CiphertextDecryptionSelection
                    .ProofOrParts
                    .RecoveredParts(electionguard.protogen.RecoveredParts(pparts))
        } else {
            throw IllegalStateException(
                "CiphertextDecryptionSelection must have proof or recoveredParts"
            )
        }
        return electionguard.protogen
            .CiphertextDecryptionSelection(
                this.selectionId,
                this.guardianId,
                this.share.publishElementModP(),
                proofOrParts,
            )
    }

private fun DecryptionShare.DecryptionShareCompensatedSelection.publishRecoveredParts(selectionId : String):
    electionguard.protogen.CiphertextCompensatedDecryptionSelection {
        return electionguard.protogen
            .CiphertextCompensatedDecryptionSelection(
                selectionId,
                this.guardianId,
                this.missingGuardianId,
                this.share.publishElementModP(),
                this.recoveryKey.publishElementModP(),
                this.proof.publishChaumPedersenProof(),
            )
    }