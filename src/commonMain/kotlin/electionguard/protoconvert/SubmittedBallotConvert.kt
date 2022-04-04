package electionguard.protoconvert

import electionguard.ballot.SubmittedBallot
import electionguard.core.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger("SubmittedBallotConvert")

fun electionguard.protogen.SubmittedBallot.importSubmittedBallot(
    groupContext: GroupContext
): SubmittedBallot? {

    val manifestHash = importUInt256(this.manifestHash)
    val trackingHash = importUInt256(this.code)
    val previousTrackingHash = importUInt256(this.codeSeed)
    val cryptoHash = importUInt256(this.cryptoHash)
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
    val contestHash = importUInt256(this.contestHash)
    val ciphertextAccumulation = groupContext.importCiphertext(this.ciphertextAccumulation)
    val cryptoHash = importUInt256(this.cryptoHash)
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

    val selectionHash = importUInt256(this.selectionHash)
    val ciphertext = groupContext.importCiphertext(this.ciphertext)
    val cryptoHash = importUInt256(this.cryptoHash)
    val proof = this.proof?.importDisjunctiveChaumPedersenProof(groupContext)
    val extendedData = groupContext.importHashedCiphertext(this.extendedData)

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
    var proof = groupContext.importChaumPedersenProof(this.proof)

    if (proof == null) {
        // 1.0
        val challenge = groupContext.importElementModQ(this.challenge)
        val response = groupContext.importElementModQ(this.response)

        if (challenge == null || response == null) {
            logger.error { "Failed to convert constant Chaum-Pedersen 1.0 proof, missing fields" }
            return null
        }
        proof = GenericChaumPedersenProof(challenge, response)
    }

    return ConstantChaumPedersenProofKnownNonce(proof, this.constant)
}

fun electionguard.protogen.DisjunctiveChaumPedersenProof.importDisjunctiveChaumPedersenProof(
    groupContext: GroupContext
): DisjunctiveChaumPedersenProofKnownNonce? {
    var proof0 = groupContext.importChaumPedersenProof(this.proof0)
    var proof1 = groupContext.importChaumPedersenProof(this.proof1)
    val proofChallenge = groupContext.importElementModQ(this.challenge)

    if (proof0 == null && proof1 == null) {
        // 1.0 election record
        val proofZeroPad = groupContext.importElementModP(this.proofZeroPad)
        val proofZeroData = groupContext.importElementModP(this.proofZeroData)
        val proofZeroChallenge = groupContext.importElementModQ(this.proofZeroChallenge)
        val proofZeroResponse = groupContext.importElementModQ(this.proofZeroResponse)

        val proofOnePad = groupContext.importElementModP(this.proofOnePad)
        val proofOneData = groupContext.importElementModP(this.proofOneData)
        val proofOneChallenge = groupContext.importElementModQ(this.proofOneChallenge)
        val proofOneResponse = groupContext.importElementModQ(this.proofOneResponse)

        if (proofZeroPad == null || proofZeroData == null || proofZeroChallenge == null ||
            proofZeroResponse == null || proofOnePad == null || proofOneData == null ||
            proofOneChallenge == null || proofOneResponse == null || proofChallenge == null
        ) {
            logger.error {
                "Failed to convert disjunctive Chaum-Pedersen proof, missing 1.0 fields"
            }
            return null
        }

        proof0 = GenericChaumPedersenProof(proofZeroChallenge, proofZeroResponse)
        proof1 = GenericChaumPedersenProof(proofOneChallenge, proofOneResponse)
    }

    if (proof0 == null || proof1 == null || proofChallenge == null) {
        logger.error { "Failed to convert disjunctive Chaum-Pedersen proof, missing fields" }
        return null
    }

    return DisjunctiveChaumPedersenProofKnownNonce(proof0, proof1, proofChallenge,)
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
                this.extendedData?.let { this.extendedData.publishHashedCiphertext() },
            )
    }

fun ConstantChaumPedersenProofKnownNonce.publishConstantChaumPedersenProof():
    electionguard.protogen.ConstantChaumPedersenProof {
        return electionguard.protogen
            .ConstantChaumPedersenProof(
                null,
                null,
                null,
                null, // 1.0 0nly
                this.constant,
                this.proof.publishChaumPedersenProof(),
            )
    }

fun DisjunctiveChaumPedersenProofKnownNonce.publishDisjunctiveChaumPedersenProof():
    electionguard.protogen.DisjunctiveChaumPedersenProof {
        return electionguard.protogen
            .DisjunctiveChaumPedersenProof(
                null,
                null,
                null,
                null, // 1.0 0nly
                null,
                null,
                null,
                null, // 1.0 0nly
                this.c.publishElementModQ(),
                this.proof0.publishChaumPedersenProof(),
                this.proof1.publishChaumPedersenProof(),
            )
    }