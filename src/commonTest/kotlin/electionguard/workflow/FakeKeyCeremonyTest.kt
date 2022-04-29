@file:OptIn(ExperimentalCli::class)

package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.encrypt
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.ElectionPolynomial
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.generatePolynomial
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ExperimentalCli
import kotlin.test.Test
import kotlin.test.assertEquals

/** Run a fake KeyCeremony to generate an ElectionInitialized for workflow testing. */
class RunFakeKeyCeremonyTest {

    @Test
    fun runFakeKeyCeremonyTest() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/runFakeKeyCeremonyTest"
        val trusteeDir = "testOut/runFakeKeyCeremonyTest/private_data"

        runFakeKeyCeremony(group, configDir, outputDir, trusteeDir, 3, 3)
    }
}

fun runFakeKeyCeremony(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    nguardians: Int,
    quorum: Int,
): ElectionInitialized {
    // just need the manifest
    val electionRecordIn = ElectionRecord(configDir, group)
    val config: ElectionConfig = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    val trustees: List<KeyCeremonyTrustee> = List(nguardians.toInt()) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq.toUInt(), quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.sharePublicKeys())
        }
    }

    // exchange SecretKeyShares
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id))
        }
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    trustees.forEach { commitments.addAll(it.polynomial.coefficientCommitments) }
    val commitmentsHash = hashElements(commitments)

    val primes = config.constants
    val crypto_base_hash: UInt256 = hashElements(
        primes.largePrime.toHex(), // LOOK is this the same as converting to ElementMod ??
        primes.smallPrime.toHex(),
        primes.generator.toHex(),
        nguardians,
        quorum,
        config.manifest.cryptoHash,
    )

    val cryptoExtendedBaseHash: UInt256 = hashElements(crypto_base_hash, commitmentsHash)
    val jointPublicKey: ElementModP =
        trustees.map { it.polynomial.coefficientCommitments[0] }.reduce { a, b -> a * b }

    val newConfig = ElectionConfig(
        config.protoVersion,
        config.constants,
        config.manifest,
        nguardians,
        quorum,
        mapOf(Pair("Created by", "runFakeKeyCeremony")),
    )

    val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
    val init = ElectionInitialized(
        newConfig,
        jointPublicKey,
        config.manifest.cryptoHash,
        cryptoExtendedBaseHash,
        guardians,
    )
    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionInitialized(init)

    val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    return init
}

fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
    val publicKeys = trustee.sharePublicKeys()
    return Guardian(
        trustee.id,
        trustee.xCoordinate,
        publicKeys.coefficientCommitments,
        publicKeys.coefficientProofs,
    )
}

