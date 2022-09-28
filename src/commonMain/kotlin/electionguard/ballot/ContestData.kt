package electionguard.ballot

import electionguard.core.safeEnumValueOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ContestData")

enum class ContestDataStatus {
    normal, null_vote, under_vote
}

data class ContestData(
    val overvotes: List<Int>,
    val writeIns: List<String>,
    val status: ContestDataStatus? = ContestDataStatus.normal,
) {

    fun publish(): electionguard.protogen.ContestData {
        return electionguard.protogen
            .ContestData(
                this.status?.publishContestDataState() ?: electionguard.protogen.ContestData.Status.NORMAL,
                this.overvotes,
                this.writeIns,
            )
    }
}

fun electionguard.protogen.ContestData.import(): ContestData {
    return ContestData(
        this.overVotes,
        this.writeIns,
        this.vote.importContestDataState(),
    )
}

private fun ContestDataStatus.publishContestDataState(): electionguard.protogen.ContestData.Status {
    return electionguard.protogen.ContestData.Status.fromName(this.name)
}

private fun electionguard.protogen.ContestData.Status.importContestDataState(): ContestDataStatus? {
    val result = safeEnumValueOf<ContestDataStatus>(this.name)
    if (result == null) {
        logger.error { "Vote election type $this has missing or incorrect name" }
    }
    return result
}