package electionguard.ballot

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.hashElements

/**
 * Makes a CiphertextElectionContext object. python: election.make_ciphertext_election_context().
 * python: make_ciphertext_election_context()
 * @param number_of_guardians The number of guardians necessary to generate the public key.
 * @param quorum The quorum of guardians necessary to decrypt an election.  Must be less than number_of_guardians.
 * @param jointPublicKey the joint public key of the election, K.
 * @param manifest the election manifest.
 * @param commitment_hash all the public commitments for all the guardians = H(K 1,0 , K 1,1 , K 1,2 , ... ,
 * K 1,k−1 , K 2,0 , K 2,1 , K 2,2 , ... , K 2,k−1 , ... , K n,0 , K n,1 , K n,2 , ... , K n,k−1 )
 */
fun createCiphertextElectionContext(
    groupContext : GroupContext,
    number_of_guardians: Int,
    quorum: Int,
    jointPublicKey: ElementModP,
    manifest: Manifest,
    commitment_hash: ElementModQ,
    extended_data: Map<String, String>?
): CiphertextElectionContext {

    val crypto_base_hash: ElementModQ = groupContext.hashElements(
        groupContext.get_large_prime(),
        groupContext.get_small_prime(),
        groupContext.get_generator(),
        number_of_guardians, quorum, manifest.cryptoHashElement()
    )
    val crypto_extended_base_hash: ElementModQ = groupContext.hashElements(crypto_base_hash, commitment_hash)
    return CiphertextElectionContext(
        number_of_guardians,
        quorum,
        jointPublicKey,
        manifest.cryptoHashElement(),
        crypto_base_hash,
        crypto_extended_base_hash,
        commitment_hash,
        extended_data
    )
}

/**
 * The cryptographic context of an election.
 * @see [Baseline Parameters](https://www.electionguard.vote/spec/0.95.0/3_Baseline_parameters/)
 * for definition of 𝑄.
 *
 * @see [Key Generation](https://www.electionguard.vote/spec/0.95.0/4_Key_generation/.details-of-key-generation)
 * for defintion of K, 𝑄, 𝑄'.
 */
class CiphertextElectionContext(
    /** The number of guardians necessary to generate the public key.  */
    val number_of_guardians: Int,
    /** The quorum of guardians necessary to decrypt an election.  Must be less than number_of_guardians.  */
    val quorum: Int,
    /** The joint public key (K) in the ElectionGuard Spec.  */
    val jointPublicKey: ElementModP,
    val commitment_hash: ElementModQ,
    val manifest_hash: ElementModQ,
    val crypto_base_hash: ElementModQ,
    val crypto_extended_base_hash: ElementModQ,
    val extended_data: Map<String, String>?
)