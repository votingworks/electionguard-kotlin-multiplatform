package electionguard.protoconvert

import electionguard.ballot.SubmittedBallot
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext

fun electionguard.protogen.SubmittedBallot.importSubmittedBallot(groupContext: GroupContext): SubmittedBallot {
    if (this.manifestHash == null) {
        throw IllegalStateException("manifestHash cant be null")
    }
    if (this.trackingHash == null) {
        throw IllegalStateException("trackingHash cant be null")
    }
    if (this.previousTrackingHash == null) {
        throw IllegalStateException("previousTrackingHash cant be null")
    }
    if (this.cryptoHash == null) {
        throw IllegalStateException("cryptoHash cant be null")
    }
    return SubmittedBallot(
        this.ballotId,
        this.ballotStyleId,
        this.manifestHash.importElementModQ(groupContext),
        this.trackingHash.importElementModQ(groupContext),
        this.previousTrackingHash.importElementModQ(groupContext),
        this.contests.map { it.importContest(groupContext) },
        this.timestamp,
        this.cryptoHash.importElementModQ(groupContext),
        this.state.importBallotState(),
    )
}

private fun electionguard.protogen.SubmittedBallot.BallotState.importBallotState(): SubmittedBallot.BallotState {
    return SubmittedBallot.BallotState.valueOf(
        this.name ?: throw IllegalArgumentException("BallotState cannot be null")
    )
}

private fun electionguard.protogen.CiphertextBallotContest.importContest(groupContext: GroupContext): SubmittedBallot.Contest {
    if (this.contestHash == null) {
        throw IllegalStateException("manifestHash cant be null")
    }
    if (this.ciphertextAccumulation == null) {
        throw IllegalStateException("ciphertextAccumulation cant be null")
    }
    if (this.cryptoHash == null) {
        throw IllegalStateException("cryptoHash cant be null")
    }
    if (this.proof == null) {
        throw IllegalStateException("proof cant be null")
    }
    return SubmittedBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.contestHash.importElementModQ(groupContext),
        this.selections.map { it.importSelection(groupContext) },
        this.ciphertextAccumulation.importCiphertext(groupContext),
        this.cryptoHash.importElementModQ(groupContext),
        this.proof.importConstantChaumPedersenProof(groupContext),
    )
}

private fun electionguard.protogen.CiphertextBallotSelection.importSelection(groupContext: GroupContext): SubmittedBallot.Selection {
    if (this.selectionHash == null) {
        throw IllegalStateException("selectionHash cant be null")
    }
    if (this.ciphertext == null) {
        throw IllegalStateException("ciphertext cant be null")
    }
    if (this.cryptoHash == null) {
        throw IllegalStateException("cryptoHash cant be null")
    }
    if (this.proof == null) {
        throw IllegalStateException("proof cant be null")
    }
    return SubmittedBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionHash.importElementModQ(groupContext),
        this.ciphertext.importCiphertext(groupContext),
        this.cryptoHash.importElementModQ(groupContext),
        this.isPlaceholderSelection,
        this.proof.importDisjunctiveChaumPedersenProof(groupContext),
        this.extendedData?.let { this.extendedData.importCiphertext(groupContext) },
    )
}

fun electionguard.protogen.ConstantChaumPedersenProof.importConstantChaumPedersenProof(groupContext: GroupContext): ConstantChaumPedersenProofKnownNonce {
    if (this.pad == null) {
        throw IllegalStateException("pad cant be null")
    }
    if (this.data == null) {
        throw IllegalStateException("data cant be null")
    }
    if (this.challenge == null) {
        throw IllegalStateException("challenge cant be null")
    }
    if (this.response == null) {
        throw IllegalStateException("response cant be null")
    }
    return ConstantChaumPedersenProofKnownNonce(
        GenericChaumPedersenProof(
            this.pad.importElementModP(groupContext),
            this.data.importElementModP(groupContext),
            this.challenge.importElementModQ(groupContext),
            this.response.importElementModQ(groupContext),
        ),
        this.constant
    )
}

