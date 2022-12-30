package electionguard.decrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.UInt256

/** One decryption from one Decrypting Trustee */
data class PartialDecryption(
    val guardianId: String,  // guardian i
    val mbari: ElementModP, // M_i = A ^ P(i)
    val u: ElementModQ,
    val a: ElementModP,
    val b: ElementModP,
)

/** One decryption with info from all the Decrypting Trustees. */
data class DecryptionResult(
    val id: String,     // "contestId#@selectionId"
    val ciphertext: ElGamalCiphertext, // A
    val share: PartialDecryption,
)

/** One contest data decryption with info from all the Decrypting Trustees. */
data class ContestDataResult(
    val contestId: String,
    val ciphertext: HashedElGamalCiphertext,
    val share: PartialDecryption,
)

/** All decryptions from one Decrypting Trustee for one ballot. */
class TrusteeDecryptions(val id : String) {
    val shares: MutableMap<String, DecryptionResult> = mutableMapOf() // key "contestId#@selectionId"
    val contestData: MutableMap<String, ContestDataResult> = mutableMapOf() // key contestId

    fun addDecryption(contestId: String, selectionId: String, ciphertext: ElGamalCiphertext, decryption: PartialDecryption) {
        val id = "${contestId}#@${selectionId}"
        this.shares[id] = DecryptionResult(id, ciphertext, decryption)
    }

    fun addContestDataResults(contestId: String, ciphertext: HashedElGamalCiphertext, decryption: PartialDecryption) {
        this.contestData[contestId] = ContestDataResult(contestId, ciphertext, decryption)
    }
}

data class ChallengeRequest(
    val id: String, // contest-selection id
    val challenge: ElementModQ,
    val nonce: ElementModQ,
)

data class ChallengeResponse(
    val id: String, // contest-selection id
    val response: ElementModQ,
)

data class TrusteeChallengeResponses(
    val id: String,
    val results: List<ChallengeResponse>,
)

//// Mutable structures to hold the work as it progresses

/** One decryption with info from all the Decrypting Trustees. */
class DecryptionResults(
    val id: String,     // "contestId#@selectionId"
    val ciphertext: ElGamalCiphertext, // A
    val shares: MutableMap<String, PartialDecryption>, // key by guardianId
    var dlogM: Int? = null,
    var mbar: ElementModP? = null, // mbar = prod(Mi)
    var challenge: UInt256? = null,
    var responses: MutableMap<String, ElementModQ> = mutableMapOf(), // guardianId, v_i
)

/** One contest data decryption with info from all the Decrypting Trustees. */
class ContestDataResults(
    val contestId: String,
    val ciphertext: HashedElGamalCiphertext,
    val shares: MutableMap<String, PartialDecryption>, // key by guardianId
    var beta: ElementModP? = null,
    var challenge: UInt256? = null,
    var responses: MutableMap<String, ElementModQ> = mutableMapOf(), // guardianId, v_i
)

/** All decryptions from all the Decrypting Trustees for one ballot. */
class Decryptions {
    val shares: MutableMap<String, DecryptionResults> = mutableMapOf() // key "contestId#@selectionId"
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
    fun addDecryption(guardianId: String, ciphertext: ElGamalCiphertext, decryption: PartialDecryption) {
        var decryptionResults = shares[guardianId]
        if (decryptionResults == null) {
            decryptionResults = DecryptionResults(guardianId, ciphertext, mutableMapOf())
            shares[guardianId] = decryptionResults
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
    fun addChallengeResponse(guardianId: String, cr : ChallengeResponse) {
        val dresult = shares[cr.id]
        if (dresult != null) {
            dresult.responses[guardianId] = cr.response
        } else {
            val cdresult = contestData[cr.id] ?: throw IllegalStateException("Cant find ${cr.id}")
            cdresult.responses[guardianId] = cr.response
        }
    }
}


