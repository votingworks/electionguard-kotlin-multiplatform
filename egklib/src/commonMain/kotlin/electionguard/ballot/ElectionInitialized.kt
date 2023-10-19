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
        require(guardians.size == this.config.numberOfGuardians) {
            "ElectionInitialized nguardians ${guardians.size} does not match config= ${this.config.numberOfGuardians}"
        }
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

    fun show(): String = buildString {
        appendLine("ElectionInitialized")
        append(config.show())
        appendLine(" jointPublicKey ${jointPublicKey}")
        appendLine(" extendedBaseHash ${extendedBaseHash}")
        guardians.forEach {
            appendLine(" ${it}")
        }
    }
}

// Computation of the extended base hash HE
// HE = H(HB ; 0x12, K)
// B0 = HB
// B1 = 0x12 ∥ b(K, 512)
// len(B1 ) = 513
fun electionExtendedHash(Hb: UInt256, jointPublicKey: ElementModP) : UInt256 {
    return hashFunction(
        Hb.bytes,
        0x12.toByte(),
        jointPublicKey,
    )
}
