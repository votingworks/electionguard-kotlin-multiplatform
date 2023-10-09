package electionguard.pep

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*

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

                val verifya = pep.blindingProof.verify(
                    extendedBaseHash,
                    0x42.toByte(),
                    jointPublicKey.key,
                    pep.ciphertextRatio.pad, pep.ciphertextRatio.data,
                    pep.ciphertextAB.pad, pep.ciphertextAB.data,
                )
                if (!verifya) errors.add("PEP test 3.a error on ${selectionKey}")

                val verifyb = pep.decryptionProof.verifyDecryption(
                    extendedBaseHash,
                    jointPublicKey.key,
                    pep.ciphertextAB,
                    pep.T,
                )
                if (!verifyb) errors.add("PEP test 3.b error on ${contest.contestId}#${pep.selectionId}")
                // println(" selection ${selectionKey} verifya = $verifya verifyb = $verifyb")
            }
        }
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString(";"))
    }
}