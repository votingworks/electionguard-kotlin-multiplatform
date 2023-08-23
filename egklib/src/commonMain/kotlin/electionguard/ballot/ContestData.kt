package electionguard.ballot

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import mu.KotlinLogging
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray
import kotlin.math.max

private val logger = KotlinLogging.logger("ContestData")
private const val debug = false
private const val BLOCK_SIZE : Int = 32
private const val CHOP_WRITE_INS : Int = 30

private const val label = "share_enc_keys"
private const val contestDataLabel = "contest_data"

enum class ContestDataStatus {
    normal, null_vote, over_vote, under_vote
}

/**
 * This information consists of any text written into one or more write-in text fields, information about overvotes,
 * undervotes, and null votes, and possibly other data about voter selections.
 */
data class ContestData(
    val overvotes: List<Int>,
    val writeIns: List<String>,
    val status: ContestDataStatus = if (overvotes.isNotEmpty()) ContestDataStatus.over_vote else ContestDataStatus.normal,
) {

    fun publish(filler: String = ""): electionguard.protogen.ContestData {
        return publish(
            this.status,
            this.overvotes,
            this.writeIns,
            filler,
        )
    }

    fun publish(
        status: ContestDataStatus,
        overvotes: List<Int>,
        writeIns: List<String>,
        filler: String = ""
    ): electionguard.protogen.ContestData {
        return electionguard.protogen.ContestData(
            status.publishContestDataStatus(),
            overvotes,
            writeIns,
            filler,
        )
    }

    // Make sure that the HashedElGamalCiphertext message is exactly (votesAllowed + 1) * BLOCK_SIZE
    // If too large, remove extra writeIns, add "*" to list to indicate some were removed
    // If still too large, truncate writeIns to CHOP_WRITE_INS characters, append "*" to string to indicate truncated
    // If still too large, truncate overVote to (votesAllowed + 1), append "-1" to list to indicate some were removed
    // If now too small, add a filler string to make it exactly (votesAllowed + 1) * BLOCK_SIZE
    fun encrypt(
        publicKey: ElGamalPublicKey, // aka K
        extendedBaseHash: UInt256, // aka He
        contestId: String, // aka Λ
        contestIndex: Int, // ind_c(Λ)
        ballotNonce: UInt256,
        votesAllowed: Int): HashedElGamalCiphertext {

        val messageSize = (1 + votesAllowed) * BLOCK_SIZE

        var trialContestData = this
        var trialContestDataBA = trialContestData.publish().encodeToByteArray()
        var trialSize = trialContestDataBA.size
        val trialSizes = mutableListOf<Int>()
        trialSizes.add(trialSize)

        // remove extra write_ins, append a "*"
        if ((trialSize > messageSize) && trialContestData.writeIns.size > votesAllowed) {
            val truncateWriteIns = trialContestData.writeIns.subList(0, votesAllowed)
                .toMutableList()
            truncateWriteIns.add("*")
            trialContestData = trialContestData.copy(
                writeIns = truncateWriteIns,
            )
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }

        // chop write-in vote strings
        if ((trialSize > messageSize) && trialContestData.writeIns.isNotEmpty()) {
            val chop = max(CHOP_WRITE_INS, (messageSize - trialSize + votesAllowed - 1) / votesAllowed)
            val truncateWriteIns = trialContestData.writeIns.map {
                if (it.length <= CHOP_WRITE_INS) it else it.substring(0, chop) + "*"
            }
            trialContestData = trialContestData.copy(writeIns = truncateWriteIns)
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }

        // chop overvote list
        while (trialSize > messageSize && (trialContestData.overvotes.size > votesAllowed + 1)) {
            val chopList = trialContestData.overvotes.subList(0, votesAllowed + 1) + (-1)
            trialContestData = trialContestData.copy(overvotes = chopList)
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }

        // now fill it up so its a uniform message length, if needed
        if (trialSize < messageSize) {
            val filler = StringBuilder().apply {
                repeat(messageSize - trialSize - 2) { append("*") }
            }
            trialContestDataBA = trialContestData.publish(filler.toString()).encodeToByteArray()
            trialSize = trialContestDataBA.size
            trialSizes.add(trialSize)
        }
        if (debug) println("encodedData = $trialContestData")
        if (debug) println(" trialSizes = $trialSizes")

        return trialContestDataBA.encryptContestData(publicKey, extendedBaseHash, contestId, contestIndex, ballotNonce)
    }

    fun ByteArray.encryptContestData(
        publicKey: ElGamalPublicKey, // aka K
        extendedBaseHash: UInt256, // aka He
        contestId: String, // aka Λ
        contestIndex: Int, // ind_c(Λ)
        ballotNonce: UInt256): HashedElGamalCiphertext {

        // D = D_1 ∥ D_2 ∥ · · · ∥ D_bD  ; eq (49)
        val messageBlocks: List<UInt256> =
            this.toList()
                .chunked(32) { block ->
                    // pad each block of the message to 32 bytes
                    val result = ByteArray(32) { 0 }
                    block.forEachIndexed { index, byte -> result[index] = byte }
                    UInt256(result)
                }

        val group = compatibleContextOrFail(publicKey.key)

        // ξ = H(HE ; 0x20, ξB , indc (Λ), “contest data”) (eq 50)
        val contestDataNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestIndex, contestDataLabel)

        // ElectionGuard spec: (α, β) = (g^ξ mod p, K^ξ mod p); by encrypting a zero, we achieve exactly this
        val (alpha, beta) = 0.encrypt(publicKey, contestDataNonce.toElementModQ(group))
        // k = H(HE ; 0x22, K, α, β) ; eq 51
        val kdfKey = hashFunction(extendedBaseHash.bytes, 0x22.toByte(), publicKey.key, alpha, beta)

        // TODO check
        // context = b(”contest_data”) ∥ b(Λ).
        val context = "$contestDataLabel$contestId"
        val kdf = KDF(kdfKey, label, context, this.size * 8) // TODO is this eq(52) ??

        val k0 = kdf[0]
        val c0 = alpha.byteArray() // (53)
        val encryptedBlocks = messageBlocks.mapIndexed { i, p -> (p xor kdf[i + 1]).bytes }.toTypedArray()
        val c1 = concatByteArrays(*encryptedBlocks) // (54)
        val c2 = (c0 + c1).hmacSha256(k0) // ; eq (55) TODO can we use hmacFunction() ??

        return HashedElGamalCiphertext(alpha, c1, c2, this.size)
    }
}

