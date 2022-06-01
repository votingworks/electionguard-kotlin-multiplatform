package electionguard.decrypt

import electionguard.ballot.DecryptingGuardian
import electionguard.core.ElementModP
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.compatibleContextOrFail
import electionguard.core.multP

/** Partial decryptions from one DecryptingTrustee, includes both direct and compensated decryptions. */
class DecryptionShare(
    val decryptingTrustee: String, // who did these Decryptions?
) {
    val partialDecryptions: MutableMap<String, DirectDecryption> = mutableMapOf()
    val compensatedDecryptions: MutableMap<String, CompensatedDecryption> = mutableMapOf()

    fun addPartialDecryption(contestId: String, selectionId: String, decryption: DirectDecryption): DecryptionShare {
        // TODO test to see if there are duplicates?
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
            compensatedDecryptions["${contestId}#@${selectionId}"] = existing
        }
        // TODO test to see if there are duplicates?
        existing.missingDecryptions[missingGuardian] = decryption
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
    // keyed by missing guardian id.
    val missingDecryptions: MutableMap<String, RecoveredPartialDecryption> = mutableMapOf()
}

data class RecoveredPartialDecryption(
    val decryptingGuardianId: String,
    val missingGuardianId: String,
    val share: ElementModP,  // M_𝑖,ℓ
    val recoveryKey: ElementModP,
    val proof: GenericChaumPedersenProof
)

// all the direct and compensated decryptions are accumulated here
class PartialDecryption(
    val selectionId: String,
    val guardianId: String, // share for this guardian
    var share: ElementModP?, // M_𝑖 set by direct decryption, else computed from missingDecryptions
    val proof: GenericChaumPedersenProof?, // not null when its directly computed
    recovered: List<RecoveredPartialDecryption>? // not null when its indirectly computed
) {
    // When guardian is missing there will be quorum of these
    val recoveredDecryptions: MutableList<RecoveredPartialDecryption> = mutableListOf()

    init {
        if (recovered != null) {
            recoveredDecryptions.addAll(recovered)
        }
    }

    constructor(guardianId: String, partial: DirectDecryption) :
            this(partial.selectionId, guardianId, partial.share, partial.proof, null)

    constructor(guardianId: String, partial: CompensatedDecryption) :
            this(partial.selectionId, guardianId, null, null, null)

    fun add(recovered: RecoveredPartialDecryption): PartialDecryption {
        recoveredDecryptions.add(recovered)
        return this
    }

    fun lagrangeInterpolation(guardians: List<DecryptingGuardian>) {
        if (share == null && recoveredDecryptions.isEmpty()) {
            throw IllegalStateException("PartialDecryption $selectionId has neither share nor missingDecryptions")
        }
        if (share != null && recoveredDecryptions.isNotEmpty()) {
            throw IllegalStateException("PartialDecryption $selectionId has both share and missingDecryptions")
        }
        // the quardians and missingDecryptions are sorted by guardianId, so can use index to match with
        // 𝑀_𝑖 = ∏ ℓ∈𝑈 (𝑀_𝑖,ℓ) mod 𝑝, where 𝑀_𝑖,ℓ = 𝐴^𝑃_𝑖(ℓ) mod 𝑝.
        if (recoveredDecryptions.isNotEmpty()) {
            val shares = recoveredDecryptions.sortedBy { it.decryptingGuardianId }.mapIndexed { idx, value ->
                value.share powP guardians[idx].lagrangeCoordinate
            }
            val context = compatibleContextOrFail(*shares.toTypedArray())
            share = context.multP(*shares.toTypedArray())
        }
    }

    // When everything is done, the share is never null
    fun share(): ElementModP {
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
        if (recoveredDecryptions != other.recoveredDecryptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectionId.hashCode()
        result = 31 * result + guardianId.hashCode()
        result = 31 * result + (share?.hashCode() ?: 0)
        result = 31 * result + (proof?.hashCode() ?: 0)
        result = 31 * result + recoveredDecryptions.hashCode()
        return result
    }
}

/** Direct decryption from the Decrypting Trustee */
data class DirectDecryptionAndProof(
    val partialDecryption: ElementModP,
    val proof: GenericChaumPedersenProof)

/** Compensated decryption from the Decrypting Trustee */
data class CompensatedDecryptionAndProof(
    val partialDecryption: ElementModP, // used in the calculation. TODO encrypt
    val proof: GenericChaumPedersenProof,
    val recoveredPublicKeyShare: ElementModP) // g^Pi(ℓ), used in the verification