fun electionguard.protogen.DisjunctiveChaumPedersenProof.importDisjunctiveChaumPedersenProof(groupContext: GroupContext): DisjunctiveChaumPedersenProofKnownNonce {
    if (this.proofZeroPad == null) {
        throw IllegalStateException("proofZeroPad cant be null")
    }
    if (this.proofZeroData == null) {
        throw IllegalStateException("proofZeroData cant be null")
    }
    if (this.proofZeroChallenge == null) {
        throw IllegalStateException("proofZeroChallenge cant be null")
    }
    if (this.proofZeroResponse == null) {
        throw IllegalStateException("proofZeroResponse cant be null")
    }
    if (this.proofOnePad == null) {
        throw IllegalStateException("proofOnePad cant be null")
    }
    if (this.proofOneData == null) {
        throw IllegalStateException("proofOneData cant be null")
    }
    if (this.proofOneChallenge == null) {
        throw IllegalStateException("proofOneChallenge cant be null")
    }
    if (this.proofOneResponse == null) {
        throw IllegalStateException("proofOneResponse cant be null")
    }
    if (this.challenge == null) {
        throw IllegalStateException("challenge cant be null")
    }

    return DisjunctiveChaumPedersenProofKnownNonce(
        GenericChaumPedersenProof(
            this.proofZeroPad.importElementModP(groupContext),
            this.proofZeroData.importElementModP(groupContext),
            this.proofZeroChallenge.importElementModQ(groupContext),
            this.proofZeroResponse.importElementModQ(groupContext),
        ),
        GenericChaumPedersenProof(
            this.proofOnePad.importElementModP(groupContext),
            this.proofOneData.importElementModP(groupContext),
            this.proofOneChallenge.importElementModQ(groupContext),
            this.proofOneResponse.importElementModQ(groupContext),
        ),
        this.challenge.importElementModQ(groupContext),
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun SubmittedBallot.publishSubmittedBallot(): electionguard.protogen.SubmittedBallot {
    return electionguard.protogen.SubmittedBallot(
        this.ballotId,
        this.ballotStyleId,
        this.manifestHash.publishElementModQ(),
        this.trackingHash.publishElementModQ(),
        this.previousTrackingHash.publishElementModQ(),
        this.contests.map { it.publishContest() },
        this.timestamp,
        this.cryptoHash.publishElementModQ(),
        this.state.publishBallotState()
    )
}

private fun SubmittedBallot.BallotState.publishBallotState(): electionguard.protogen.SubmittedBallot.BallotState {
    return electionguard.protogen.SubmittedBallot.BallotState.fromName(this.name)
}

private fun SubmittedBallot.Contest.publishContest(): electionguard.protogen.CiphertextBallotContest {
    return electionguard.protogen.CiphertextBallotContest(
        this.contestId,
        this.sequenceOrder,
        this.contestHash.publishElementModQ(),
        this.selections.map { it.publishSelection() },
        this.ciphertextAccumulation.publishCiphertext(),
        this.cryptoHash.publishElementModQ(),
        this.proof?.let { this.proof.publishConstantChaumPedersenProof() },
    )
}

private fun SubmittedBallot.Selection.publishSelection(): electionguard.protogen.CiphertextBallotSelection {
    return electionguard.protogen.CiphertextBallotSelection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionHash.publishElementModQ(),
        this.ciphertext.publishCiphertext(),
        this.cryptoHash.publishElementModQ(),
        this.isPlaceholderSelection,
        this.proof?.let { this.proof.publishDisjunctiveChaumPedersenProof() },
        this.extendedData?.let { this.extendedData.publishCiphertext() },
    )
}

fun ConstantChaumPedersenProofKnownNonce.publishConstantChaumPedersenProof(): electionguard.protogen.ConstantChaumPedersenProof {
    return electionguard.protogen.ConstantChaumPedersenProof(
        this.proof.a.publishElementModP(),
        this.proof.b.publishElementModP(),
        this.proof.c.publishElementModQ(),
        this.proof.r.publishElementModQ(),
        this.constant
    )
}

fun DisjunctiveChaumPedersenProofKnownNonce.publishDisjunctiveChaumPedersenProof(): electionguard.protogen.DisjunctiveChaumPedersenProof {
    return electionguard.protogen.DisjunctiveChaumPedersenProof(
        this.proof0.a.publishElementModP(),
        this.proof0.b.publishElementModP(),
        this.proof0.c.publishElementModQ(),
        this.proof0.r.publishElementModQ(),
        this.proof1.a.publishElementModP(),
        this.proof1.b.publishElementModP(),
        this.proof1.c.publishElementModQ(),
        this.proof1.r.publishElementModQ(),
        this.c.publishElementModQ(),
    )
}