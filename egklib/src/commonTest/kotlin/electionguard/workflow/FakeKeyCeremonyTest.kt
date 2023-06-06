package electionguard.workflow

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.*
import electionguard.core.Base16.toHex
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.PartialDecryption
import electionguard.decrypt.computeLagrangeCoefficient
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals

/** Run a fake KeyCeremony to generate an ElectionInitialized for workflow testing. */
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
    // just need the manifest from here
    val consumerIn = makeConsumer(configDir, group)
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
        assertEquals(nguardians - 1, it.otherPublicKeys.size)
        assertEquals(quorum, it.coefficientCommitments().size)
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    trustees.forEach {
        commitments.addAll(it.coefficientCommitments())
        println("trustee ${it.id}")
        it.coefficientCommitments().forEach { println("   ${it.toStringShort()}") }
    }
    assertEquals(quorum * nguardians, commitments.size)

    val jointPublicKey: ElementModP =
        trustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }
    println("jointPublicKey ${jointPublicKey.toStringShort()}")

    // create a new config so the quorum, nguardians can change
    val newConfig = ElectionConfig(
        protocolVersion,
        config.constants,
        config.manifestFile,
        config.manifest,
        nguardians,
        quorum,
        config.electionDate,
        config.jurisdictionInfo,
        mapOf(Pair("Created by", "runFakeKeyCeremony")),
    )
    println("newConfig.electionBaseHash ${newConfig.electionBaseHash}")

    // He = H(HB ; 12, K, K1,0 , K1,1 , . . . , K1,k−1 , K2,0 , . . . , Kn,k−2 , Kn,k−1 ) spec 1.9 p.22, eq 20.
    val He = hashFunction(newConfig.electionBaseHash.bytes, 0x12.toByte(), jointPublicKey, commitments)

    val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
    val init = ElectionInitialized(
        newConfig,
        jointPublicKey,
        He,
        guardians,
    )
    val publisher = makePublisher(outputDir)
    publisher.writeElectionInitialized(init)

    val decryptingTrustees: List<DecryptingTrusteeDoerre> = trustees.map { makeDoerreTrustee(it) }
    val trusteePublisher = makePublisher(trusteeDir)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    testDoerreDecrypt(group, ElGamalPublicKey(jointPublicKey), decryptingTrustees, decryptingTrustees.map {it.xCoordinate})

    return init
}

fun testDoerreDecrypt(group: GroupContext,
                      publicKey: ElGamalPublicKey,
                      trustees: List<DecryptingTrusteeDoerre>,
                      present: List<Int>) {
    val missing = trustees.filter {!present.contains(it.xCoordinate())}.map { it.id }
    println("present $present, missing $missing")
    val vote = 42
    val evote = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

    val available = trustees.filter {present.contains(it.xCoordinate())}
    val lagrangeCoefficients = available.associate { it.id to group.computeLagrangeCoefficient(it.xCoordinate, present) }

    val shares: List<PartialDecryption> = available.map {
        it.decrypt(group, listOf(evote.pad))[0]
    }

    val weightedProduct = with(group) {
        shares.map {
            val coeff = lagrangeCoefficients[it.guardianId] ?: throw IllegalArgumentException()
            it.mbari powP coeff
        }.multP() // eq 7
    }
    val bm = evote.data / weightedProduct
    val expected = publicKey powP vote.toElementModQ(group)
    assertEquals(expected, bm)

    val dlogM: Int = publicKey.dLog(bm, 100) ?: throw RuntimeException("dlog failed")
    println("The answer is $dlogM")
    assertEquals(42, dlogM)
}

/* check that the public keys are good
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

 */