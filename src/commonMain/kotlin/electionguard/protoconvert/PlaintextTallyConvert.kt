package electionguard.protoconvert

import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging
private val logger = KotlinLogging.logger("PlaintextTallyConvert")

fun electionguard.protogen.PlaintextTally.importPlaintextTally(
    groupContext: GroupContext
): PlaintextTally? {

    // TODO: can it ever occur that we have zero contests?
    if (this.contests.isEmpty()) {
        logger.error { "No contests in PlaintextTally" }
        return null
    }

    val contestMap =
        this.contests
            .associate { it.contestId to it.importContest(groupContext) }
            .noNullValuesOrNull()

    if (contestMap == null) {
        logger.error { "Failed to convert PlaintextTally" }
        return null
    }

    return PlaintextTally(this.tallyId, contestMap)
}

private fun electionguard.protogen.PlaintextTallyContest.importContest(
    groupContext: GroupContext
): PlaintextTally.Contest? {

    // TODO: can it ever occur that we have zero selections?
    if (this.selections.isEmpty()) {
        logger.error { "No selections in PlaintextTallyContest" }
        return null
    }

    val selectionMap =
        this.selections
            .associate { it.selectionId to it.importSelection(groupContext) }
            .noNullValuesOrNull()

    if (selectionMap == null) {
        logger.error { "Failed to convert PlaintextTallyContest" }
        return null
    }

    return PlaintextTally.Contest(this.contestId, selectionMap)
}

private fun electionguard.protogen.PlaintextTallySelection.importSelection(
    groupContext: GroupContext
): PlaintextTally.Selection? {
    val value = groupContext.importElementModP(this.value)
    val message = groupContext.importCiphertext(this.message)
    val shares = this.shares.map { it.importShare(groupContext) }.noNullValuesOrNull()

    if (value == null || message == null || shares == null) {
        logger.error { "Failed to convert PlaintextTallySelection" }
        return null
    }

    // TODO: can it ever occur that we have zero shares?
    if (shares.isEmpty()) {
        logger.error { "No shares in PlaintextTallySelection" }
        return null
    }

    return PlaintextTally.Selection(this.selectionId, this.tally, value, message, shares)
}

private fun electionguard.protogen.CiphertextDecryptionSelection.importShare(
    groupContext: GroupContext
) : DecryptionShare.DecryptionShareSelection? {

    val share = groupContext.importElementModP(this.share)
    val proof = groupContext.importChaumPedersenProof(this.proof)
    val recoveredParts = this.recoveredParts
    val parts =
        if (recoveredParts != null) {
            recoveredParts.fragments
                .map { it.importRecoveredParts(groupContext) }
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
        this.selectionId,
        this.guardianId,
        share,
        proof,
        parts,
    )
}

private fun electionguard.protogen.CiphertextCompensatedDecryptionSelection.importRecoveredParts(
    groupContext: GroupContext
): DecryptionShare.DecryptionShareCompensatedSelection? {
    val share = groupContext.importElementModP(this.share)
    val recoveryKey = groupContext.importElementModP(this.recoveryKey)
    val proof = groupContext.importChaumPedersenProof(this.proof)

    if (share == null || recoveryKey == null || proof == null) {
        logger.error { "Failed to convert CiphertextCompensatedDecryptionSelection" }
        return null
    }

    return DecryptionShare.DecryptionShareCompensatedSelection(
        this.guardianId,
        this.missingGuardianId,
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