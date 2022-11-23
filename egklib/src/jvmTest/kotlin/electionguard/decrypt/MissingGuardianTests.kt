package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.makeDecryptingTrustee
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.productionGroup
import electionguard.keyceremony.KeyCeremonyTrustee

/** Test decryption wiyh various combinations of missing guardinas. */

class MissingGuardianTests {

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

    /*
fun params() : Stream<Arguments> = Stream.of(
            Arguments.of(100.0, -100.0, 0.0),
            Arguments.of(-100.0, 100.0, 360.0),
            Arguments.of(-100.0, -180.0, 0.0),
            Arguments.of(-180.0, -100.0, 360.0),
            Arguments.of(-180.0, 180.0, 360.0),
            Arguments.of(181.0, -180.0, -360.0),
            Arguments.of(181.0, -200.0, -360.0),
            Arguments.of(-200.0, 200.0, 720.0),
            Arguments.of(-179.0, 180.0, 360.0)
        )


        @ParameterizedTest
        @MethodSource("params")
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


    @ParameterizedTest
    @MethodSource("params")

     */

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
                    t1.receivePublicKeys(t2.publicKeys().unwrap())
                }
            }

            // exchange SecretKeyShares
            trustees.forEach { t1 ->
                trustees.filter { it.id != t1.id }.forEach { t2 ->
                    t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.id).unwrap())
                }
            }

            val dTrustees: List<DecryptingTrustee> = trustees.map { makeDecryptingTrustee(it) }

            val jointPublicKey: ElementModP =
                dTrustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }

            testEncryptRecoveredDecrypt(group, ElGamalPublicKey(jointPublicKey), dTrustees, present)
        }

    }



