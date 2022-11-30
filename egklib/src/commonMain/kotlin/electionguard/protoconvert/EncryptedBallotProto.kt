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

private val logger = KotlinLogging.logger("EncryptedBallotConvert")

fun electionguard.protogen.EncryptedBallot.import(group: GroupContext):
        Result<EncryptedBallot, String> {
    val here = this.ballotId

    val manifestHash = importUInt256(this.manifestHash)
        .toResultOr { "EncryptedBallot $here manifestHash was malformed or missing" }
    val trackingHash = importUInt256(this.code)
        .toResultOr { "EncryptedBallot $here trackingHash was malformed or missing" }
    val previousTrackingHash = importUInt256(this.codeSeed)
        .toResultOr { "EncryptedBallot $here previousTrackingHash was malformed or missing" }
    val cryptoHash = importUInt256(this.cryptoHash)
        .toResultOr { "EncryptedBallot $here cryptoHash was malformed or missing" }
    val ballotState = this.state.import(here)

    val (contests, cerrors) = this.contests.map { it.import(here, group) }.partition()

    val errors = getAllErrors(manifestHash, trackingHash, previousTrackingHash, cryptoHash, ballotState) + cerrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot(
            this.ballotId,
            this.ballotStyleId,
            manifestHash.unwrap(),
            trackingHash.unwrap(),
            previousTrackingHash.unwrap(),
            contests,
            this.timestamp,
            cryptoHash.unwrap(),
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
    val cryptoHash = importUInt256(this.cryptoHash)
        .toResultOr { "CiphertextBallotContest $here cryptoHash was malformed or missing" }
    val proof = group.importRangeProof(here, this.proof)
    val contestData = group.importHashedCiphertext(this.encryptedContestData).toResultOr { "No contestData found" }

    val (selections, serrors) = this.selections.map { it.import(here, group) }.partition()

    val errors = getAllErrors(contestHash, cryptoHash, proof, contestData) + serrors
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot.Contest(
            this.contestId,
            this.sequenceOrder,
            contestHash.unwrap(),
            selections,
            cryptoHash.unwrap(),
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

    val selectionHash = importUInt256(this.selectionHash)
        .toResultOr { "CiphertextBallotSelection $here selectionHash was malformed or missing" }
    val ciphertext = group.importCiphertext(this.ciphertext)
        .toResultOr { "CiphertextBallotSelection $here ciphertext was malformed or missing" }
    val cryptoHash = importUInt256(this.cryptoHash)
        .toResultOr { "CiphertextBallotSelection $here cryptoHash was malformed or missing" }
    val proof = group.importRangeProof(here, this.proof)

    val errors = getAllErrors(proof, selectionHash, ciphertext, cryptoHash)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(
        EncryptedBallot.Selection(
            this.selectionId,
            this.sequenceOrder,
            selectionHash.unwrap(),
            ciphertext.unwrap(),
            cryptoHash.unwrap(),
            proof.unwrap(),
        )
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////
// preencrypt

fun EncryptedBallot.publishProto(recordedPreBallot: RecordedPreBallot) = electionguard.protogen.EncryptedBallot(
            this.ballotId,
            this.ballotStyleId,
            this.manifestHash.publishProto(),
            this.code.publishProto(),
            this.codeSeed.publishProto(),
            this.contests.map { it.publishProto(recordedPreBallot) },
            this.timestamp,
            this.cryptoHash.publishProto(),
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
            this.cryptoHash.publishProto(),
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
        this.manifestHash.publishProto(),
        this.code.publishProto(),
        this.codeSeed.publishProto(),
        this.contests.map { it.publishProto() },
        this.timestamp,
        this.cryptoHash.publishProto(),
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
        this.cryptoHash.publishProto(),
        this.proof.publishProto(),
        this.contestData.publishProto(),
    )

private fun EncryptedBallot.Selection.publishProto() =
    electionguard.protogen.EncryptedBallotSelection(
        this.selectionId,
        this.sequenceOrder,
        this.selectionHash.publishProto(),
        this.ciphertext.publishProto(),
        this.cryptoHash.publishProto(),
        this.proof.publishProto(),
    )

fun RangeChaumPedersenProofKnownNonce.publishProto() =
    electionguard.protogen.RangeChaumPedersenProofKnownNonce(
        this.proofs.map { it.publishProto() },
    )