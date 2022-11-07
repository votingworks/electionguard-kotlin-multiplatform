package webapps.electionguard.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

import electionguard.core.productionGroup
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.SecretKeyShare
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

import mu.KotlinLogging
private val logger = KotlinLogging.logger("RemoteGuardianJson")

@Serializable
data class RemoteKeyTrustee(val id: String, val xCoordinate: Int, val quorum: Int) {
    @Transient
    private val delegate = KeyCeremonyTrustee(productionGroup(), id, xCoordinate, quorum)

    fun publicKeys() = delegate.publicKeys()
    fun receivePublicKeys(keys: PublicKeys) = delegate.receivePublicKeys(keys)
    fun secretKeyShareFor(forGuardian: String) = delegate.secretKeyShareFor(forGuardian)
    fun receiveSecretKeyShare(share: SecretKeyShare) = delegate.receiveSecretKeyShare(share)
    fun saveState() = delegate.saveState()
}

val guardians = mutableListOf<RemoteKeyTrustee>()

// LOOK pass this in on command line
val trusteeDir = "/home/snake/tmp/electionguard/RunRemoteKeyCeremonyTest/private_data/trustees"
fun KeyCeremonyTrustee.saveState() : Result<Boolean, String> {
    // store the trustees in some private place.
    val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
    try {
        trusteePublisher.writeTrustee(trusteeDir, this)
        return Ok(true)
    } catch (t : Throwable) {
        logger.atError().setCause(t).log { t.message }
        return Err(t.message ?: "KeyCeremonyTrustee.saveState failed, no message")
    }
}
