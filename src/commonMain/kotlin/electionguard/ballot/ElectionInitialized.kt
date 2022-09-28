package electionguard.ballot

import electionguard.core.*

/** Results of KeyCeremony, for Encryption. */
data class ElectionInitialized(
    val config: ElectionConfig,
    /** The joint public key (K) in the ElectionGuard Spec. */
    val jointPublicKey: ElementModP, // maybe ElGamalPublicKey?
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val cryptoBaseHash: UInt256, // aka q
    val cryptoExtendedBaseHash: UInt256, // aka qbar
    val guardians: List<Guardian>,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(guardians.isNotEmpty())
        require(guardians.size == this.config.numberOfGuardians)
        require(config.manifest.cryptoHash == manifestHash)
    }

    fun manifest(): Manifest {
        return config.manifest
    }
    fun addMetadata(vararg pairs: Pair<String, String>): ElectionInitialized {
        val added = metadata.toMutableMap()
        pairs.forEach { added[it.first] = it.second }
        return this.copy(metadata = added)
    }
    fun jointPublicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(this.jointPublicKey)
    }
    fun cryptoExtendedBaseHash(): ElementModQ {
        return this.cryptoExtendedBaseHash.toElementModQ(jointPublicKey.context)
    }
    fun numberOfGuardians(): Int {
        return this.config.numberOfGuardians
    }
}

/** Public info for the ith Guardian/Trustee. */
data class Guardian(
    val guardianId: String,
    val xCoordinate: Int, // use sequential numbering starting at 1; = i of T_i, K_i
    val coefficientProofs: List<SchnorrProof>
) {
    init {
        require(guardianId.isNotEmpty())
        require(xCoordinate > 0)
        require(coefficientProofs.isNotEmpty())
    }
    fun publicKey() : ElementModP = coefficientProofs[0].publicKey

    fun coefficientCommitments(): List<ElementModP> {
        return coefficientProofs.map { it.publicKey }
    }
}