package electionguard.workflow

import com.github.michaelbull.result.Err
import electionguard.ballot.*
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.PartialDecryption
import electionguard.decrypt.computeLagrangeCoefficient
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Run a fake KeyCeremony to generate an ElectionInitialized for workflow testing. */
class RunFakeKeyCeremonyTest {

    @Test
    fun runFakeKeyCeremonyAll() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/startConfigJson"
        val outputDir = "testOut/keyceremony/runFakeKeyCeremonyAll"
        val trusteeDir = "$outputDir/private_data/trustees"

        runFakeKeyCeremony(group, configDir, outputDir, trusteeDir, 3, 3, false)
    }

    @Test
    fun runFakeKeyCeremonySome() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/startConfigJson"
        val outputDir = "testOut/keyceremony/runFakeKeyCeremonySome"
        val trusteeDir = "$outputDir/private_data/trustees"

        runFakeKeyCeremony(group, configDir, outputDir, trusteeDir, 5, 3, false)
    }
}

fun runFakeKeyCeremony(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    nguardians: Int,
    quorum: Int,
    chained: Boolean,
): Pair<Manifest, ElectionInitialized> {
    val electionRecord = readElectionRecord(group, configDir)
    val config: ElectionConfig = electionRecord.config().copy(chainConfirmationCodes = chained)

    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq, nguardians, quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        println("keyCeremonyExchange error = ${exchangeResult}")
    }

    // check they are complete
    trustees.forEach {
        assertTrue( it.isComplete() )
        assertEquals(quorum, it.coefficientCommitments().size)
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    trustees.forEach {
        commitments.addAll(it.coefficientCommitments())
        // it.coefficientCommitments().forEach { println("   ${it.toStringShort()}") }
    }
    assertEquals(quorum * nguardians, commitments.size)

    val jointPublicKey: ElementModP =
        trustees.map { it.guardianPublicKey() }.reduce { a, b -> a * b }

    // create a new config so the quorum, nguardians can change
    val newConfig = makeElectionConfig(
        protocolVersion,
        config.constants,
        nguardians,
        quorum,
        electionRecord.manifestBytes(),
        chained,
        config.configBaux0,
        mapOf(Pair("Created by", "runFakeKeyCeremony")),
    )
    // println("newConfig.electionBaseHash ${newConfig.electionBaseHash}")

    // He = H(HB ; 0x12, K) ; spec 2.0.0 p.25, eq 23.
    val He = electionExtendedHash(newConfig.electionBaseHash, jointPublicKey)

    val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
    val init = ElectionInitialized(
        newConfig,
        jointPublicKey,
        He,
        guardians,
    )
    val publisher = makePublisher(outputDir, false, electionRecord.isJson())
    publisher.writeElectionInitialized(init)

    val trusteePublisher = makePublisher(trusteeDir, false, electionRecord.isJson())
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    // val decryptingTrustees: List<DecryptingTrusteeDoerre> = trustees.map { makeDoerreTrustee(it) }
    // testDoerreDecrypt(group, ElGamalPublicKey(jointPublicKey), decryptingTrustees, decryptingTrustees.map {it.xCoordinate})

    return Pair(electionRecord.manifest(), init)
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
            it.Mi powP coeff
        }.multP() // eq 7
    }
    val bm = evote.data / weightedProduct
    val expected = publicKey powP vote.toElementModQ(group)
    assertEquals(expected, bm)

    val dlogM: Int = publicKey.dLog(bm, 100) ?: throw RuntimeException("dlog error")
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
    val dlogM: Int = publicKey.dLog(decryptedValue) ?: throw RuntimeException("dlog error")
    assertEquals(42, dlogM)
}

 */