package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.UInt256

data class ChallengeRequest(
    val id: String, // contest-selection id
    val challenge: ElementModQ,
    val nonce: ElementModQ,
)

data class ChallengeResponse(
    val id: String, // contest-selection id
    val response: ElementModQ,
)

/** One decryption from the Decrypting Trustee */
data class PartialDecryption(
    val guardianId: String,  // guardian i
    val mbari: ElementModP, // Mbar_i
    val u: ElementModQ,
    val a: ElementModP,
    val b: ElementModP,
)

class DecryptionResults(
    val id: String,     // "contestId#@selectionId"
    val ciphertext: ElGamalCiphertext,
    val shares: MutableMap<String, PartialDecryption>, // key by guardianId
    var dlogM: Int? = null,
    var M: ElementModP? = null,
    var challenge: UInt256? = null,
    var responses: MutableMap<String, ElementModQ> = mutableMapOf(), // guardianId, v_i
)

class TrusteeDecryptions() {
    val shares: MutableMap<String, DecryptionResults> = mutableMapOf() // key "contestId#@selectionId"

    /** add Partial decryptions from one DecryptingTrustee. */
    fun addDecryption(contestId: String, selectionId: String, ciphertext: ElGamalCiphertext, decryption: PartialDecryption) {
        val id = "${contestId}#@${selectionId}"
        var hasOne = shares[id]
        if (hasOne == null) {
            hasOne = DecryptionResults(id, ciphertext, mutableMapOf())
            shares[id] = hasOne
        }
        hasOne.shares[decryption.guardianId] = decryption
    }

    /** add challenge responses from one DecryptingTrustee. */
    fun addChallengeResponse(guardianId: String, cr : ChallengeResponse) {
        val hasOne = shares[cr.id] ?: throw IllegalStateException()
        hasOne.responses[guardianId] = cr.response
    }
}

