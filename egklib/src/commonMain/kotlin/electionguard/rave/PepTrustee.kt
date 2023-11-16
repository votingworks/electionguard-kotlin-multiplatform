package electionguard.rave

import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.randomElementModQ

class PepTrustee(val idx : Int, val group : GroupContext) :  PepTrusteeIF {
    val randomConstantNonce = group.randomElementModQ(2) // weak sauce

    // 1. all BGj in {BGj}:  // 4
    //    (a) ξj ← Zq
    //    (b) Compute Aj = α^ξj mod p and Bj = β^ξj mod p.
    //    (c) uj ← Zq
    //    (d) Compute aj = α^uj mod p and bj = β^uj mod p
    //    (e) Send (Aj, Bj, aj, bj) to admin
    override fun blind(texts: List<ElGamalCiphertext>): List<BlindResponse> {
        return texts.map { text ->
            val eps = group.randomElementModQ(2)
            val u = group.randomElementModQ(2)
            BlindResponse(
                text.pad powP eps,
                text.data powP eps,
                text.pad powP u,
                text.data powP u,
                eps + randomConstantNonce,
                u + randomConstantNonce,
                )
        }
    }

    override fun challenge(challenges: List<BlindChallenge>): List<BlindChallengeResponse> {
        return challenges.map {
            val eps = it.eps - randomConstantNonce
            val u = it.u - randomConstantNonce
            BlindChallengeResponse(u - it.challenge * eps)
        }
    }
}