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
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ExperimentalCli
import kotlin.test.Test
import kotlin.test.assertEquals

/** Run a fake KeyCeremony and generate an ElectionInitialized and DecryptingTrustees for workflow testing. */

class FakeKeyCeremonyTrusteeTest {
    @Test
    fun runFakeKeyCeremonyTrusteeTest() {
        val group = productionGroup()
        val configDir = "src/commonTest/data/start"
        val outputDir = "testOut/FakeKeyCeremonyTrusteeTest"
        val trusteeDir = "testOut/FakeKeyCeremonyTrusteeTest/private_data"

        runFakeKeyCeremonyWithTrustees(group, configDir, outputDir, trusteeDir)
    }

    fun runFakeKeyCeremonyWithTrustees(
        group: GroupContext,
        configDir: String,
        outputDir: String,
        trusteeDir: String,
    ) {
        val electionRecordIn = ElectionRecord(configDir, group)
        val config: ElectionConfig = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

        // class KeyCeremonyTrustee(
        //    val group: GroupContext,
        //    val id: String,
        //    val xCoordinate: UInt,
        //    val quorum: Int,
        val trustees: List<KeyCeremonyTrustee> = List(config.numberOfGuardians) {
            val seq = it + 1
            KeyCeremonyTrustee(group, "KeyCeremonyTrustee$seq", seq.toUInt(), config.quorum)
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
        val cryptoBaseHash: UInt256 = hashElements(
            primes.largePrime.toHex(), // LOOK is this the same as converting to ElementMod ??
            primes.smallPrime.toHex(),
            primes.generator.toHex(),
            config.numberOfGuardians,
            config.quorum,
            config.manifest.cryptoHash,
        )

        val cryptoExtendedBaseHash: UInt256 = hashElements(cryptoBaseHash, commitmentsHash)
        val jointPublicKey: ElementModP =
            trustees.map { it.polynomial.coefficientCommitments[0] }.reduce { a, b -> a * b }
        val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
        val init = ElectionInitialized(
            config,
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
    }

    private fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
        val publicKeys = trustee.sharePublicKeys()
        return Guardian(
            "guardian${trustee.xCoordinate}",
            trustee.xCoordinate,
            publicKeys.coefficientCommitments,
            publicKeys.coefficientProofs,
        )
    }

    // private
    private fun makeDecryptingTrustee(ktrustee: KeyCeremonyTrustee): DecryptingTrustee {
        // val id : String,
        //                             val xCoordinate: UInt,
        //                             val electionKeypair: ElGamalKeypair,
        //                             val guardianPublicKeys: Map<String, PublicKeys>,
        //                             val guardianSecretKeyShares: Map<String, SecretKeyShare>
        return DecryptingTrustee(
            "guardian${ktrustee.xCoordinate}",
            ktrustee.xCoordinate,
            ElGamalKeypair(
                ElGamalSecretKey(ktrustee.polynomial.coefficients[0]),
                ElGamalPublicKey(ktrustee.polynomial.coefficientCommitments[0])
            ),
            ktrustee.guardianSecretKeyShares,
            ktrustee.guardianPublicKeys.entries.associate { it.key to it.value.coefficientCommitments },
        )
    }

    fun testEncryptDecrypt(group: GroupContext, publicKey: ElGamalPublicKey, trustees: List<DecryptingTrustee>) {
        val vote = 42
        val evote = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

        //decrypt
        val shares = trustees.map { evote.pad powP it.electionKeypair.secretKey.key }
        val allSharesProductM: ElementModP = with(group) { shares.multP() }
        val decryptedValue: ElementModP = evote.data / allSharesProductM
        val dlogM: Int = publicKey.dLog(decryptedValue) ?: throw RuntimeException("dlog failed")
        assertEquals(42, dlogM)
        println("The answer is $dlogM")
    }
}

