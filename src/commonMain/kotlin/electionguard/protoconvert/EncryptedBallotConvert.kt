package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*

fun GroupContext.importEncryptedBallot(
    ballot: electionguard.protogen.EncryptedBallot
): Result<EncryptedBallot, String> {
    val here = ballot.ballotId

    val manifestHash = importUInt256(ballot.manifestHash)
        .toResultOr {"EncryptedBallot $here manifestHash was malformed or missing"}
    val trackingHash = importUInt256(ballot.code)
        .toResultOr {"EncryptedBallot $here trackingHash was malformed or missing"}
    val previousTrackingHash = importUInt256(ballot.codeSeed)
        .toResultOr {"EncryptedBallot $here previousTrackingHash was malformed or missing"}
    val cryptoHash = importUInt256(ballot.cryptoHash)
        .toResultOr {"EncryptedBallot $here cryptoHash was malformed or missing"}
    val ballotState = ballot.state.importBallotState(ballot.ballotId)

    val (contests, cerrors) = ballot.contests.map { this.importContest(it, ballot.ballotId) }.partition()

    val errors = getAllErrors(manifestHash, trackingHash, previousTrackingHash, cryptoHash, ballotState) + cerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot(
            ballot.ballotId,
            ballot.ballotStyleId,
            manifestHash.unwrap(),
            trackingHash.unwrap(),
            previousTrackingHash.unwrap(),
            contests,
            ballot.timestamp,
            cryptoHash.unwrap(),
            ballotState.unwrap(),
        )
    )
}

private fun electionguard.protogen.EncryptedBallot.BallotState.importBallotState(where: String):
        Result<EncryptedBallot.BallotState, String> {

    val name = this.name ?: return Err("Failed to convert ballot state, missing name in $where\"")

    return try {
        Ok(EncryptedBallot.BallotState.valueOf(name))
    } catch (e: IllegalArgumentException) {
        Err("Failed to convert ballot state, unknown name $name in $where\"")
    }
}

private fun GroupContext.importContest(
    contest: electionguard.protogen.EncryptedBallotContest, where: String,
): Result<EncryptedBallot.Contest, String> {
    val here = "$where ${contest.contestId}"

    val contestHash = importUInt256(contest.contestHash)
        .toResultOr {"CiphertextBallotContest $here contestHash was malformed or missing"}
    val cryptoHash = importUInt256(contest.cryptoHash)
        .toResultOr {"CiphertextBallotContest $here cryptoHash was malformed or missing"}
    val proof = this.importConstantChaumPedersenProof(contest.proof, here)

    val (selections, serrors) = contest.selections.map { this.importSelection(it, here) }.partition()

    val errors = getAllErrors(contestHash, cryptoHash, proof) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            contestHash.unwrap(),
            selections,
            cryptoHash.unwrap(),
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
    selection: electionguard.protogen.EncryptedBallotSelection,
    where: String
): Result<EncryptedBallot.Selection, String> {
    val here = "$where ${selection.selectionId}"

    val selectionHash = importUInt256(selection.selectionHash)
        .toResultOr {"CiphertextBallotSelection $here selectionHash was malformed or missing"}
    val ciphertext = this.importCiphertext(selection.ciphertext)
        .toResultOr {"CiphertextBallotSelection $here ciphertext was malformed or missing"}
    val cryptoHash = importUInt256(selection.cryptoHash)
        .toResultOr {"CiphertextBallotSelection $here cryptoHash was malformed or missing"}
    val proof = this.importDisjunctiveChaumPedersenProof(selection.proof, here)
    val extendedData = this.importHashedCiphertext(selection.extendedData)

    val errors = getAllErrors(proof, selectionHash, ciphertext, cryptoHash)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot.Selection(
            selection.selectionId,
            selection.sequenceOrder,
            selectionHash.unwrap(),
            ciphertext.unwrap(),
            cryptoHash.unwrap(),
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

fun EncryptedBallot.publishEncryptedBallot(): electionguard.protogen.EncryptedBallot {
    return electionguard.protogen
        .EncryptedBallot(
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

private fun EncryptedBallot.BallotState.publishBallotState():
        electionguard.protogen.EncryptedBallot.BallotState {
    return electionguard.protogen.EncryptedBallot.BallotState.fromName(this.name)
}

private fun EncryptedBallot.Contest.publishContest():
        electionguard.protogen.EncryptedBallotContest {
    return electionguard.protogen
        .EncryptedBallotContest(
            this.contestId,
            this.sequenceOrder,
            this.contestHash.publishUInt256(),
            this.selections.map { it.publishSelection() },
            this.cryptoHash.publishUInt256(),
            this.proof.let { this.proof.publishConstantChaumPedersenProof() },
        )
}

private fun EncryptedBallot.Selection.publishSelection():
        electionguard.protogen.EncryptedBallotSelection {
    return electionguard.protogen
        .EncryptedBallotSelection(
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