package electionguard.protoconvert

import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.preencrypt.RecordedPreBallot
import electionguard.preencrypt.RecordedPreEncryption
import electionguard.preencrypt.RecordedSelectionVector
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import pbandk.ByteArr

private val logger = KotlinLogging.logger("EncryptedBallotConvert")

fun electionguard.protogen.EncryptedBallot.import(group: GroupContext, errs : ErrorMessages): EncryptedBallot? {
    val confirmationCode = importUInt256(this.confirmationCode) ?: errs.addNull("malformed confirmationCode") as UInt256?
    val electionId = importUInt256(this.electionId) ?: errs.addNull("malformed electionId") as UInt256?
    val ballotState = this.state.import() ?: errs.addNull("malformed ballotState") as EncryptedBallot.BallotState?

    val contests = this.contests.map { it.import(group, errs.nested("EncryptedBallotContest ${it.contestId}")) }

    return if (errs.hasErrors()) null
    else EncryptedBallot(
            this.ballotId,
            this.ballotStyleId,
            this.encryptingDevice,
            this.timestamp,
            this.codeBaux.array,
            confirmationCode!!,
            electionId!!,
            contests.filterNotNull(),
            ballotState!!,
            this.isPreencrypt,
        )
}

private fun electionguard.protogen.EncryptedBallot.BallotState.import(): EncryptedBallot.BallotState? {
    return safeEnumValueOf<EncryptedBallot.BallotState>(this.name)
}

private fun electionguard.protogen.EncryptedBallotContest.import(group: GroupContext, errs : ErrorMessages): EncryptedBallot.Contest? {
    val contestHash = importUInt256(this.contestHash) ?: errs.addNull("malformed contestHash") as UInt256?
    val proof = group.importRangeProof(this.proof, errs.nested("RangeProof"))
    val selections = this.selections.map { it.import(group, errs.nested("EncryptedBallotSelection ${it.selectionId}")) }

    val contestData = group.importHashedCiphertext(this.encryptedContestData, errs)
    val preEncryption = if (this.preEncryption == null) null else {
        group.importPreEncryption(this.preEncryption, errs.nested("preEncryption"))
    }

    return if (errs.hasErrors()) null
    else EncryptedBallot.Contest(
            this.contestId,
            this.sequenceOrder,
            this.votesAllowed,
            contestHash!!,
            selections.filterNotNull(),
            proof!!,
            contestData!!,
            preEncryption,
        )
}

private fun GroupContext.importPreEncryption(proto: electionguard.protogen.PreEncryption, errs : ErrorMessages): EncryptedBallot.PreEncryption? {
    val preencryptionHash = importUInt256(proto.preencryptionHash) ?: errs.addNull("malformed preencryptionHash") as UInt256?
    val selectionHashes = proto.allSelectionHashes.mapIndexed { idx,it -> importUInt256(it) ?: errs.addNull("malformed allSelectionHashes $idx") as UInt256? }
    val selectedVectors = proto.selectedVectors.mapIndexed { idx,it -> this.importSelectionVector(it, errs.nested("selectedVector $idx")) }

    return if (errs.hasErrors()) null
    else EncryptedBallot.PreEncryption(
        preencryptionHash!!,
        selectionHashes.filterNotNull(),
        selectedVectors.filterNotNull(),
    )
}

private fun GroupContext.importSelectionVector(vector: electionguard.protogen.SelectionVector, errs : ErrorMessages):
        EncryptedBallot.SelectionVector? {

    val selectionHash = importUInt256(vector.selectionHash) ?: errs.addNull("malformed selectionHash") as UInt256?
    val encryptions = vector.encryptions.mapIndexed { idx,it -> this.importCiphertext(it) ?: errs.addNull("malformed encryptions $idx") as ElGamalCiphertext? }

    return if (errs.hasErrors()) null
    else EncryptedBallot.SelectionVector(
        selectionHash!!,
        vector.shortCode,
        encryptions.filterNotNull(),
    )
}

