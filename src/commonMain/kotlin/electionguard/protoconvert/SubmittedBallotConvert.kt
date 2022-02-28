package electionguard.protoconvert

import electionguard.ballot.SubmittedBallot
import electionguard.core.*
import mu.KotlinLogging
private val logger = KotlinLogging.logger("SubmittedBallotConvert")

data class SubmittedBallotConvert(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.SubmittedBallot?): SubmittedBallot? {
        if (proto == null) {
            return null
        }

        val manifestHash = convertElementModQ(proto.manifestHash, groupContext)
        val trackingHash = convertElementModQ(proto.trackingHash, groupContext)
        val previousTrackingHash = convertElementModQ(proto.previousTrackingHash, groupContext)
        val cryptoHash = convertElementModQ(proto.cryptoHash, groupContext)
        val ballotState = convertBallotState(proto.state)
        val contests = proto.contests.map{ convertContest(it) }.noNullValuesOrNull()

        // TODO: should we also check that the contests lists is non-empty?

        if (manifestHash == null || trackingHash == null || previousTrackingHash == null || cryptoHash == null || ballotState == null || contests == null) {
            logger.error { "Failed to convert submitted ballot, missing fields" }
            return null
        }

        return SubmittedBallot(
            proto.ballotId,
            proto.ballotStyleId,
            manifestHash,
            trackingHash,
            previousTrackingHash,
            contests,
            proto.timestamp,
            cryptoHash,
            ballotState,
        )
    }

    private fun convertBallotState(proto: electionguard.protogen.SubmittedBallot.BallotState?): SubmittedBallot.BallotState? {
        if (proto == null) {
            return null
        }

        val name = proto.name
        if (name == null) {
            logger.error { "Failed to convert ballot state, missing name" }
            return null
        }

        try {
            return SubmittedBallot.BallotState.valueOf(name)
        } catch (e: IllegalArgumentException) {
            logger.error { "Failed to convert ballot state, unknown name: $name" }
            return null
        }
    }

    private fun convertContest(proto: electionguard.protogen.CiphertextBallotContest?): SubmittedBallot.Contest? {
        if (proto == null) {
            return null
        }

        val contestHash = convertElementModQ(proto.contestHash, groupContext)
        val ciphertextAccumulation = convertCiphertext(proto.ciphertextAccumulation, groupContext)
        val cryptoHash = convertElementModQ(proto.cryptoHash, groupContext)
        val proof = convertConstantChaumPedersenProof(proto.proof)
        val selections = proto.selections.map { convertSelection(it) }.noNullValuesOrNull()

        // TODO: should we also check if the selections is empty or the wrong length?

        if (contestHash == null || ciphertextAccumulation == null || cryptoHash == null || proof == null || selections == null) {
            logger.error { "Failed to convert contest, missing fields" }
            return null
        }

        return SubmittedBallot.Contest(
            proto.contestId,
            proto.sequenceOrder,
            contestHash,
            selections,
            ciphertextAccumulation,
            cryptoHash,
            proof
        )
    }

    private fun convertSelection(proto: electionguard.protogen.CiphertextBallotSelection?): SubmittedBallot.Selection? {
        if (proto == null) {
            return null
        }

        val selectionHash = convertElementModQ(proto.selectionHash, groupContext)
        val ciphertext = convertCiphertext(proto.ciphertext, groupContext)
        val cryptoHash = convertElementModQ(proto.cryptoHash, groupContext)
        val proof = convertDisjunctiveChaumPedersenProof(proto.proof)
        val extendedData = convertCiphertext(proto.extendedData, groupContext)

        if (selectionHash == null || ciphertext == null || cryptoHash == null || proof == null || extendedData == null) {
            logger.error { "Failed to convert selection, missing fields" }
            return null
        }

        return SubmittedBallot.Selection(
            proto.selectionId,
            proto.sequenceOrder,
            selectionHash,
            ciphertext,
            cryptoHash,
            proto.isPlaceholderSelection,
            proof,
            extendedData
        )
    }

    fun convertConstantChaumPedersenProof(proof: electionguard.protogen.ConstantChaumPedersenProof?): ConstantChaumPedersenProofKnownNonce? {
        if (proof == null) {
            return null
        }
        val pad = convertElementModP(proof.pad, groupContext)
        val data = convertElementModP(proof.data, groupContext)
        val challenge = convertElementModQ(proof.challenge, groupContext)
        val response = convertElementModQ(proof.response, groupContext)

        if (pad == null || data == null || challenge == null || response == null) {
            logger.error { "Failed to convert constant Chaum-Pedersen proof, missing fields" }
            return null
        }

        return ConstantChaumPedersenProofKnownNonce(
            GenericChaumPedersenProof(pad, data, challenge, response),
            proof.constant
        )
    }

    fun convertDisjunctiveChaumPedersenProof(proof: electionguard.protogen.DisjunctiveChaumPedersenProof?): DisjunctiveChaumPedersenProofKnownNonce? {
        if (proof == null) {
            return null
        }

        val proofZeroPad = convertElementModP(proof.proofZeroPad, groupContext)
        val proofZeroData = convertElementModP(proof.proofZeroData, groupContext)
        val proofZeroChallenge = convertElementModQ(proof.proofZeroChallenge, groupContext)
        val proofZeroResponse = convertElementModQ(proof.proofZeroResponse, groupContext)

        val proofOnePad = convertElementModP(proof.proofOnePad, groupContext)
        val proofOneData = convertElementModP(proof.proofOneData, groupContext)
        val proofOneChallenge = convertElementModQ(proof.proofOneChallenge, groupContext)
        val proofOneResponse = convertElementModQ(proof.proofOneResponse, groupContext)

        val proofChallenge = convertElementModQ(proof.challenge, groupContext)

        if (proofZeroPad == null || proofZeroData == null || proofZeroChallenge == null || proofZeroResponse == null ||
            proofOnePad == null || proofOneData == null || proofOneChallenge == null || proofOneResponse == null ||
            proofChallenge == null) {
            logger.error { "Failed to convert disjunctive Chaum-Pedersen proof, missing fields" }
            return null
        }


        return DisjunctiveChaumPedersenProofKnownNonce(
            GenericChaumPedersenProof(proofZeroPad, proofZeroData, proofZeroChallenge, proofZeroResponse),
            GenericChaumPedersenProof(proofOnePad, proofOneData, proofOneChallenge, proofOneResponse),
            proofChallenge
        )
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////

    fun translateToProto(ballot: SubmittedBallot): electionguard.protogen.SubmittedBallot {
        return electionguard.protogen.SubmittedBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            convertElementModQ(ballot.manifestHash),
            convertElementModQ(ballot.trackingHash),
            convertElementModQ(ballot.previousTrackingHash),
            ballot.contests.map{ convertContest(it) },
            ballot.timestamp,
            convertElementModQ(ballot.cryptoHash),
            convertBallotState(ballot.state)
        )
    }

    private fun convertBallotState(type: SubmittedBallot.BallotState ): electionguard.protogen.SubmittedBallot.BallotState{
        return electionguard.protogen.SubmittedBallot.BallotState.fromName(type.name)
    }

    private fun convertContest(contest: SubmittedBallot.Contest): electionguard.protogen.CiphertextBallotContest {
        return electionguard.protogen.CiphertextBallotContest(
                contest.contestId,
                contest.sequenceOrder,
            convertElementModQ(contest.contestHash),
            contest.selections.map{ convertSelection(it) },
            convertCiphertext(contest.ciphertextAccumulation),
            convertElementModQ(contest.cryptoHash),
            if (contest.proof == null) { null } else { convertConstantChaumPedersenProof(contest.proof) },
        )
    }

    private fun convertSelection(selection: SubmittedBallot.Selection): electionguard.protogen.CiphertextBallotSelection {
        return electionguard.protogen.CiphertextBallotSelection(
                selection.selectionId,
                selection.sequenceOrder,
                convertElementModQ(selection.selectionHash),
                convertCiphertext(selection.ciphertext),
            convertElementModQ(selection.cryptoHash),
            selection.isPlaceholderSelection,
            if (selection.proof == null) { null } else { convertDisjunctiveChaumPedersenProof(selection.proof) },
            if (selection.extendedData == null) { null } else { convertCiphertext(selection.extendedData) },
            )
    }

    fun convertConstantChaumPedersenProof(proof: ConstantChaumPedersenProofKnownNonce):  electionguard.protogen.ConstantChaumPedersenProof {
        return electionguard.protogen.ConstantChaumPedersenProof(
                convertElementModP(proof.proof.a),
                convertElementModP(proof.proof.b),
                convertElementModQ(proof.proof.c),
                convertElementModQ(proof.proof.r),
            proof.constant
        )
    }

    fun convertDisjunctiveChaumPedersenProof(proof: DisjunctiveChaumPedersenProofKnownNonce): electionguard.protogen.DisjunctiveChaumPedersenProof {
        return electionguard.protogen.DisjunctiveChaumPedersenProof(
            convertElementModP(proof.proof0.a),
            convertElementModP(proof.proof0.b),
            convertElementModQ(proof.proof0.c),
            convertElementModQ(proof.proof0.r),
            convertElementModP(proof.proof1.a),
            convertElementModP(proof.proof1.b),
            convertElementModQ(proof.proof1.c),
            convertElementModQ(proof.proof1.r),
            convertElementModQ(proof.c),
        )
    }
}