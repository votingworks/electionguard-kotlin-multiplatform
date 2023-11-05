package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.UInt256

/** One decryption from one Decrypting Trustee */
data class PartialDecryption(
    val guardianId: String,  // guardian i
    val Mi: ElementModP, // Mi = A ^ P(i); spec 2.0.0, eq 66 or = C0 ^ P(i); eq 77
    //// these are needed for the proof
    val u: ElementModQ,  // opaque, just pass back to the trustee
    val a: ElementModP,  // g^u
    val b: ElementModP,  // A^u
)

/** One selection decryption from one Decrypting Trustees. */
data class DecryptionResult(
    val selectionKey: String,     // "contestId#@selectionId"
    val ciphertext: ElGamalCiphertext, // text to decrypt
    val share: PartialDecryption,
)

/** One contest data decryption from one Decrypting Trustees. */
data class ContestDataResult(
    val contestId: String,
    val ciphertext: HashedElGamalCiphertext, // text to decrypt
    val share: PartialDecryption,
)

/** All decryptions from one Decrypting Trustee for one ballot/tally. */
class TrusteeDecryptions(val trusteeId : String) {
    val shares = mutableMapOf<String, DecryptionResult>() // key "contestId#@selectionId" aka "selectionKey"
    val contestData = mutableMapOf<String, ContestDataResult>() // key = contestId

    fun addDecryption(contestId: String, selectionId: String, ciphertext: ElGamalCiphertext, decryption: PartialDecryption) {
        val selectionKey = "${contestId}#@${selectionId}"
        this.shares[selectionKey] = DecryptionResult(selectionKey, ciphertext, decryption)
    }

    fun addContestDataResults(contestId: String, ciphertext: HashedElGamalCiphertext, decryption: PartialDecryption) {
        this.contestData[contestId] = ContestDataResult(contestId, ciphertext, decryption)
    }
}

data class ChallengeRequest(
    val id: String, // "contestId#@selectionId" aka "selectionKey"
    val challenge: ElementModQ, // 2.0, eq 72
    val nonce: ElementModQ, // opaque, only for use by the trustee
)

data class ChallengeResponse(
    val id: String, // "contestId#@selectionId" aka "selectionKey"
    val response: ElementModQ, // 2.0, eq 73
)

data class TrusteeChallengeResponses(
    val id: String, // "contestId#@selectionId" aka "selectionKey"
    val results: List<ChallengeResponse>,
)

//// Mutable structures to hold the work as it progresses

/**
 * One decryption with info from all the Decrypting Trustees.
 * Mutable, built incrementally.
 */
class DecryptionResults(
    val selectionKey: String,     // "contestId#@selectionId"
    val ciphertext: ElGamalCiphertext, // text to decrypt
    val shares: MutableMap<String, PartialDecryption>, // key by guardianId
    var tally: Int? = null, // the decrypted tally
    var M: ElementModP? = null, // lagrange weighted product of the shares, M = Prod(M_i^w_i) mod p; spec 2.0, eq 68
    var collectiveChallenge: UInt256? = null, // spec 2.0, eq 71
    var responses: MutableMap<String, ElementModQ> = mutableMapOf(), // key = guardianId, v_i; spec 2.0, eq 73
)

/**
 * One contest data decryption with info from all the Decrypting Trustees.
 * Mutable, built incrementally.
 */
class ContestDataResults(
    val contestId: String,
    val hashedCiphertext: HashedElGamalCiphertext, // text to decrypt
    val shares: MutableMap<String, PartialDecryption>, // key by guardianId
    var beta: ElementModP? = null,
    var collectiveChallenge: UInt256? = null, // joint challenge value, spec 2.0, eq 81
    var responses: MutableMap<String, ElementModQ> = mutableMapOf(), // key = guardianId, v_i; spec 2.0, eq 82
)

/**
 * All decryptions from all the Decrypting Trustees for one ballot.
 * Mutable, built incrementally.
 */
class AllDecryptions {
    val shares: MutableMap<String, DecryptionResults> = mutableMapOf() // key "contestId#@selectionId" = "selectionKey"
    val contestData: MutableMap<String, ContestDataResults> = mutableMapOf() // key contestId

    fun addTrusteeDecryptions(trusteeDecryptions : TrusteeDecryptions) {
        trusteeDecryptions.shares.forEach {
            addDecryption(it.key, it.value.ciphertext, it.value.share)
        }
        trusteeDecryptions.contestData.forEach {
            addContestDataResults(it.key, it.value.ciphertext, it.value.share)
        }
    }

    /** add Partial decryptions from one DecryptingTrustee. */
    fun addDecryption(selectionKey: String, ciphertext: ElGamalCiphertext, decryption: PartialDecryption) {
        var decryptionResults = shares[selectionKey]
        if (decryptionResults == null) {
            decryptionResults = DecryptionResults(selectionKey, ciphertext, mutableMapOf())
            shares[selectionKey] = decryptionResults
        }
        decryptionResults.shares[decryption.guardianId] = decryption
    }

    /** add Partial decryptions from one DecryptingTrustee. */
    fun addContestDataResults(contestId: String, ciphertext: HashedElGamalCiphertext, decryption: PartialDecryption) {
        var cresult = contestData[contestId]
        if (cresult == null) {
            cresult = ContestDataResults(contestId, ciphertext, mutableMapOf())
            contestData[contestId] = cresult
        }
        cresult.shares[decryption.guardianId] = decryption
    }

    /** add challenge response from one DecryptingTrustee. */
    fun addChallengeResponse(guardianId: String, cr : ChallengeResponse) : Boolean {
        val dresult = shares[cr.id]
        if (dresult != null) {
            dresult.responses[guardianId] = cr.response
        } else {
            val cdresult = contestData[cr.id]
            if (cdresult == null) {
                return false
            }
            cdresult.responses[guardianId] = cr.response
        }
        return true
    }
}


