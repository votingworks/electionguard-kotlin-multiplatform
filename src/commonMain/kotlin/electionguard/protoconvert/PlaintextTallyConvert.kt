package electionguard.protoconvert

import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import mu.KotlinLogging
private val logger = KotlinLogging.logger("PlaintextTallyConvert")

data class PlaintextTallyConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.PlaintextTally?): PlaintextTally? {
        if (proto == null) {
            return null
        }

        // TODO: can it ever occur that we have zero contests?
        if (proto.contests.isEmpty()) {
            logger.error { "No contests in PlaintextTally" }
            return null
        }

        val contestMap = proto.contests.associate { it.contestId to convertContest(it) }.noNullValuesOrNull()

        if (contestMap == null) {
            logger.error { "Failed to convert PlaintextTally" }
            return null
        }

        return PlaintextTally(proto.tallyId, contestMap)
    }

    private fun convertContest(proto: electionguard.protogen.PlaintextTallyContest?): PlaintextTally.Contest? {
        if (proto == null) {
            return null
        }

        // TODO: can it ever occur that we have zero selections?
        if (proto.selections.isEmpty()) {
            logger.error { "No selections in PlaintextTallyContest" }
            return null
        }

        val selectionMap = proto.selections.associate { it.selectionId to convertSelection(it) }.noNullValuesOrNull()

        if (selectionMap == null) {
            logger.error { "Failed to convert PlaintextTallyContest" }
            return null
        }

        return PlaintextTally.Contest(proto.contestId, selectionMap)
    }

    private fun convertSelection(proto: electionguard.protogen.PlaintextTallySelection?): PlaintextTally.Selection? {
        if (proto == null) {
            return null
        }

        val value = convertElementModP(proto.value, groupContext)
        val message = convertCiphertext(proto.message, groupContext)
        val shares = proto.shares.map { convertShare(it) }.noNullValuesOrNull()

        if (value == null || message == null || shares == null) {
            logger.error { "Failed to convert PlaintextTallySelection" }
            return null
        }

        // TODO: can it ever occur that we have zero shares?
        if (shares.isEmpty()) {
            logger.error { "No shares in PlaintextTallySelection" }
            return null
        }

        return PlaintextTally.Selection(
            proto.selectionId,
            proto.tally,
            value,
            message,
            shares
        )
    }

    private fun convertShare(proto: electionguard.protogen.CiphertextDecryptionSelection?): DecryptionShare.CiphertextDecryptionSelection? {
        if (proto == null) {
            return null
        }

        val share = convertElementModP(proto.share, groupContext)
        val proof = convertChaumPedersenProof(proto.proof, groupContext)
        val recoveredParts = proto.recoveredParts
        val parts = if (recoveredParts != null) {
              recoveredParts.fragments.associate { it.guardianId to convertRecoveredParts(it) }.noNullValuesOrNull()
        } else {
            null
        }

        if (share == null) {
            logger.error { "Missing CiphertextDecryptionSelection share" }
            return null
        }

        when {
            proof == null && parts == null -> {
                logger.error { "Failed to convert CiphertextDecryptionSelection: no proof or parts present" }
                return null
            }
            proof != null && parts != null -> {
                logger.error { "Failed to convert CiphertextDecryptionSelection: both proof *and* parts present" }
                return null
            }
        }

        // If we get here, one of proof or parts is non-null, and we'll replace a
        // null parts value with an empty map.

        return DecryptionShare.CiphertextDecryptionSelection(
            proto.selectionId,
            proto.guardianId,
            share,
            proof,
            parts ?: emptyMap()
        )
    }

    private fun convertRecoveredParts(proto: electionguard.protogen.CiphertextCompensatedDecryptionSelection?): DecryptionShare.CiphertextCompensatedDecryptionSelection? {
        if (proto == null) {
            return null
        }

        val share = convertElementModP(proto.share, groupContext)
        val recoveryKey = convertElementModP(proto.recoveryKey, groupContext)
        val proof = convertChaumPedersenProof(proto.proof, groupContext)

        if (share == null || recoveryKey == null || proof == null) {
            logger.error { "Failed to convert CiphertextCompensatedDecryptionSelection" }
            return null
        }

        return DecryptionShare.CiphertextCompensatedDecryptionSelection(
            proto.selectionId,
            proto.guardianId,
            proto.missingGuardianId,
            share,
            recoveryKey,
            proof
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