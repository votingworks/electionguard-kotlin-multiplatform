package electionguard.pep

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.ChallengeResponse

interface PepTrusteeIF {
    fun blind(
        texts: List<ElGamalCiphertext>,
    ): List<BlindResponse>

    fun challenge(
        challenges: List<BlindChallenge>,
    ): List<BlindChallengeResponse>
}