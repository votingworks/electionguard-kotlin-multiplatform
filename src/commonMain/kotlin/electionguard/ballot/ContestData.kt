package electionguard.ballot

import electionguard.core.ElGamalPublicKey
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.safeEnumValueOf
import electionguard.protoconvert.publishHashedCiphertext
import mu.KotlinLogging
import pbandk.encodeToByteArray

private val logger = KotlinLogging.logger("ContestData")
private const val BLOCK_SIZE : Int = 32
private const val debug = false

enum class ContestDataStatus {
    normal, null_vote, over_vote, under_vote
}

data class ContestData(
    val overvotes: List<Int>,
    val writeIns: List<String>,
    val status: ContestDataStatus = ContestDataStatus.normal,
) {

    fun publish(filler : String = ""): electionguard.protogen.ContestData {
        return electionguard.protogen
            .ContestData(
                this.status?.publishContestDataStatus() ?: electionguard.protogen.ContestData.Status.NORMAL,
                this.overvotes,
                this.writeIns,
                filler,
            )
    }

    fun publish(status: ContestDataStatus, overvotes: List<Int>, writeIns: List<String>, filler : String = ""): electionguard.protogen.ContestData {
        return electionguard.protogen.ContestData(
                status.publishContestDataStatus(),
                overvotes,
                writeIns,
                filler,
            )
    }

    fun encrypt(publicKey: ElGamalPublicKey, votesAllowed : Int): HashedElGamalCiphertext  {
        if (debug) println("  encrypt = $this votes = $votesAllowed")
        // see how much room we need
        var contestDataProto = publish()
        var contestDataBA = contestDataProto.encodeToByteArray()
        var trialSize = contestDataBA.size
        if (debug) println("  contestDataBA = $trialSize")

        if (trialSize < votesAllowed * BLOCK_SIZE) {
            val filler = StringBuilder().apply{
                repeat(votesAllowed * BLOCK_SIZE - trialSize - 2) {
                    append("*")
                }
            }
            contestDataProto = publish(filler.toString())
            contestDataBA = contestDataProto.encodeToByteArray()
            trialSize = contestDataBA.size
            if (debug) println("  contestDataBA = $trialSize")
        } else if (trialSize > votesAllowed * BLOCK_SIZE) {
            val needToRemove = trialSize - votesAllowed * BLOCK_SIZE
            if (this.writeIns.isNotEmpty()) {
                val writeinChopped = mutableListOf<String>()
                val n = this.writeIns.size
                val chopEach = (needToRemove + n - 1 ) / n
                this.writeIns.forEach { writeinChopped.add( it.substring(0, it.length - chopEach))}

                contestDataProto = publish(this.status!!, overvotes, writeinChopped, "")
                contestDataBA = contestDataProto.encodeToByteArray()
                trialSize = contestDataBA.size
                if (debug) println("  contestDataBA = $trialSize")
            } else {
                val maxover = (votesAllowed * BLOCK_SIZE + 1) / 2
                contestDataProto = publish(this.status!!, overvotes.subList(0, maxover), emptyList(), "")
                contestDataBA = contestDataProto.encodeToByteArray()
                trialSize = contestDataBA.size
                if (debug) println("  contestDataBA = $trialSize")
            }
        }

        // HMAC encryption
        val hashedElGamalEncrypt = contestDataBA.hashedElGamalEncrypt(publicKey)
        if (debug) println("  hashed = ${hashedElGamalEncrypt.c1.size}")
        val hashedProto = hashedElGamalEncrypt.publishHashedCiphertext()
        if (debug) println("  hashedProto = ${hashedProto.c1.array.size}")
        val hashedProtoBA = hashedProto.encodeToByteArray()
        if (debug) println("  hashedProtoBA = ${hashedProtoBA.size}")

        return hashedElGamalEncrypt
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