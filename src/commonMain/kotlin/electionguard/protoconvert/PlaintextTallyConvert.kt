package electionguard.protoconvert

import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.protogen.PlaintextTallyContest
import electionguard.protogen.PlaintextTallySelection

data class PlaintextTallyConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.PlaintextTally): PlaintextTally {
        return PlaintextTally(
            proto.objectId,
            proto.contests.map{ (key, value) -> key to convertContest(value ?:
                    throw IllegalArgumentException("PlaintextTallyContest cannot be null")) }
                .toMap()
            )
    }

    private fun convertContest(proto: PlaintextTallyContest): PlaintextTally.Contest {
        return PlaintextTally.Contest(
            proto.objectId,
            proto.selections.map{ (key, value) -> key to convertSelection(value ?:
                    throw IllegalArgumentException("PlaintextTallySelection cannot be null")) }
                .toMap()
        )
    }

    private fun convertSelection(proto: PlaintextTallySelection): PlaintextTally.Selection {
        return PlaintextTally.Selection(
            proto.objectId,
            proto.tally,
            convertElementModP(proto.value?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            convertCiphertext(proto.message?: throw IllegalArgumentException("Selection message cannot be null"), groupContext),
            proto.shares.map{ convertShare(it) }
        )
    }

    private fun convertShare(proto: electionguard.protogen.CiphertextDecryptionSelection): DecryptionShare.CiphertextDecryptionSelection {
        // either proof or recoverdParts is non null / non empty
        var proof : GenericChaumPedersenProof? = null
        if (proto.proof != null) {
            proof = convertChaumPedersenProof(proto.proof, groupContext)
        }
        var parts : Map<String, DecryptionShare.CiphertextCompensatedDecryptionSelection>? = emptyMap()
        if (proto.recoveredParts.isNotEmpty()) {
            parts = proto.recoveredParts.associate { it.key to convertRecoveredParts(it) }
        }
        return DecryptionShare.CiphertextDecryptionSelection(
            proto.objectId,
            proto.guardianId,
            convertElementModP(proto.share?: throw IllegalArgumentException("Selection value cannot be null"), groupContext),
            proof,
            parts
        )
    }

    private fun convertRecoveredParts(proto: electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry): DecryptionShare.CiphertextCompensatedDecryptionSelection {
        val part : electionguard.protogen.CiphertextCompensatedDecryptionSelection = proto.value?: throw IllegalArgumentException("DecryptionShare cannot be null")
        return DecryptionShare.CiphertextCompensatedDecryptionSelection(
            part.objectId,
            part.guardianId,
            part.missingGuardianId,
            convertElementModP(part.share?: throw IllegalArgumentException("DecryptionShare part cannot be null"), groupContext),
            convertElementModP(part.recoveryKey?: throw IllegalArgumentException("DecryptionShare recoveryKey cannot be null"), groupContext),
            convertChaumPedersenProof(part.proof?: throw IllegalArgumentException("DecryptionShare recoveryKey cannot be null"), groupContext),
        )
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(tally: PlaintextTally): electionguard.protogen.PlaintextTally {
        return electionguard.protogen.PlaintextTally(
            tally.objectId,
            tally.contests.values.map{ convertContest(it) }
        )
    }

    private fun convertContest(contest: PlaintextTally.Contest): electionguard.protogen.PlaintextTally.ContestsEntry {
        return electionguard.protogen.PlaintextTally.ContestsEntry(
            contest.contestId,
                PlaintextTallyContest(
                contest.contestId,
                contest.ballotSelections.values.map{ convertSelection(it) }
            )
        )
    }

    private fun convertSelection(selection: PlaintextTally.Selection): PlaintextTallyContest.SelectionsEntry {
        return PlaintextTallyContest.SelectionsEntry(
            selection.selectionId,
            PlaintextTallySelection(
                selection.selectionId,
                selection.tally,
                convertElementModP(selection.value),
                convertCiphertext(selection.message),
                selection.shares.map{ convertShare(it) }
            )
        )
    }

    private fun convertShare(share: DecryptionShare.CiphertextDecryptionSelection):  electionguard.protogen.CiphertextDecryptionSelection {
        // either proof or recovered_parts is non null / non empty
        var pproof : electionguard.protogen.ChaumPedersenProof? = null
        if (share.proof != null) {
            pproof = convertChaumPedersenProof(share.proof)
        }
        var pparts : List<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry> = emptyList()
        if (share.recoveredParts != null && share.recoveredParts.isNotEmpty()) {
            pparts = share.recoveredParts.map{ convertRecoveredParts(it.value)}
        }
        return electionguard.protogen.CiphertextDecryptionSelection(
            share.objectId,
            share.guardianId,
            convertElementModP(share.share),
            pproof,
            pparts
        )
    }

    private fun convertRecoveredParts(part: DecryptionShare.CiphertextCompensatedDecryptionSelection):  electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry{
        return electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry(
            part.missingGuardianId,
            electionguard.protogen.CiphertextCompensatedDecryptionSelection(
                part.objectId,
                part.guardianId,
                part.missingGuardianId,
                convertElementModP(part.share),
                convertElementModP(part.recoveryKey),
                convertChaumPedersenProof(part.proof),
            )
        )
    }
}