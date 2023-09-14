package electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.GroupContext

/** The interface to a DecryptingTrustee. */
interface DecryptingTrusteeIF {
    /** Guardian id.  */
    fun id(): String

    /** Guardian x coordinate  */
    fun xCoordinate(): Int

    /** The guardian's public key = K_i.  */
    fun guardianPublicKey(): ElementModP

    /**
     * Compute partial decryptions of elgamal encryptions.
     *
     * @param texts list of ElementModP (ciphertext.pad or A) to be partially decrypted
     * @return a list of partial decryptions, in the same order as the texts
     */
    fun decrypt(
        group: GroupContext,
        texts: List<ElementModP>,
    ): List<PartialDecryption>

    /**
     * Compute responses to Chaum-Pedersen challenges
     * @param challenges list of Chaum-Pedersen challenges
     * @return a list of responses, in the same order as the challenges
     */
    fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse>
}