private fun GroupContext.importRangeProof(range: electionguard.protogen.ChaumPedersenRangeProofKnownNonce?, errs : ErrorMessages): ChaumPedersenRangeProofKnownNonce? {
    if (range == null) {
        errs.add("missing RangeProof")
        return null
    }
    val proofs = range.proofs.mapIndexed { idx,it -> this.importChaumPedersenProof(it, errs.nested("Proof $idx")) }

    return if (errs.hasErrors()) null
    else ChaumPedersenRangeProofKnownNonce(proofs.filterNotNull())
}

private fun electionguard.protogen.EncryptedBallotSelection.import(group: GroupContext, errs : ErrorMessages): EncryptedBallot.Selection? {
    val ciphertext = group.importCiphertext(this.encryptedVote) ?: errs.addNull("malformed encryptedVote") as ElGamalCiphertext?
    val proof = group.importRangeProof(this.proof, errs.nested("RangeProof"))

    return if (errs.hasErrors()) null
    else EncryptedBallot.Selection(
            this.selectionId,
            this.sequenceOrder,
            ciphertext!!,
            proof!!,
        )
}

////////////////////////////////////////////////////////////////////////////////////////////////
// preencrypt

fun EncryptedBallot.publishProto(recordedPreBallot: RecordedPreBallot) = electionguard.protogen.EncryptedBallot(
    this.ballotId,
    this.ballotStyleId,
    this.encryptingDevice,
    this.timestamp,
    ByteArr(this.codeBaux),
    this.confirmationCode.publishProto(),
    this.electionId.publishProto(),
    this.contests.map { it.publishProto(recordedPreBallot) },
    this.state.publishProto(),
    true,
)

private fun EncryptedBallot.Contest.publishProto(recordedPreBallot: RecordedPreBallot):
        electionguard.protogen.EncryptedBallotContest {

    val rcontest = recordedPreBallot.contests.find { it.contestId == this.contestId }
        ?: throw IllegalArgumentException("Cant find ${this.contestId}")

    return electionguard.protogen
        .EncryptedBallotContest(
            this.contestId,
            this.sequenceOrder,
            this.votesAllowed,
            this.contestHash.publishProto(),
            this.selections.map { it.publishProto() },
            this.proof.publishProto(),
            this.contestData.publishProto(),
            rcontest.publishProto(),
        )
}

private fun RecordedPreEncryption.publishProto():
        electionguard.protogen.PreEncryption {
    return electionguard.protogen.PreEncryption(
        this.preencryptionHash.publishProto(),
        this.allSelectionHashes.map { it.publishProto() },
        this.selectedVectors.map { it.publishProto() },
    )
}

private fun RecordedSelectionVector.publishProto():
        electionguard.protogen.SelectionVector {
    return electionguard.protogen
        .SelectionVector(
            this.selectionHash.toUInt256safe().publishProto(),
            this.shortCode,
            this.encryptions.map { it.publishProto() },
            // this.proofs.map { it.publishProto() },
        )
}

/////////////////////////////////////////////////////////////////////////////////////////////////
// normal

fun EncryptedBallot.publishProto() =
    electionguard.protogen.EncryptedBallot(
        this.ballotId,
        this.ballotStyleId,
        this.encryptingDevice,
        this.timestamp,
        ByteArr(this.codeBaux),
        this.confirmationCode.publishProto(),
        this.electionId.publishProto(),
        this.contests.map { it.publishProto() },
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
        this.votesAllowed,
        this.contestHash.publishProto(),
        this.selections.map { it.publishProto() },
        this.proof.publishProto(),
        this.contestData.publishProto(),
    )

private fun EncryptedBallot.Selection.publishProto() =
    electionguard.protogen.EncryptedBallotSelection(
        this.selectionId,
        this.sequenceOrder,
        this.encryptedVote.publishProto(),
        this.proof.publishProto(),
    )

fun ChaumPedersenRangeProofKnownNonce.publishProto() =
    electionguard.protogen.ChaumPedersenRangeProofKnownNonce(
        this.proofs.map { it.publishProto() },
    )