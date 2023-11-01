package electionguard.pep

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*
import electionguard.json2.*
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

data class BallotPep(
    val ballotId: String,
    val isEq: Boolean,
    val contests: List<ContestPep>,
)

data class ContestPep(
    val contestId: String,
    val selections: List<SelectionPep>,
)

data class SelectionPep(
    val selectionId: String,
    val ciphertextRatio: ElGamalCiphertext, // α, β
    val ciphertextAB: ElGamalCiphertext, // A, B
    val blindingProof: ChaumPedersenProof,
    val T: ElementModP,
    val decryptionProof: ChaumPedersenProof,
) {

    constructor(step1: PepSimple.SelectionStep1, dselection: DecryptedTallyOrBallot.Selection) : this (
        dselection.selectionId,
        step1.ciphertextRatio,
        step1.ciphertextAB,
        ChaumPedersenProof(step1.c, step1.v),
        dselection.bOverM,
        dselection.proof,
    )
    constructor(selection: PepTrusted.SelectionWorking, dselection: DecryptedTallyOrBallot.Selection) : this(
        dselection.selectionId,
        selection.ciphertextRatio,
        selection.ciphertextAB,
        ChaumPedersenProof(selection.c, selection.v),
        dselection.bOverM,
        dselection.proof,
    )
    constructor(work: BlindWorking, dselection: DecryptedTallyOrBallot.Selection) : this (
        dselection.selectionId,
        work.ciphertextRatio,
        work.ciphertextAB!!,
        ChaumPedersenProof(work.c, work.v!!),
        dselection.bOverM,
        dselection.proof,
    )
}

@Serializable
data class BallotPepJson(
    val ballot_id: String,
    val is_equal: Boolean,
    val contests: List<ContestPepJson>,
)

@Serializable
data class ContestPepJson(
    val contest_id: String,
    val selections: List<SelectionPepJson>,
)

@Serializable
data class SelectionPepJson(
    val selection_id: String,
    val ciphertext_ratio: ElGamalCiphertextJson,
    val ciphertext_AB: ElGamalCiphertextJson,
    val blinding_proof: ChaumPedersenJson,
    val T: ElementModPJson,
    val decryption_proof: ChaumPedersenJson,
)

fun BallotPep.publishJson(): BallotPepJson {
    val contests = this.contests.map { pcontest ->

        //     val selectionId: String,
        //    val ciphertextRatio: ElGamalCiphertext, // α, β
        //    val ciphertextAB: ElGamalCiphertext, // A, B
        //    val blindingProof: ChaumPedersenProof,
        //    val T: ElementModP,
        //    val decryptionProof: ChaumPedersenProof,
        ContestPepJson(
            pcontest.contestId,
            pcontest.selections.map {
                SelectionPepJson(
                    it.selectionId,
                    it.ciphertextRatio.publishJson(),
                    it.ciphertextAB.publishJson(),
                    it.blindingProof.publishJson(),
                    it.T.publishJson(),
                    it.decryptionProof.publishJson(),
                )
            })
    }
    return BallotPepJson(this.ballotId, this.isEq, contests)
}

///////////////////////////////////////////////////////////////////////////////////////////////

fun BallotPepJson.import(group: GroupContext, errs : ErrorMessages): BallotPep? {
    val contests = this.contests.map { it.import(group, errs.nested("ContestPepJson ${it.contest_id}")) }

    return if (errs.hasErrors()) null
    else BallotPep(this.ballot_id, this.is_equal, contests.filterNotNull())
}

fun ContestPepJson.import(group: GroupContext, errs : ErrorMessages): ContestPep? {
    val selections = this.selections.map { it.import(group, errs.nested("SelectionPepJson ${it.selection_id}")) }

    return if (errs.hasErrors()) null
    else ContestPep(
        this.contest_id,
        selections.filterNotNull()
    )
}

fun SelectionPepJson.import(group: GroupContext, errs : ErrorMessages): SelectionPep? {
    val ciphertextRatio = this.ciphertext_ratio.import(group) ?: errs.addNull("malformed ciphertext_ratio") as ElGamalCiphertext?
    val ciphertextAB = this.ciphertext_AB.import(group) ?: errs.addNull("malformed ciphertext_AB") as ElGamalCiphertext?
    val blindingProof = this.blinding_proof.import(group, errs.nested("BlindingProof"))
    val decryptionProof = this.decryption_proof.import(group, errs.nested("DecryptionProof"))
    val T = this.T.import(group) ?: errs.addNull("malformed T") as ElementModP?

    return if (errs.hasErrors()) null
    else SelectionPep(
        this.selection_id,
        ciphertextRatio!!,
        ciphertextAB!!,
        blindingProof!!,
        T!!,
        decryptionProof!!
    )
}