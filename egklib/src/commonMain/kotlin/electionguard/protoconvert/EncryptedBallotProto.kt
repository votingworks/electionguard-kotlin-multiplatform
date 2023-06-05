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
import electionguard.preencrypt.RecordedPreBallot
import electionguard.preencrypt.RecordedPreContest
import electionguard.preencrypt.RecordedSelectionVector
import mu.KotlinLogging
import pbandk.ByteArr

private val logger = KotlinLogging.logger("EncryptedBallotConvert")

fun electionguard.protogen.EncryptedBallot.import(group: GroupContext):
        Result<EncryptedBallot, String> {
    val here = this.ballotId

    val confirmationCode = importUInt256(this.confirmationCode)
        .toResultOr { "EncryptedBallot $here trackingHash was malformed or missing" }
    val ballotState = this.state.import(here)

    val (contests, cerrors) = this.contests.map { it.import(here, group) }.partition()

    val errors = getAllErrors(confirmationCode, ballotState) + cerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot(
            this.ballotId,
            this.ballotStyleId,
            confirmationCode.unwrap(),
            this.codeBaux.array,
            contests,
            this.timestamp,
            ballotState.unwrap(),
            this.isPreencrypt,
        )
    )
}

private fun electionguard.protogen.EncryptedBallot.BallotState.import(where: String):
        Result<EncryptedBallot.BallotState, String> {
    val state = safeEnumValueOf<EncryptedBallot.BallotState>(this.name)
        ?: return Err("Failed to convert ballot state, missing or unknown name in $where\"")
    return Ok(state)
}

private fun electionguard.protogen.EncryptedBallotContest.import(
    where: String,
    group: GroupContext
): Result<EncryptedBallot.Contest, String> {
    val here = "$where ${this.contestId}"

    val contestHash = importUInt256(this.contestHash)
        .toResultOr { "CiphertextBallotContest $here contestHash was malformed or missing" }
    val proof = group.importRangeProof(here, this.proof)
    val contestData = group.importHashedCiphertext(this.encryptedContestData).toResultOr { "No contestData found" }

    val (selections, serrors) = this.selections.map { it.import(here, group) }.partition()

    val errors = getAllErrors(contestHash, proof, contestData) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot.Contest(
            this.contestId,
            this.sequenceOrder,
            contestHash.unwrap(),
            selections,
            proof.unwrap(),
            group.importHashedCiphertext(this.encryptedContestData)!!,
            group.importPreEncryption(this.preEncryption),
        )
    )
}

private fun GroupContext.importPreEncryption(proto: electionguard.protogen.PreEncryption?):
        EncryptedBallot.PreEncryption? {
    if (proto === null) {
        return null
    }
    return EncryptedBallot.PreEncryption(
        importUInt256(proto.contestHash)!!,
        proto.selectedVectors.map { it.import(this) },
        proto.allHashes.map { it.import(this) },
    )
}

private fun electionguard.protogen.PreEncryptionVector.import(group: GroupContext):
        EncryptedBallot.PreEncryptionVector {
    return EncryptedBallot.PreEncryptionVector(
        importUInt256(this.selectionHash)!!,
        this.code,
        this.selectedVector.map { group.importCiphertext(it)!! }, // LOOK make Result
    )
}

private fun GroupContext.importRangeProof(
    where: String,
    range: electionguard.protogen.RangeChaumPedersenProofKnownNonce?
): Result<RangeChaumPedersenProofKnownNonce, String> {
    if (range == null) {
        return Err("Null RangeChaumPedersenProofKnownNonce in $where")
    }
    val proofs = range.proofs.map { this.importChaumPedersenProof(it) }
    val allgood = proofs.map { it != null }.reduce{a, b -> a && b }

    return if (allgood) Ok(RangeChaumPedersenProofKnownNonce(proofs.map { it!! }))
    else Err("importChaumPedersenProof failed $where")
}

private fun electionguard.protogen.EncryptedBallotSelection.import(
    where: String,
    group: GroupContext,
):
        Result<EncryptedBallot.Selection, String> {
    val here = "$where ${this.selectionId}"

    val ciphertext = group.importCiphertext(this.ciphertext)
        .toResultOr { "CiphertextBallotSelection $here ciphertext was malformed or missing" }
    val proof = group.importRangeProof(here, this.proof)

    val errors = getAllErrors(proof, ciphertext)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot.Selection(
            this.selectionId,
            this.sequenceOrder,
            ciphertext.unwrap(),
            proof.unwrap(),
        )
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////
// preencrypt

fun EncryptedBallot.publishProto(recordedPreBallot: RecordedPreBallot) = electionguard.protogen.EncryptedBallot(
            this.ballotId,
            this.ballotStyleId,
            this.confirmationCode.publishProto(),
            ByteArr(this.codeBaux),
            this.contests.map { it.publishProto(recordedPreBallot) },
            this.timestamp,
            this.state.publishProto(),
            this.isPreencrypt,
        )

private fun EncryptedBallot.Contest.publishProto(recordedPreBallot: RecordedPreBallot):
        electionguard.protogen.EncryptedBallotContest {

    val rcontest = recordedPreBallot.contests.find { it.contestId == this.contestId }
        ?: throw IllegalArgumentException("Cant find ${this.contestId}")

    return electionguard.protogen
        .EncryptedBallotContest(
            this.contestId,
            this.sequenceOrder,
            this.contestHash.publishProto(),
            this.selections.map { it.publishProto() },
            this.proof.publishProto(),
            this.contestData.publishProto(),
            rcontest.publishProto(),
        )
}

private fun RecordedPreContest.publishProto():
        electionguard.protogen.PreEncryption {
    return electionguard.protogen.PreEncryption(
        this.contestHash.publishProto(),
        this.selectionVectors.map { it.publishProto() },
    )
}

private fun RecordedSelectionVector.publishProto():
        electionguard.protogen.PreEncryptionVector {
    return electionguard.protogen
        .PreEncryptionVector(
            this.selectionHash.publishProto(),
            this.code,
            this.selectionVector.map { it.publishProto() },
        )
}

/////////////////////////////////////////////////////////////////////////////////////////////////
// normal

fun EncryptedBallot.publishProto() =
    electionguard.protogen.EncryptedBallot(
        this.ballotId,
        this.ballotStyleId,
        this.confirmationCode.publishProto(),
        ByteArr(this.codeBaux),
        this.contests.map { it.publishProto() },
        this.timestamp,
        this.state.publishProto()
    )

private fun EncryptedBallot.BallotState.publishProto() =
    try {
        electionguard.protogen.EncryptedBallot.BallotState.fromName(this.name)
    } catch (e: IllegalArgumentException) {
        logger.error { "BallotState $this has missing or unknown name" }
        electionguard.protogen.EncryptedBallot.BallotState.UNKNOWN
    }

private fun EncryptedBallot.Contest.publishProto() =
    electionguard.protogen.EncryptedBallotContest(
        this.contestId,
        this.sequenceOrder,
        this.contestHash.publishProto(),
        this.selections.map { it.publishProto() },
        this.proof.publishProto(),
        this.contestData.publishProto(),
    )

private fun EncryptedBallot.Selection.publishProto() =
    electionguard.protogen.EncryptedBallotSelection(
        this.selectionId,
        this.sequenceOrder,
        this.ciphertext.publishProto(),
        this.proof.publishProto(),
    )

fun RangeChaumPedersenProofKnownNonce.publishProto() =
    electionguard.protogen.RangeChaumPedersenProofKnownNonce(
        this.proofs.map { it.publishProto() },
    )