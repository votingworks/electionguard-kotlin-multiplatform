package electionguard.pep

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ

data class BlindResponse(
    val bigAj : ElementModP,
    val bigBj : ElementModP,
    val aj : ElementModP,
    val bj : ElementModP,
    val eps: ElementModQ,  // opaque, just pass back when challenging
    val u: ElementModQ,  // opaque, just pass back when challenging
)

data class BlindChallenge(
    val challenge: ElementModQ,
    val eps: ElementModQ,  // opaque, just pass back when challenging
    val u: ElementModQ,  // opaque, just pass back when challenging
)

data class BlindChallengeResponse(
    val response: ElementModQ,
)

data class BlindWorking (
    val ciphertextRatio: ElGamalCiphertext,
    val bigA : ElementModP,
    val bigB : ElementModP,
    val a : ElementModP,
    val b : ElementModP,
    val c: ElementModQ,
    val blindChallenges: MutableList<BlindChallenge> = mutableListOf(),
    var blindResponses: List<BlindResponse>? = null,
    var ciphertextAB: ElGamalCiphertext? = null,
    var v: ElementModQ? = null,
 )