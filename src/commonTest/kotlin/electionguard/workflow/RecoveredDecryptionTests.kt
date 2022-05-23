package electionguard.workflow

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrustee
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlin.test.Test

/** Test KeyCeremony Trustee generation and recovered decryption. */

class RecoveredDecryptionTests {
    @Test
    fun testLagrangeCoefficientAreIntegral() {
        testEncryptRecoveredDecrypt(listOf(1, 2, 3))
        testEncryptRecoveredDecrypt(listOf(1, 2, 3, 4))
        testEncryptRecoveredDecrypt(listOf(2, 3, 4))
        testEncryptRecoveredDecrypt(listOf(2, 4, 5))
        testEncryptRecoveredDecrypt(listOf(2, 3, 4, 5))
        testEncryptRecoveredDecrypt(listOf(5, 6, 7, 8, 9))
        testEncryptRecoveredDecrypt(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        testEncryptRecoveredDecrypt(listOf(2, 3, 4, 5, 6, 7, 9))
        testEncryptRecoveredDecrypt(listOf(2, 3, 4, 5, 6, 9))
        testEncryptRecoveredDecrypt(listOf(2, 3, 4, 5, 6, 7, 11))
        testEncryptRecoveredDecrypt(listOf(2, 3, 4, 7, 11))
    }
}

fun testEncryptRecoveredDecrypt(present: List<Int>) {
    val group = productionGroup()
    val nguardians = present.maxOf { it }
    val quorum = present.count()

    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
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

    val dTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }

    val jointPublicKey: ElementModP =
        dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

    testEncryptRecoveredDecrypt(group, ElGamalPublicKey(jointPublicKey), group.TWO_MOD_Q, dTrustees, present.map {it.toUInt()})
}



