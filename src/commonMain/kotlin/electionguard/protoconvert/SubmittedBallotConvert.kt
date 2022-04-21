package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.SubmittedBallot
import electionguard.core.*

fun GroupContext.importSubmittedBallot(
    ballot: electionguard.protogen.SubmittedBallot
): Result<SubmittedBallot, String> {

    val manifestHash = importUInt256(ballot.manifestHash)
    val trackingHash = importUInt256(ballot.code)
    val previousTrackingHash = importUInt256(ballot.codeSeed)
    val cryptoHash = importUInt256(ballot.cryptoHash)
    val ballotState = ballot.state.importBallotState(ballot.ballotId)
    if (ballotState is Err) return ballotState

    val (contests, errors) = ballot.contests.map { this.importContest(it, ballot.ballotId) }.partition()
    if (!errors.isEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    if (manifestHash == null || trackingHash == null || previousTrackingHash == null || cryptoHash == null) {
        return Err("Missing fields in ballot ${ballot.ballotId}")
    }

    return Ok(
        SubmittedBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            manifestHash,
            trackingHash,
            previousTrackingHash,
            contests,
            ballot.timestamp,
            cryptoHash,
            ballotState.unwrap(),
        )
    )
}

private fun electionguard.protogen.SubmittedBallot.BallotState.importBallotState(where: String):
        Result<SubmittedBallot.BallotState, String> {

    val name = this.name
    if (name == null) {
        return Err("Failed to convert ballot state, missing name in $where\"")
    }

    try {
        return Ok(SubmittedBallot.BallotState.valueOf(name))
    } catch (e: IllegalArgumentException) {
        return Err("Failed to convert ballot state, unknown name $name in $where\"")
    }
}

private fun GroupContext.importContest(
    contest: electionguard.protogen.CiphertextBallotContest, where: String,
): Result<SubmittedBallot.Contest, String> {
    val here = "$where ${contest.contestId}"

    val contestHash = importUInt256(contest.contestHash)
    val ciphertextAccumulation = this.importCiphertext(contest.ciphertextAccumulation)
    val cryptoHash = importUInt256(contest.cryptoHash)
    val proof = this.importConstantChaumPedersenProof(contest.proof, here)

    val (selections, errors) = contest.selections.map { this.importSelection(it, here) }.partition()
    if (!errors.isEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    if (proof is Err) return proof

    if (contestHash == null || ciphertextAccumulation == null || cryptoHash == null) {
        return Err("Missing fields in contest $here")
    }

    return Ok(
        SubmittedBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            contestHash,
            selections,
            ciphertextAccumulation,
            cryptoHash,
            proof.unwrap(),
        )
    )
}

private fun GroupContext.importConstantChaumPedersenProof(
    constant: electionguard.protogen.ConstantChaumPedersenProof?, where: String
): Result<ConstantChaumPedersenProofKnownNonce, String> {
    if (constant == null) {
        return Err("Null ConstantChaumPedersenProof in $where")
    }
    var proof = this.importChaumPedersenProof(constant.proof)

    if (proof == null) {
        // 1.0
        val challenge = this.importElementModQ(constant.challenge)
        val response = this.importElementModQ(constant.response)

        if (challenge == null || response == null) {
            return Err("Missing fields ConstantChaumPedersenProof in $where")
        }
        proof = GenericChaumPedersenProof(challenge, response)
    }

    return Ok(ConstantChaumPedersenProofKnownNonce(proof, constant.constant))
}

private fun GroupContext.importSelection(
    selection: electionguard.protogen.CiphertextBallotSelection,
    where: String
): Result<SubmittedBallot.Selection, String> {
    val here = "$where ${selection.selectionId}"

    val selectionHash = importUInt256(selection.selectionHash)
    val ciphertext = this.importCiphertext(selection.ciphertext)
    val cryptoHash = importUInt256(selection.cryptoHash)
    val proof = this.importDisjunctiveChaumPedersenProof(selection.proof, here)
    val extendedData = this.importHashedCiphertext(selection.extendedData)

    if (proof is Err) return proof
    if (selectionHash == null || ciphertext == null || cryptoHash == null) {
        return Err("Missing fields in selection $here")
    }

    return Ok(
        SubmittedBallot.Selection(
            selection.selectionId,
            selection.sequenceOrder,
            selectionHash,
            ciphertext,
            cryptoHash,
            selection.isPlaceholderSelection,
            proof.unwrap(),
            extendedData
        )
    )
}

private fun GroupContext.importDisjunctiveChaumPedersenProof(
    disjunct: electionguard.protogen.DisjunctiveChaumPedersenProof?, where: String
): Result<DisjunctiveChaumPedersenProofKnownNonce, String> {
    if (disjunct == null) {
        return Err("Missing DisjunctiveChaumPedersenProof in $where")
    }
    var proof0 = this.importChaumPedersenProof(disjunct.proof0)
    var proof1 = this.importChaumPedersenProof(disjunct.proof1)
    val proofChallenge = this.importElementModQ(disjunct.challenge)

    if (proof0 == null && proof1 == null) {
        // 1.0 election record
        val proofZeroPad = this.importElementModP(disjunct.proofZeroPad)
        val proofZeroData = this.importElementModP(disjunct.proofZeroData)
        val proofZeroChallenge = this.importElementModQ(disjunct.proofZeroChallenge)
        val proofZeroResponse = this.importElementModQ(disjunct.proofZeroResponse)

        val proofOnePad = this.importElementModP(disjunct.proofOnePad)
        val proofOneData = this.importElementModP(disjunct.proofOneData)
        val proofOneChallenge = this.importElementModQ(disjunct.proofOneChallenge)
        val proofOneResponse = this.importElementModQ(disjunct.proofOneResponse)

        if (proofZeroPad == null || proofZeroData == null || proofZeroChallenge == null ||
            proofZeroResponse == null || proofOnePad == null || proofOneData == null ||
            proofOneChallenge == null || proofOneResponse == null || proofChallenge == null
        ) {
            return Err("Failed to convert DisjunctiveChaumPedersenProofKnownNonce $where from proto (1)")
        }

        proof0 = GenericChaumPedersenProof(proofZeroChallenge, proofZeroResponse)
        proof1 = GenericChaumPedersenProof(proofOneChallenge, proofOneResponse)
    }

    if (proof0 == null || proof1 == null || proofChallenge == null) {
        return Err("Failed to convert DisjunctiveChaumPedersenProofKnownNonce $where from proto (2)")
    }

    return Ok(DisjunctiveChaumPedersenProofKnownNonce(proof0, proof1, proofChallenge))
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
            this.proof.let { this.proof.publishConstantChaumPedersenProof() },
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
            this.proof.let { this.proof.publishDisjunctiveChaumPedersenProof() },
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