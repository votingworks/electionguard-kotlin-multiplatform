package electionguard.decrypt

import electionguard.ballot.DecryptingGuardian
import electionguard.core.ElementModP
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.compatibleContextOrFail
import electionguard.core.multP

/** Partial decryptions from one DecryptingTrustee, includes both direct and compensated decryptions. */
class DecryptionShare(
    val decryptingTrustee: String, // who did these decryptions?
) {
    val directDecryptions: MutableMap<String, DirectDecryption> = mutableMapOf() // key "contestId#@selectionId"
    val compensatedDecryptions: MutableMap<String, CompensatedDecryption> = mutableMapOf() // key "contestId#@selectionId"

    fun addDirectDecryption(contestId: String, selectionId: String, decryption: DirectDecryption): DecryptionShare {
        // TODO test to see if there are duplicates?
        directDecryptions["${contestId}#@${selectionId}"] = decryption
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

// serialized to protobuf
data class RecoveredPartialDecryption(
    val decryptingGuardianId: String, // Trustee ℓ
    val missingGuardianId: String, // Trustee i
    val share: ElementModP,       // M_𝑖,ℓ
    val recoveryKey: ElementModP, // g^Pi(ℓ), where Pi(ℓ) is the value of the missing_guardian secret polynomial at
                                  // available guardian ℓ coordinate. used in the proof verification.
    val proof: GenericChaumPedersenProof // challenge c_𝑖,ℓ, response v_𝑖,ℓ
) {
    init {
        require(decryptingGuardianId.isNotEmpty())
        require(missingGuardianId.isNotEmpty())
    }
}

// serialized to protobuf
// All the direct and compensated decryptions are accumulated here
// Either theres a proof or a recovered field
class PartialDecryption(
    val selectionId: String,
    val guardianId: String, // share is for this guardian
    var share: ElementModP?, // M_𝑖 set by direct decryption, else computed from recovered
    val proof: GenericChaumPedersenProof?,       // present only when its directly computed
    recovered: List<RecoveredPartialDecryption>? // present only when its indirectly computed
) {
    // When guardians are missing there will be LOOK quorum or nguardians - nmissing >= quorum of these?
    val recoveredDecryptions: MutableList<RecoveredPartialDecryption> = mutableListOf()

    init {
        require(selectionId.isNotEmpty())
        require(guardianId.isNotEmpty())
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

    // Not a data class, have to do our own equals
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
    val partialDecryption: ElementModP, // 𝑀_i,l = 𝐴^Pi_(ℓ) (spec 1.03 section 3.5.2 eq 56)
    val proof: GenericChaumPedersenProof, // proof that we know Pi_(ℓ)
    val recoveredPublicKeyShare: ElementModP) // g^Pi(ℓ), used in the proof verification.