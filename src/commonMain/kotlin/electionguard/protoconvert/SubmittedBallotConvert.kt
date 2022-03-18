package electionguard.protoconvert

import electionguard.ballot.SubmittedBallot
import electionguard.core.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("SubmittedBallotConvert")

fun electionguard.protogen.SubmittedBallot.importSubmittedBallot(
    groupContext: GroupContext
): SubmittedBallot? {

    val manifestHash = groupContext.importUInt256(this.manifestHash)
    val trackingHash = groupContext.importUInt256(this.code)
    val previousTrackingHash = groupContext.importUInt256(this.codeSeed)
    val cryptoHash = groupContext.importUInt256(this.cryptoHash)
    val ballotState = this.state.importBallotState()
    val contests = this.contests.map { it.importContest(groupContext) }.noNullValuesOrNull()

    // TODO: should we also check that the contests lists is non-empty?

    if (manifestHash == null || trackingHash == null || previousTrackingHash == null ||
        cryptoHash == null || ballotState == null || contests == null
    ) {
        logger.error { "Failed to convert submitted ballot, missing fields" }
        return null
    }

    return SubmittedBallot(
        this.ballotId,
        this.ballotStyleId,
        manifestHash,
        trackingHash,
        previousTrackingHash,
        contests,
        this.timestamp,
        cryptoHash,
        ballotState,
    )
}

private fun electionguard.protogen.SubmittedBallot.BallotState.importBallotState():
    SubmittedBallot.BallotState? {

        val name = this.name
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

private fun electionguard.protogen.CiphertextBallotContest.importContest(
    groupContext: GroupContext
): SubmittedBallot.Contest? {
    val contestHash = groupContext.importUInt256(this.contestHash)
    val ciphertextAccumulation = groupContext.importCiphertext(this.ciphertextAccumulation)
    val cryptoHash = groupContext.importUInt256(this.cryptoHash)
    val proof = this.proof?.let { this.proof.importConstantChaumPedersenProof(groupContext) }
    val selections = this.selections.map { it.importSelection(groupContext) }.noNullValuesOrNull()

    // TODO: should we also check if the selections is empty or the wrong length?

    if (contestHash == null || ciphertextAccumulation == null || cryptoHash == null ||
        proof == null || selections == null
    ) {
        logger.error { "Failed to convert contest, missing fields" }
        return null
    }

    return SubmittedBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        contestHash,
        selections,
        ciphertextAccumulation,
        cryptoHash,
        proof
    )
}

private fun electionguard.protogen.CiphertextBallotSelection.importSelection(
    groupContext: GroupContext
): SubmittedBallot.Selection? {

    val selectionHash = groupContext.importUInt256(this.selectionHash)
    val ciphertext = groupContext.importCiphertext(this.ciphertext)
    val cryptoHash = groupContext.importUInt256(this.cryptoHash)
    val proof = this.proof?.let { it.importDisjunctiveChaumPedersenProof(groupContext) }
    val extendedData = groupContext.importCiphertext(this.extendedData)

    if (selectionHash == null || ciphertext == null || cryptoHash == null || proof == null) {
        logger.error { "Failed to convert selection, missing fields" }
        return null
    }

    return SubmittedBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        selectionHash,
        ciphertext,
        cryptoHash,
        this.isPlaceholderSelection,
        proof,
        extendedData
    )
}

fun electionguard.protogen.ConstantChaumPedersenProof.importConstantChaumPedersenProof(
    groupContext: GroupContext
): ConstantChaumPedersenProofKnownNonce? {
    val pad = groupContext.importElementModP(this.pad)
    val data = groupContext.importElementModP(this.data)
    val challenge = groupContext.importElementModQ(this.challenge)
    val response = groupContext.importElementModQ(this.response)

    if (pad == null || data == null || challenge == null || response == null) {
        logger.error { "Failed to convert constant Chaum-Pedersen proof, missing fields" }
        return null
    }

    return ConstantChaumPedersenProofKnownNonce(
        GenericChaumPedersenProof(pad, data, challenge, response),
        this.constant
    )
}

