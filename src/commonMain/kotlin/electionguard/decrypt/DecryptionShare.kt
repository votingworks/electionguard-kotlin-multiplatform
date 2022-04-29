package electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.compatibleContextOrFail
import electionguard.core.multP

/** partial decryptions from one DecryptingTrustee, includes both direct and compensated decryptions */
class DecryptionShare(
    val decryptingTrustee: String, // who did these Decryptions?
) {
    val partialDecryptions: MutableMap<String, DirectDecryption> = mutableMapOf()
    val compensatedDecryptions: MutableMap<String, CompensatedDecryption> = mutableMapOf()

    fun addPartialDecryption(contestId: String, selectionId: String, decryption: DirectDecryption): DecryptionShare {
        partialDecryptions["${contestId}#@${selectionId}"] = decryption
        return this
    }

    fun addRecoveredDecryption(
        contestId: String,
        selectionId: String,
        missingGuardian: String,
        decryption: RecoveredPartialDecryption
    ): DecryptionShare {
        var existing = compensatedDecryptions["${contestId}#@${selectionId}"]
        if (existing == null) {
            existing = CompensatedDecryption(selectionId)
        }
        existing.recoveredDecryption[missingGuardian] = decryption
        return this
    }
}

data class DirectDecryption(
    val selectionId: String,
    val guardianId: String,
    val share: ElementModP,
    val proof: GenericChaumPedersenProof,
)

class CompensatedDecryption(
    val selectionId: String,
) {
    // keyed by missing guardian id
    val recoveredDecryption: MutableMap<String, RecoveredPartialDecryption> = mutableMapOf()
}

data class RecoveredPartialDecryption(
    val decryptingGuardian: String,
    val missingGuardian: String,
    val share: ElementModP,
    val recoveryKey: ElementModP,
    val proof: GenericChaumPedersenProof
)

class PartialDecryption(
    val selectionId: String,
    val guardianId: String, // share for this guardian
    var share: ElementModP?,
    val proof: GenericChaumPedersenProof?,
    decryptions: List<RecoveredPartialDecryption>?
) {
    val recoveredDecryption: MutableList<RecoveredPartialDecryption> = mutableListOf()

    init {
        if (decryptions != null) {
            recoveredDecryption.addAll(decryptions)
        }
    }

    constructor(guardianId: String, partial: DirectDecryption) :
            this(partial.selectionId, guardianId, partial.share, partial.proof, null)

    constructor(guardianId: String, partial: CompensatedDecryption) :
            this(partial.selectionId, guardianId, null, null, null)

    fun add(recovered: RecoveredPartialDecryption): PartialDecryption {
        recoveredDecryption.add(recovered)
        return this
    }

    fun computeShare(): ElementModP {
        if (share == null) {
            val shares = recoveredDecryption.map { it.share }
            val context = compatibleContextOrFail(*shares.toTypedArray())
            share = context.multP(*shares.toTypedArray())
        }
        return share!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PartialDecryption

        if (selectionId != other.selectionId) return false
        if (guardianId != other.guardianId) return false
        if (share != other.share) return false
        if (proof != other.proof) return false
        if (recoveredDecryption != other.recoveredDecryption) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectionId.hashCode()
        result = 31 * result + guardianId.hashCode()
        result = 31 * result + (share?.hashCode() ?: 0)
        result = 31 * result + (proof?.hashCode() ?: 0)
        result = 31 * result + recoveredDecryption.hashCode()
        return result
    }


}

/** Direct decryption from the Decrypting Trustee */
data class PartialDecryptionAndProof(
    val partialDecryption: ElementModP,
    val proof: GenericChaumPedersenProof)

/** Compensated decryption from the Decrypting Trustee */
data class CompensatedPartialDecryptionAndProof(
    val partialDecryption: ElementModP, // used in the calculation. LOOK encrypt ??
    val proof: GenericChaumPedersenProof,
    val recoveredPublicKeyShare: ElementModP) // g^Pi(ℓ), used in the verification