// TODO could be used in Encryptor ??
fun makeContestData(
    votesAllowed: Int,
    selections: List<PlaintextBallot.Selection>,
    writeIns: List<String>
): ContestData {
    val votedFor = mutableListOf<Int>()
    for (selection in selections) {
        if (selection.vote > 0) {
            votedFor.add(selection.sequenceOrder)
        }
    }

    val totalVotedFor = votedFor.size + writeIns.size
    val status = if (totalVotedFor == 0) ContestDataStatus.null_vote
    else if (totalVotedFor < votesAllowed)  ContestDataStatus.under_vote
    else if (totalVotedFor > votesAllowed)  ContestDataStatus.over_vote
    else ContestDataStatus.normal

    return ContestData(
        if (status == ContestDataStatus.over_vote) votedFor else emptyList(),
        writeIns,
        status
    )
}

fun HashedElGamalCiphertext.decryptWithBetaToContestData(
    publicKey: ElGamalPublicKey, // aka K
    extendedBaseHash: UInt256, // aka He
    contestId: String, // aka Λ
    beta : ElementModP) : Result<ContestData, String> {

    val ba: ByteArray = this.decryptContestData(publicKey, extendedBaseHash, contestId, c0, beta) ?: return Err( "decryptWithNonceToContestData failed")
    val proto = electionguard.protogen.ContestData.decodeFromByteArray(ba)
    return importContestData(proto)
}

