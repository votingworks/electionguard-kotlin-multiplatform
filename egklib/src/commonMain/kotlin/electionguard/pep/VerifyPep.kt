package electionguard.pep

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*

data class BallotPep(
    val isEq: Boolean,
    val ballotId: String,
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
    val c: ElementModQ, // 1.e
    val v: ElementModQ, // 1.f
    val T: ElementModP, // step 2, T
    val c_prime: ElementModQ, // step 2, c'
    val v_prime: ElementModQ, // step 2, v'
) {

    constructor(step1: PepSimple.SelectionStep1, dselection: DecryptedTallyOrBallot.Selection) : this(
        dselection.selectionId,
        step1.ciphertextRatio,
        step1.ciphertextAB,
        step1.c,
        step1.v,
        dselection.bOverM,
        dselection.proof.c,
        dselection.proof.r,
    )
    constructor(selection: PepTrusted.SelectionWorking, dselection: DecryptedTallyOrBallot.Selection) : this(
        dselection.selectionId,
        selection.ciphertextRatio,
        selection.ciphertextAB,
        selection.c,
        selection.v,
        dselection.bOverM,
        dselection.proof.c,
        dselection.proof.r,
    )
    constructor(work: BlindWorking, dselection: DecryptedTallyOrBallot.Selection) : this (
        dselection.selectionId,
        work.ciphertextRatio,
        work.ciphertextAB!!,
        work.c,
        work.v!!,
        dselection.bOverM,
        dselection.proof.c,
        dselection.proof.r,
    )
}

class VerifierPep(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
) {
    //    (a) verify if ChaumPedersenProof(c, v).verify(cons0; {cons1, K}, α, β, A, B).   // 4
    //    (b) verify if ChaumPedersenProof(c', v').verifyDecryption(g, K, A, B, T)   // 4
    //    (c) If T = 1, IsEq = 1 and (A, B) ̸= (1, 1), output “accept(equal)”.
    //         If T ̸= 1, IsEq = 0, output “accept(unequal)”.
    //         Otherwise, output “reject”

    fun verify(ballotPEP: BallotPep): Result<Boolean, String> {
        val errors = mutableListOf<String>()
        ballotPEP.contests.forEach { contest ->
            contest.selections.forEach { pep ->
                val selectionKey = "${contest.contestId}#${pep.selectionId}"

                val verifya = ChaumPedersenProof(pep.c, pep.v).verify(
                    extendedBaseHash,
                    0x42.toByte(),
                    jointPublicKey.key,
                    pep.ciphertextRatio.pad, pep.ciphertextRatio.data,
                    pep.ciphertextAB.pad, pep.ciphertextAB.data,
                )
                if (!verifya) errors.add("PEP test 3.a failed on ${selectionKey}")

                val verifyb = ChaumPedersenProof(pep.c_prime, pep.v_prime).verifyDecryption(
                    extendedBaseHash,
                    jointPublicKey.key,
                    pep.ciphertextAB,
                    pep.T,
                )
                if (!verifyb) errors.add("PEP test 3.b failed on ${contest.contestId}#${pep.selectionId}")
                // println(" selection ${selectionKey} verifya = $verifya verifyb = $verifyb")
            }
        }
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString(";"))
    }
}