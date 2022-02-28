package electionguard.protoconvert

import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext

fun electionguard.protogen.PlaintextTally.importPlaintextTally(groupContext: GroupContext): PlaintextTally {
    return PlaintextTally(
        this.tallyId,
        this.contests.associate { it.contestId to it.importContest(groupContext) }
    )
}

private fun electionguard.protogen.PlaintextTallyContest.importContest(groupContext: GroupContext): PlaintextTally.Contest {
    return PlaintextTally.Contest(
        this.contestId,
        this.selections.associate { it.selectionId to it.importSelection(groupContext) }
    )
}

private fun electionguard.protogen.PlaintextTallySelection.importSelection(groupContext: GroupContext): PlaintextTally.Selection {
    if (this.value == null) {
        throw IllegalStateException("Selection value cant be null")
    }
    if (this.message == null) {
        throw IllegalStateException("Selection message cant be null")
    }
    return PlaintextTally.Selection(
        this.selectionId,
        this.tally,
        this.value.importElementModP(groupContext),
        this.message.importCiphertext(groupContext),
        this.shares.map { it.importShare(groupContext) }
    )
}

private fun electionguard.protogen.CiphertextDecryptionSelection.importShare(groupContext: GroupContext): DecryptionShare.CiphertextDecryptionSelection {
    if (this.share == null) {
        throw IllegalStateException("CiphertextDecryptionSelection share cant be null")
    }

    // either proof or recoverdParts is non null
    var proof: GenericChaumPedersenProof? = null
    var parts: Map<String, DecryptionShare.CiphertextCompensatedDecryptionSelection>? = null
    if (this.proof != null) {
        val pproof = this.proof ?: throw IllegalStateException("CiphertextDecryptionSelection.proof is null");
        proof = pproof.importChaumPedersenProof(groupContext)
    } else if (this.recoveredParts != null) { // will ignore if proof exists
        val recoveredParts = this.recoveredParts
            ?: throw IllegalStateException("CiphertextDecryptionSelection.recoveredParts is null");
        parts = recoveredParts.fragments.associate { it.guardianId to it.importRecoveredParts(groupContext) }
    } else {
        throw IllegalArgumentException("both CiphertextDecryptionSelection proof and recoveredParts are null")
    }
    return DecryptionShare.CiphertextDecryptionSelection(
        this.selectionId,
        this.guardianId,
        this.share.importElementModP(groupContext),
        proof,
        parts
    )
}

private fun electionguard.protogen.CiphertextCompensatedDecryptionSelection.importRecoveredParts(groupContext: GroupContext): DecryptionShare.CiphertextCompensatedDecryptionSelection {
    if (this.share == null) {
        throw IllegalStateException("CiphertextCompensatedDecryptionSelection share cant be null")
    }
    if (this.recoveryKey == null) {
        throw IllegalStateException("CiphertextCompensatedDecryptionSelection recoveryKey cant be null")
    }
    if (this.proof == null) {
        throw IllegalStateException("CiphertextCompensatedDecryptionSelection proof cant be null")
    }
    return DecryptionShare.CiphertextCompensatedDecryptionSelection(
        this.selectionId,
        this.guardianId,
        this.missingGuardianId,
        this.share.importElementModP(groupContext),
        this.recoveryKey.importElementModP(groupContext),
        this.proof.importChaumPedersenProof(groupContext),
    )
}

//////////////////////////////////////////////////////////////////////////////////////////////


fun PlaintextTally.publishPlaintextTally(): electionguard.protogen.PlaintextTally {
    return electionguard.protogen.PlaintextTally(
        this.tallyId,
        this.contests.values.map { it.publishContest() }
    )
}

private fun PlaintextTally.Contest.publishContest(): electionguard.protogen.PlaintextTallyContest {
    return electionguard.protogen.PlaintextTallyContest(
        this.contestId,
        this.selections.values.map { it.publishSelection() }
    )
}

private fun PlaintextTally.Selection.publishSelection(): electionguard.protogen.PlaintextTallySelection {
    return electionguard.protogen.PlaintextTallySelection(
        this.selectionId,
        this.tally,
        this.value.publishElementModP(),
        this.message.publishCiphertext(),
        this.shares.map { it.publishShare() }
    )
}

private fun DecryptionShare.CiphertextDecryptionSelection.publishShare(): electionguard.protogen.CiphertextDecryptionSelection {
    // either proof or recovered_parts is non null
    var proofOrParts: electionguard.protogen.CiphertextDecryptionSelection.ProofOrParts<*>? = null
    if (this.proof != null) {
        proofOrParts = electionguard.protogen.CiphertextDecryptionSelection.ProofOrParts.Proof(this.proof.publishChaumPedersenProof())
    } else if (this.recoveredParts != null) {
        val pparts = this.recoveredParts.map { it.value.publishRecoveredParts() }
        proofOrParts = electionguard.protogen.CiphertextDecryptionSelection.ProofOrParts.RecoveredParts(
            electionguard.protogen.RecoveredParts(pparts)
        )
    } else {
        throw IllegalStateException("CiphertextDecryptionSelection must have proof or recoveredParts")
    }
    return electionguard.protogen.CiphertextDecryptionSelection(
        this.selectionId,
        this.guardianId,
        this.share.publishElementModP(),
        proofOrParts,
    )
}

private fun DecryptionShare.CiphertextCompensatedDecryptionSelection.publishRecoveredParts(): electionguard.protogen.CiphertextCompensatedDecryptionSelection {
    return electionguard.protogen.CiphertextCompensatedDecryptionSelection(
        this.selectionId,
        this.guardianId,
        this.missingGuardianId,
        this.share.publishElementModP(),
        this.recoveryKey.publishElementModP(),
        this.proof.publishChaumPedersenProof(),
    )
}