fun HashedElGamalCiphertext.decryptWithNonceToContestData(
    publicKey: ElGamalPublicKey, // aka K
    extendedBaseHash: UInt256, // aka He
    contestId: String, // aka Λ
    contestIndex: Int,
    ballotNonce: UInt256) : Result<ContestData, String> {

    val group = compatibleContextOrFail(publicKey.key)
    val contestDataNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestIndex, contestDataLabel)
    val (alpha, beta) = 0.encrypt(publicKey, contestDataNonce.toElementModQ(group))
    val ba: ByteArray = this.decryptContestData(publicKey, extendedBaseHash, contestId, alpha, beta) ?: return Err( "decryptWithNonceToContestData failed")
    val proto = electionguard.protogen.ContestData.decodeFromByteArray(ba)
    return importContestData(proto)
}

fun HashedElGamalCiphertext.decryptWithSecretKey(
    publicKey: ElGamalPublicKey, // aka K
    extendedBaseHash: UInt256, // aka He
    contestId: String, // aka Λ
    secretKey: ElGamalSecretKey): ByteArray? = decryptContestData(publicKey, extendedBaseHash, contestId, c0, c0 powP secretKey.key)

private fun HashedElGamalCiphertext.decryptContestData(
    publicKey: ElGamalPublicKey, // aka K
    extendedBaseHash: UInt256, // aka He
    contestId: String, // aka Λ
    alpha: ElementModP,
    beta: ElementModP): ByteArray? {

    // k = H(HE ; 22, K, α, β). (51)
    val kdfKey = hashFunction(extendedBaseHash.bytes, 0x22.toByte(), publicKey.key, alpha, beta)

    // context = b(”contest_data”) ∥ b(Λ).
    val context = "$contestDataLabel$contestId"
    val kdf = KDF(kdfKey, label, context, numBytes * 8) // TODO check this (86, 87) ??
    val k0 = kdf[0]

    val expectedHmac = (c0.byteArray() + c1).hmacSha256(k0) // TODO use hmacFunction() ?

    if (expectedHmac != c2) {
        logger.error { "HashedElGamalCiphertext decryptContestData failure: HMAC doesn't match" }
        return null
    }

    val ciphertextBlocks = c1.toList().chunked(32) { it.toByteArray().toUInt256() } // eq 88
    val plaintextBlocks = ciphertextBlocks.mapIndexed { i, c -> (c xor kdf[i + 1]).bytes }.toTypedArray()
    val plaintext = concatByteArrays(*plaintextBlocks) // eq 89

    return if (plaintext.size == numBytes) {
        plaintext
    } else {
        // Truncate trailing values, which should be zeros.
        // No need to check, because we've already validated the HMAC on the data.
        plaintext.copyOfRange(0, numBytes)
    }
}

//////////////////////////////////////////////////////////////////
// LOOK maybe move to protoconvert

fun importContestData(proto : electionguard.protogen.ContestData?): Result<ContestData, String> {
    if (proto == null) return Err( "ContestData is missing")
    return Ok(ContestData(
        proto.overVotes,
        proto.writeIns,
        importContestDataStatus(proto.status)?: ContestDataStatus.normal,
    ))
}

private fun importContestDataStatus(proto: electionguard.protogen.ContestData.Status): ContestDataStatus? {
    val result = safeEnumValueOf<ContestDataStatus>(proto.name)
    if (result == null) {
        logger.error { "ContestDataStatus $proto has missing or unknown name" }
    }
    return result
}

private fun ContestDataStatus.publishContestDataStatus(): electionguard.protogen.ContestData.Status {
    return try {
        electionguard.protogen.ContestData.Status.fromName(this.name)
    }  catch (e: IllegalArgumentException) {
        logger.error { "ContestDataStatus $this has missing or unknown name" }
        electionguard.protogen.ContestData.Status.NORMAL
    }
}

