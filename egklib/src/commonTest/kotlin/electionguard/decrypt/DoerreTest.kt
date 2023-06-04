package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.makeDoerreTrustee

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.encrypt
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val group = productionGroup()
private val configDir = "src/commonTest/data/start"
private val outputDir = "testOut/RecoveredDecryptionTest"
private val trusteeDir = "testOut/RecoveredDecryptionTest/private_data"

/** Test KeyCeremony Trustee generation and recovered decryption. */
class DoerreTest {

    @Test
    fun testEncryptDecrypt() {
        runDoerreTest( 1, 1, listOf(1))
        runDoerreTest( 2, 2, listOf(1, 2))
        runDoerreTest( 3, 3, listOf(1,2,3)) // all
        runDoerreTest(5, 5, listOf(1,2,3,4,5)) // all
        runDoerreTest(5, 3, listOf(2,3,4)) // quota
        runDoerreTest(5, 3, listOf(1,2,3,4)) // between
    }
}

fun runDoerreTest(
    nguardians: Int,
    quorum: Int,
    present: List<Int>,
) {
    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group,"guardian$seq", seq, quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.publicKeys().unwrap())
        }
    }

    // exchange SecretKeyShares
    trustees.forEach { t1 ->
        trustees.filter { it.id != t1.id }.forEach { t2 ->
            t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.id).unwrap())
        }
    }

    val dTrustees: List<DecryptingTrusteeDoerre> = trustees.map { makeDoerreTrustee(it) }

    val jointPublicKey: ElementModP =
        dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    testDoerreDecrypt(group, ElGamalPublicKey(jointPublicKey), dTrustees, present)

    /*
    if (writeout) {
        val commitments: MutableList<ElementModP> = mutableListOf()
        trustees.forEach { commitments.addAll(it.coefficientCommitments()) }
        val commitmentsHash = hashElements(commitments)

        val consumerIn = makeConsumer(configDir, group)
        val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

        val primes = config.constants
        val cryptoBaseHash: UInt256 = hashElements(
            primes.largePrime.toHex(),
            primes.smallPrime.toHex(),
            primes.generator.toHex(),
            config.numberOfGuardians,
            config.quorum,
            config.manifest.cryptoHash,
        )

        // spec 1.52, eq 17 and 3.B
        val cryptoExtendedBaseHash: UInt256 = hashElements(cryptoBaseHash, jointPublicKey, commitmentsHash)
        val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
        val init = ElectionInitialized(
            config,
            jointPublicKey,
            config.manifest.cryptoHash,
            cryptoBaseHash,
            cryptoExtendedBaseHash,
            guardians,
        )
        val publisher = makePublisher(outputDir)
        publisher.writeElectionInitialized(init)

        val trusteePublisher = makePublisher(trusteeDir)
        trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }
    }

     */
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




