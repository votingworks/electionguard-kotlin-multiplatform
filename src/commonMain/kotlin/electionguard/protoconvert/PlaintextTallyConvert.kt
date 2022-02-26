package electionguard.protoconvert

import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext

data class PlaintextTallyConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.PlaintextTally): PlaintextTally {
        return PlaintextTally(
            proto.tallyId,
            proto.contests.associate{ it.contestId to convertContest(it) }
            )
    }

    private fun convertContest(proto: electionguard.protogen.PlaintextTallyContest): PlaintextTally.Contest {
        return PlaintextTally.Contest(
            proto.contestId,
            proto.selections.associate{ it.selectionId to convertSelection(it) }
        )
    }

    private fun convertSelection(proto: electionguard.protogen.PlaintextTallySelection): PlaintextTally.Selection {
        return PlaintextTally.Selection(
            proto.selectionId,
            proto.tally,
            convertElementModP(proto.value?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertCiphertext(proto.message?: throw IllegalArgumentException("Selection message cannot be null"), groupContext),
            proto.shares.map{ convertShare(it) }
        )
    }

    private fun convertShare(proto: electionguard.protogen.CiphertextDecryptionSelection): DecryptionShare.CiphertextDecryptionSelection {
        // either proof or recoverdParts is non null
        var proof : GenericChaumPedersenProof? = null
        var parts : Map<String, DecryptionShare.CiphertextCompensatedDecryptionSelection>? = emptyMap()
        if (proto.proof != null) {
            val pproof = proto.proof?: throw IllegalStateException("CiphertextDecryptionSelection.proof is null");
            proof = convertChaumPedersenProof(pproof, groupContext)
        } else if (proto.recoveredParts != null) {
            val recoveredParts = proto.recoveredParts?: throw IllegalStateException("CiphertextDecryptionSelection.recoveredParts is null");
            parts = recoveredParts.fragments.associate { it.guardianId to convertRecoveredParts(it) }
        } else {
            throw IllegalArgumentException("both CiphertextDecryptionSelection proof and recoveredParts are null")
        }
        return DecryptionShare.CiphertextDecryptionSelection(
            proto.selectionId,
            proto.guardianId,
            convertElementModP(proto.share?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proof,
            parts
        )
    }

    private fun convertRecoveredParts(proto: electionguard.protogen.CiphertextCompensatedDecryptionSelection): DecryptionShare.CiphertextCompensatedDecryptionSelection {
        return DecryptionShare.CiphertextCompensatedDecryptionSelection(
            proto.selectionId,
            proto.guardianId,
            proto.missingGuardianId,
            convertElementModP(proto.share?: throw IllegalArgumentException("DecryptionShare part cannot be null"), groupContext),
            convertElementModP(proto.recoveryKey?: throw IllegalArgumentException("DecryptionShare recoveryKey cannot be null"), groupContext),
            convertChaumPedersenProof(proto.proof?: throw IllegalArgumentException("DecryptionShare recoveryKey cannot be null"), groupContext),
        )
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(tally: PlaintextTally): electionguard.protogen.PlaintextTally {
        return electionguard.protogen.PlaintextTally(
            tally.tallyId,
            tally.contests.values.map{ convertContest(it) }
        )
    }

    private fun convertContest(contest: PlaintextTally.Contest): electionguard.protogen.PlaintextTallyContest {
        return electionguard.protogen.PlaintextTallyContest(
                contest.contestId,
                contest.selections.values.map{ convertSelection(it) }
        )
    }

    private fun convertSelection(selection: PlaintextTally.Selection): electionguard.protogen.PlaintextTallySelection {
        return electionguard.protogen.PlaintextTallySelection(
                selection.selectionId,
                selection.tally,
                convertElementModP(selection.value),
                convertCiphertext(selection.message),
                selection.shares.map{ convertShare(it) }
        )
    }

    private fun convertShare(share: DecryptionShare.CiphertextDecryptionSelection):  electionguard.protogen.CiphertextDecryptionSelection {
        // either proof or recovered_parts is non null / non empty
        var proofOrParts : electionguard.protogen.CiphertextDecryptionSelection.ProofOrParts<*>? = null
        if (share.proof != null) {
            proofOrParts = electionguard.protogen.CiphertextDecryptionSelection.ProofOrParts.Proof(convertChaumPedersenProof(share.proof))
        } else if (share.recoveredParts != null) {
            val pparts : List<electionguard.protogen.CiphertextCompensatedDecryptionSelection>
            pparts = share.recoveredParts.map { convertRecoveredParts(it.value) }
            proofOrParts =electionguard.protogen. CiphertextDecryptionSelection.ProofOrParts.RecoveredParts(electionguard.protogen.RecoveredParts(pparts))
        }
        return electionguard.protogen.CiphertextDecryptionSelection(
            share.selectionId,
            share.guardianId,
            convertElementModP(share.share),
            proofOrParts,
        )
    }

    private fun convertRecoveredParts(part: DecryptionShare.CiphertextCompensatedDecryptionSelection):  electionguard.protogen.CiphertextCompensatedDecryptionSelection {
        return electionguard.protogen.CiphertextCompensatedDecryptionSelection(
                part.guardianId,
                part.selectionId,
                part.missingGuardianId,
                convertElementModP(part.share),
                convertElementModP(part.recoveryKey),
                convertChaumPedersenProof(part.proof),
        )
    }
}