package webapps.electionguard.models

import electionguard.core.ElementModP
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.makeConsumer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KotlinLogging
import webapps.electionguard.groupContext

private val logger = KotlinLogging.logger("DecryptingTrusteeJson")

@Serializable
data class RemoteDecryptingTrusteeJson(val trustee_dir: String, val guardian_id: String) {
    @Transient
    private val delegate = readState(trustee_dir, guardian_id)

    fun id() = guardian_id
    fun xCoordinate() = delegate.xCoordinate()

    fun decrypt(texts: List<ElementModP>) = delegate.decrypt(groupContext, texts)
    fun challenge(challenges: List<ChallengeRequest>) = delegate.challenge(groupContext, challenges)
}

fun readState(trusteeDir : String, guardianId: String) : DecryptingTrusteeIF {
    val consumer = makeConsumer(trusteeDir, groupContext)
    try {
        return consumer.readTrustee(trusteeDir, guardianId)
    } catch (t: Throwable) {
        logger.atError().setCause(t).log(" readState failed ${t.message}")
        throw RuntimeException(t)
    }
}
