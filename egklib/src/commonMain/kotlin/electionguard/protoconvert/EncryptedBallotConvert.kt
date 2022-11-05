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
import mu.KotlinLogging

private val logger = KotlinLogging.logger("EncryptedBallotConvert")

fun GroupContext.importEncryptedBallot(ballot: electionguard.protogen.EncryptedBallot):
        Result<EncryptedBallot, String> {
    val here = ballot.ballotId

    val manifestHash = importUInt256(ballot.manifestHash)
        .toResultOr { "EncryptedBallot $here manifestHash was malformed or missing" }
    val trackingHash = importUInt256(ballot.code)
        .toResultOr { "EncryptedBallot $here trackingHash was malformed or missing" }
    val previousTrackingHash = importUInt256(ballot.codeSeed)
        .toResultOr { "EncryptedBallot $here previousTrackingHash was malformed or missing" }
    val cryptoHash = importUInt256(ballot.cryptoHash)
        .toResultOr { "EncryptedBallot $here cryptoHash was malformed or missing" }
    val ballotState = importBallotState(ballot.ballotId, ballot.state)

    val (contests, cerrors) = ballot.contests.map { this.importContest(ballot.ballotId, it) }.partition()

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

private fun importBallotState(where: String, proto: electionguard.protogen.EncryptedBallot.BallotState):
        Result<EncryptedBallot.BallotState, String> {
    val state = safeEnumValueOf<EncryptedBallot.BallotState>(proto.name)
        ?: return Err("Failed to convert ballot state, missing or unknown name in $where\"")
    return Ok(state)
}

private fun GroupContext.importContest(
    where: String,
    contest: electionguard.protogen.EncryptedBallotContest,
): Result<EncryptedBallot.Contest, String> {
    val here = "$where ${contest.contestId}"

    val contestHash = importUInt256(contest.contestHash)
        .toResultOr { "CiphertextBallotContest $here contestHash was malformed or missing" }
    val cryptoHash = importUInt256(contest.cryptoHash)
        .toResultOr { "CiphertextBallotContest $here cryptoHash was malformed or missing" }
    val proof = this.importRangeProof(here, contest.proof)
    val contestData = this.importHashedCiphertext(contest.encryptedContestData).toResultOr { "No contestData found" }

    val (selections, serrors) = contest.selections.map { this.importSelection(here, it) }.partition()

    val errors = getAllErrors(contestHash, cryptoHash, proof, contestData) + serrors
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
            contestData.unwrap(),
        )
    )
}

private fun GroupContext.importRangeProof(
    where: String,
    range: electionguard.protogen.RangeChaumPedersenProofKnownNonce?
):
        Result<RangeChaumPedersenProofKnownNonce, String> {
    if (range == null) {
        return Err("Null RangeChaumPedersenProofKnownNonce in $where")
    }
    return Ok(
        RangeChaumPedersenProofKnownNonce(
            range.proofs.map { this.importChaumPedersenProof(it)!! },
        )
    )
}

private fun GroupContext.importSelection(
    where: String,
    selection: electionguard.protogen.EncryptedBallotSelection
):
        Result<EncryptedBallot.Selection, String> {
    val here = "$where ${selection.selectionId}"

    val selectionHash = importUInt256(selection.selectionHash)
        .toResultOr { "CiphertextBallotSelection $here selectionHash was malformed or missing" }
    val ciphertext = this.importCiphertext(selection.ciphertext)
        .toResultOr { "CiphertextBallotSelection $here ciphertext was malformed or missing" }
    val cryptoHash = importUInt256(selection.cryptoHash)
        .toResultOr { "CiphertextBallotSelection $here cryptoHash was malformed or missing" }
    val proof = this.importRangeProof(here, selection.proof)

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
            proof.unwrap(),
        )
    )
}

/////////////////////////////////////////////////////////////////////////////////////////////////

fun EncryptedBallot.publishEncryptedBallot() =
    electionguard.protogen.EncryptedBallot(
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

private fun EncryptedBallot.BallotState.publishBallotState() =
    try {
        electionguard.protogen.EncryptedBallot.BallotState.fromName(this.name)
    } catch (e: IllegalArgumentException) {
        logger.error { "BallotState $this has missing or unknown name" }
        electionguard.protogen.EncryptedBallot.BallotState.UNKNOWN
    }

private fun EncryptedBallot.Contest.publishContest() =
    electionguard.protogen.EncryptedBallotContest(
        this.contestId,
        this.sequenceOrder,
        this.contestHash.publishUInt256(),
        this.selections.map { it.publishSelection() },
        this.cryptoHash.publishUInt256(),
        this.proof.let { this.proof.publishRangeProof() },
        this.contestData.let { this.contestData.publishHashedCiphertext() },
    )

private fun EncryptedBallot.Selection.publishSelection() =
    electionguard.protogen.EncryptedBallotSelection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionHash.publishUInt256(),
        this.ciphertext.publishCiphertext(),
        this.cryptoHash.publishUInt256(),
        this.proof.let { this.proof.publishRangeProof() },
    )

fun RangeChaumPedersenProofKnownNonce.publishRangeProof() =
    electionguard.protogen.RangeChaumPedersenProofKnownNonce(
        this.proofs.map { it.publishChaumPedersenProof() },
    )