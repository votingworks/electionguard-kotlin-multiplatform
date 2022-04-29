package electionguard.ballot

import electionguard.core.*

/** Results of KeyCeremony, for Encryption. */
data class ElectionInitialized(
    val config: ElectionConfig,
    /** The joint public key (K) in the ElectionGuard Spec. */
    val jointPublicKey: ElementModP, // maybe ElGamalPublicKey?
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val cryptoExtendedBaseHash: UInt256, // aka qbar
    val guardians: List<Guardian>,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun manifest(): Manifest {
        return config.manifest
    }
}

/** Public info for the ith Guardian/Trustee. */
data class Guardian(
    val guardianId: String,
    val xCoordinate: UInt, // use sequential numbering starting at 1; = i of T_i, K_i
    val coefficientCommitments: List<ElementModP>,  // h_j = g^a_j, j = 1..quorum; h_0 = K_i = public key
    val coefficientProofs: List<SchnorrProof>
) {
    fun publicKey() : ElementModP = coefficientCommitments[0]
}