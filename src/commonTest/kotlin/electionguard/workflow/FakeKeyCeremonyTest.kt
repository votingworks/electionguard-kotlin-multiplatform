package electionguard.workflow

import com.github.michaelbull.result.Err
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
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.keyceremony.makeGuardian
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Run a fake KeyCeremony to generate an ElectionInitialized for workflow testing. */
// LOOK can we call RunKeyCeremony instead?
class RunFakeKeyCeremonyTest {

    @Test
    fun runFakeKeyCeremonyAll() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/runFakeKeyCeremonyTest"
        val trusteeDir = "testOut/runFakeKeyCeremonyTest/private_data"

        runFakeKeyCeremony(group, configDir, outputDir, trusteeDir, 3, 3)
    }

    @Test
    fun runFakeKeyCeremonySome() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/runFakeKeyCeremonyTest"
        val trusteeDir = "testOut/runFakeKeyCeremonyTest/private_data"

        runFakeKeyCeremony(group, configDir, outputDir, trusteeDir, 5, 3)
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
    val consumerIn = Consumer(configDir, group)
    val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq, quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        println("keyCeremonyExchange failed = ${exchangeResult}")
    }

    // check they are complete
    trustees.forEach {
        assertEquals(nguardians, it.guardianPublicKeys.size)
        assertEquals(nguardians - 1, it.otherSharesForMe.size)
        assertEquals(nguardians - 1, it.mySharesForOther.size)
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    trustees.forEach { commitments.addAll(it.coefficientCommitments()) }

    val primes = config.constants
    val cryptoBaseHash: UInt256 = hashElements(
        primes.largePrime.toHex(),
        primes.smallPrime.toHex(),
        primes.generator.toHex(),
        nguardians,
        quorum,
        config.manifest.cryptoHash,
    )

    val jointPublicKey: ElementModP =
        trustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    // spec 1.52, eq 17 and 3.B
    val cryptoExtendedBaseHash: UInt256 = hashElements(cryptoBaseHash, jointPublicKey, commitments)

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
        cryptoBaseHash,
        cryptoExtendedBaseHash,
        guardians,
    )
    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionInitialized(init)

    val decryptingTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }
    val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    testEncryptDecrypt(group, ElGamalPublicKey(jointPublicKey), decryptingTrustees)

    return init
}

fun makeDecryptingTrustee(ktrustee: KeyCeremonyTrustee): DecryptingTrustee {
    // val id : String,
    //                             val xCoordinate: UInt,
    //                             val electionKeypair: ElGamalKeypair,
    //                             val guardianPublicKeys: Map<String, PublicKeys>,
    //                             val guardianSecretKeyShares: Map<String, SecretKeyShare>
    return DecryptingTrustee(
        ktrustee.id,
        ktrustee.xCoordinate,
        ElGamalKeypair(
            ElGamalSecretKey(ktrustee.electionPrivateKey()),
            ElGamalPublicKey(ktrustee.electionPublicKey())
        ),
        ktrustee.otherSharesForMe,
    )
}

// check that the public keys are good
fun testEncryptDecrypt(group: GroupContext, publicKey: ElGamalPublicKey, trustees: List<DecryptingTrustee>) {
    val vote = 42
    val evote = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

    //decrypt
    val shares = trustees.map { evote.pad powP it.electionKeypair.secretKey.key }
    val allSharesProductM: ElementModP = with(group) { shares.multP() }
    val decryptedValue: ElementModP = evote.data / allSharesProductM
    val dlogM: Int = publicKey.dLog(decryptedValue) ?: throw RuntimeException("dlog failed")
    assertEquals(42, dlogM)
}