fun electionguard.protogen.DisjunctiveChaumPedersenProof.importDisjunctiveChaumPedersenProof(
    groupContext: GroupContext
): DisjunctiveChaumPedersenProofKnownNonce? {

    val proofZeroPad = groupContext.importElementModP(this.proofZeroPad)
    val proofZeroData = groupContext.importElementModP(this.proofZeroData)
    val proofZeroChallenge = groupContext.importElementModQ(this.proofZeroChallenge)
    val proofZeroResponse = groupContext.importElementModQ(this.proofZeroResponse)

    val proofOnePad = groupContext.importElementModP(this.proofOnePad)
    val proofOneData = groupContext.importElementModP(this.proofOneData)
    val proofOneChallenge = groupContext.importElementModQ(this.proofOneChallenge)
    val proofOneResponse = groupContext.importElementModQ(this.proofOneResponse)

    val proofChallenge = groupContext.importElementModQ(this.challenge)

    if (proofZeroPad == null || proofZeroData == null || proofZeroChallenge == null ||
        proofZeroResponse == null || proofOnePad == null || proofOneData == null ||
        proofOneChallenge == null || proofOneResponse == null || proofChallenge == null
    ) {
        logger.error { "Failed to convert disjunctive Chaum-Pedersen proof, missing fields" }
        return null
    }

    return DisjunctiveChaumPedersenProofKnownNonce(
        GenericChaumPedersenProof(
            proofZeroPad,
            proofZeroData,
            proofZeroChallenge,
            proofZeroResponse,
        ),
        GenericChaumPedersenProof(proofOnePad, proofOneData, proofOneChallenge, proofOneResponse,),
        proofChallenge,
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun SubmittedBallot.publishSubmittedBallot(): electionguard.protogen.SubmittedBallot {
    return electionguard.protogen
        .SubmittedBallot(
            this.ballotId,
            this.ballotStyleId,
            this.manifestHash.publishUInt256(),
            this.code.publishUInt256(),
            this.codeSeed.publishUInt256(),
            this.contests.map { it.publishContest() },
            this.timestamp,
            this.cryptoHash.publishUInt256(),
            this.state.publishBallotState()
        )
}

private fun SubmittedBallot.BallotState.publishBallotState():
    electionguard.protogen.SubmittedBallot.BallotState {
        return electionguard.protogen.SubmittedBallot.BallotState.fromName(this.name)
    }

private fun SubmittedBallot.Contest.publishContest():
    electionguard.protogen.CiphertextBallotContest {
        return electionguard.protogen
            .CiphertextBallotContest(
                this.contestId,
                this.sequenceOrder,
                this.contestHash.publishUInt256(),
                this.selections.map { it.publishSelection() },
                this.ciphertextAccumulation.publishCiphertext(),
                this.cryptoHash.publishUInt256(),
                this.proof?.let { this.proof.publishConstantChaumPedersenProof() },
            )
    }

private fun SubmittedBallot.Selection.publishSelection():
    electionguard.protogen.CiphertextBallotSelection {
        return electionguard.protogen
            .CiphertextBallotSelection(
                this.selectionId,
                this.sequenceOrder,
                this.selectionHash.publishUInt256(),
                this.ciphertext.publishCiphertext(),
                this.cryptoHash.publishUInt256(),
                this.isPlaceholderSelection,
                this.proof?.let { this.proof.publishDisjunctiveChaumPedersenProof() },
                this.extendedData?.let { this.extendedData.publishCiphertext() },
            )
    }

fun ConstantChaumPedersenProofKnownNonce.publishConstantChaumPedersenProof():
    electionguard.protogen.ConstantChaumPedersenProof {
        return electionguard.protogen
            .ConstantChaumPedersenProof(
                this.proof.a.publishElementModP(),
                this.proof.b.publishElementModP(),
                this.proof.c.publishElementModQ(),
                this.proof.r.publishElementModQ(),
                this.constant
            )
    }

fun DisjunctiveChaumPedersenProofKnownNonce.publishDisjunctiveChaumPedersenProof():
    electionguard.protogen.DisjunctiveChaumPedersenProof {
        return electionguard.protogen
            .DisjunctiveChaumPedersenProof(
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