package electionguard.ballot

import electionguard.core.*

/** Results of KeyCeremony, for Encryption. */
data class ElectionInitialized(
    val config: ElectionConfig,
    /** The joint public key (K) in the ElectionGuard Spec. */
    val jointPublicKey: ElementModP,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val cryptoExtendedBaseHash: UInt256, // qbar
    val guardians: List<Guardian>,
    val metadata: Map<String, String> = emptyMap(),
) {

    fun manifest(): Manifest {
        return config.manifest
    }
}

/** Public info for Guardian. */
data class Guardian(
    val guardianId: String,
    val xCoordinate: Int,
    val coefficientCommitments: List<ElementModP>,
    val coefficientProofs: List<SchnorrProof>
) {
    fun publicKey() : ElementModP = coefficientCommitments[0]
}