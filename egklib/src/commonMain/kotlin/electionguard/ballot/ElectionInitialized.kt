package electionguard.ballot

import electionguard.core.*

/** Results of KeyCeremony, used for all the rest of the election processing. */
data class ElectionInitialized(
    val config: ElectionConfig,
    /** The joint public key (K) in the ElectionGuard Spec. */
    val jointPublicKey: ElementModP,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
    val cryptoBaseHash: UInt256,  // aka q
    val cryptoExtendedBaseHash: UInt256, // aka qbar
    val guardians: List<Guardian>,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(guardians.isNotEmpty()) { "empty guardians" }
        require(guardians.size == this.config.numberOfGuardians) { "nguardians ${guardians.size} != ${this.config.numberOfGuardians}" }
        require(config.manifest.cryptoHash == manifestHash) { "bad manifest hash" }
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
