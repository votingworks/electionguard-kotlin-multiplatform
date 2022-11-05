package webapps.electionguard.models

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.Consumer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import webapps.electionguard.groupContext
import webapps.electionguard.trusteeDir

private val logger = KotlinLogging.logger("DecryptingTrusteeJson")

@Serializable
data class RemoteDecryptingTrusteeJson(val guardian_id: String) {
    @Transient
    private val delegate = readState(guardian_id)

    fun id() = guardian_id
    fun xCoordinate() = delegate.xCoordinate()

    fun setMissing(coeff : ElementModQ, missingGuardians: List<String>) = delegate.setMissing(groupContext, coeff, missingGuardians)
    fun decrypt(texts: List<ElementModP>) = delegate.decrypt(groupContext, texts, null)
    fun challenge(challenges: List<ChallengeRequest>) = delegate.challenge(groupContext, challenges)
}

val trustees = mutableListOf<RemoteDecryptingTrusteeJson>()

fun readState(guardianId: String) : DecryptingTrusteeIF {
    val consumer = Consumer(trusteeDir, groupContext)
    try {
        return consumer.readTrustee(trusteeDir, guardianId)
    } catch (t: Throwable) {
        logger.atError().setCause(t).log(" readState failed ${t.message}")
        throw RuntimeException(t)
    }
}
