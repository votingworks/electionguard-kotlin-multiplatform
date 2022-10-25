package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.makeDecryptingTrustee
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.productionGroup
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlin.test.Test

/** Test decryption wiyh various combinations of missing guardinas. */

class RecoveredDecryptionTests {
    @Test
    fun testLagrangeCoefficientAreIntegral() {
        testMissingGuardians(listOf(1, 2, 3))
        testMissingGuardians(listOf(1, 2, 3, 4))
        testMissingGuardians(listOf(2, 3, 4))
        testMissingGuardians(listOf(2, 4, 5))
        testMissingGuardians(listOf(2, 3, 4, 5))
        testMissingGuardians(listOf(5, 6, 7, 8, 9))
        testMissingGuardians(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        testMissingGuardians(listOf(2, 3, 4, 5, 6, 7, 9))
        testMissingGuardians(listOf(2, 3, 4, 5, 6, 9))
        testMissingGuardians(listOf(2, 3, 4, 5, 6, 7, 11))
        testMissingGuardians(listOf(2, 3, 4, 7, 11))
    }
}

fun testMissingGuardians(present: List<Int>) {
    val group = productionGroup()
    val nguardians = present.maxOf { it }
    val quorum = present.count()

    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq, quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.sendPublicKeys().unwrap())
        }
    }

    // exchange SecretKeyShares
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id).unwrap())
        }
    }

    val dTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }

    val jointPublicKey: ElementModP =
        dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    testEncryptRecoveredDecrypt(group, ElGamalPublicKey(jointPublicKey), dTrustees, present)
}



