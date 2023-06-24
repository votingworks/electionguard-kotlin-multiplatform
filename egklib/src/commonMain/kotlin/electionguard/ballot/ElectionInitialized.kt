package electionguard.ballot

import electionguard.core.*

/** Results of KeyCeremony, used for all the rest of the election processing. */
data class ElectionInitialized(
    val config: ElectionConfig,
    val jointPublicKey: ElementModP, // aka K
    val extendedBaseHash: UInt256, // aka He
    val guardians: List<Guardian>,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(guardians.isNotEmpty()) { "empty guardians" }
        require(guardians.size == this.config.numberOfGuardians) { "nguardians ${guardians.size} != ${this.config.numberOfGuardians}" }
    }

    fun jointPublicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(this.jointPublicKey)
    }
    fun cryptoExtendedBaseHash(): ElementModQ {
        return this.extendedBaseHash.toElementModQ(jointPublicKey.context)
    }
    fun numberOfGuardians(): Int {
        return this.config.numberOfGuardians
    }

    // Note this returns a copy
    fun addMetadataToCopy(vararg pairs: Pair<String, String>): ElectionInitialized {
        val added = metadata.toMutableMap()
        pairs.forEach { added[it.first] = it.second }
        return this.copy(metadata = added)
    }
}
