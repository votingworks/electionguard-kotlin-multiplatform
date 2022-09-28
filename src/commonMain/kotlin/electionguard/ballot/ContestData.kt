package electionguard.ballot

import electionguard.core.ElGamalPublicKey
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.safeEnumValueOf
import mu.KotlinLogging
import pbandk.encodeToByteArray

private val logger = KotlinLogging.logger("ContestData")
private const val BLOCK_SIZE : Int = 32
private const val CHOP_WRITE_INS : Int = 30 // LOOK maybe make smaller in case of multibyte characters?

enum class ContestDataStatus {
    normal, null_vote, over_vote, under_vote
}

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

    fun encrypt(publicKey: ElGamalPublicKey, votesAllowed: Int): HashedElGamalCiphertext {
        val messageSize = (1 + votesAllowed) * BLOCK_SIZE

        var trialContestData = this
        var trialContestDataBA = trialContestData.publish().encodeToByteArray()
        var trialSize = trialContestDataBA.size

        if ((trialSize > messageSize) && this.writeIns.isNotEmpty()) {
            // see if you can just chop the writeIn lengths
            trialContestData = trialContestData.copy(writeIns = chopWriteins(CHOP_WRITE_INS))
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size

            if ((trialSize > messageSize) && trialContestData.writeIns.size > votesAllowed + 1) {
                // remove extra write_ins, append a "*"
                val truncateWriteIns = trialContestData.writeIns.subList(0, votesAllowed + 1)
                    .toMutableList()
                truncateWriteIns.add("*")
                trialContestData = trialContestData.copy(
                    writeIns = truncateWriteIns,
                    status = ContestDataStatus.over_vote
                )
                trialContestDataBA = trialContestData.publish().encodeToByteArray()
                trialSize = trialContestDataBA.size
            }
        }

        // this next part guarantees the result is <= message length
        if (trialSize > messageSize) {
            val bytesToRemove = trialSize - messageSize
            trialContestData = trialContestData.copy(overvotes = trialContestData.removeOvervotes(bytesToRemove))
            trialContestDataBA = trialContestData.publish().encodeToByteArray()
            trialSize = trialContestDataBA.size
        }

        // now fill it up so its a uniform message length, if needed
        if (trialSize < messageSize) {
            val filler = StringBuilder().apply {
                repeat(messageSize - trialSize - 2) {
                    append("*")
                }
            }
            trialContestDataBA = trialContestData.publish(filler.toString()).encodeToByteArray()
            trialSize = trialContestDataBA.size
        }

        // HMAC encryption
        val result = trialContestDataBA.hashedElGamalEncrypt(publicKey)
        if (result.c1.size != messageSize) {
            throw IllegalStateException("ContestData,encrypt ${result.c1.size} != $messageSize")
        }
        return result
    }
}

// LOOK could add a -1 to indicate removal
fun ContestData.removeOvervotes(bytesToRemove : Int): List<Int> {
    val remove = (bytesToRemove + 2) // assume 1 byte each
    return this.overvotes.subList(0, this.overvotes.size - remove)
}

fun ContestData.chopWriteins(maxlen : Int): List<String> {
    return this.writeIns.map {
        if (it.length <= maxlen) it else it.substring(0, maxlen)
    }
}

fun electionguard.protogen.ContestData.import(): ContestData {
    return ContestData(
        this.overVotes,
        this.writeIns,
        this.status.importContestDataStatus()?: ContestDataStatus.normal,
    )
}

private fun ContestDataStatus.publishContestDataStatus(): electionguard.protogen.ContestData.Status {
    return electionguard.protogen.ContestData.Status.fromName(this.name)
}

private fun electionguard.protogen.ContestData.Status.importContestDataStatus(): ContestDataStatus? {
    val result = safeEnumValueOf<ContestDataStatus>(this.name)
    if (result == null) {
        logger.error { "Vote election type $this has missing or incorrect name" }
    }
    return result
}