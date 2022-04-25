package electionguard.workflow

import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.PowRadixOption
import electionguard.core.elGamalKeyPairFromRandom
import electionguard.core.encrypt
import electionguard.core.encryptedSum
import electionguard.core.computeShare
import electionguard.core.decryptWithShares
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.core.toElementModQ
import kotlin.test.Test
import kotlin.test.assertEquals

/** test basic workflow: encrypt, accumulate, decrypt */
class WorkflowTest {
    @Test
    fun singleTrusteeZero() {
        runTest {
            val group = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val secret = group.randomElementModQ(minimum = 1)
            val publicKey = ElGamalPublicKey(group.gPowP(secret))
            val keypair = ElGamalKeypair(ElGamalSecretKey(secret), publicKey)
            val nonce = group.randomElementModQ(minimum = 1)

            // acumulate random sequence of 1 and 0
            val vote = 0
            val evote = vote.encrypt(publicKey, nonce)
            assertEquals(group.gPowP(nonce), evote.pad)
            val oneOrG = group.gPowP(vote.toElementModQ(group))
            val expectedData = oneOrG * publicKey.key powP nonce
            assertEquals(expectedData, evote.data)

            //decrypt
            val partialDecryption = evote.computeShare(keypair.secretKey)
            val decryptedValue: ElementModP = evote.data / partialDecryption
            val m = publicKey.dLog(decryptedValue)
            assertEquals(0, m)
        }
    }

    @Test
    fun singleTrusteeOne() {
        runTest {
            val group = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val secret = group.randomElementModQ(minimum = 1)
            val publicKey = ElGamalPublicKey(group.gPowP(secret))
            val keypair = ElGamalKeypair(ElGamalSecretKey(secret), publicKey)
            val nonce = group.randomElementModQ(minimum = 1)

            // acumulate random sequence of 1 and 0
            val vote = 1
            val evote = vote.encrypt(publicKey, nonce)
            assertEquals(group.gPowP(nonce), evote.pad)
            val expectedData = publicKey.key powP (nonce + vote.toElementModQ(group))
            assertEquals(expectedData, evote.data)

            //decrypt
            val partialDecryption = evote.computeShare(keypair.secretKey)
            val decryptedValue: ElementModP = evote.data / partialDecryption
            val m = publicKey.dLog(decryptedValue)
            assertEquals(1, m)
        }
    }

    @Test
    fun singleTrusteeTally() {
        runTest {
            val group = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val secret = group.randomElementModQ(minimum = 1)
            val publicKey = ElGamalPublicKey(group.gPowP(secret))
            assertEquals(group.gPowP(secret), publicKey.key)
            val keypair = ElGamalKeypair(ElGamalSecretKey(secret), publicKey)

            val vote = 1
            val evote1 = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))
            val evote2 = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))
            val evote3 = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

            val accum = listOf(evote1, evote2, evote3)
            val eAccum = accum.encryptedSum()

            //decrypt
            val partialDecryption = eAccum.computeShare(keypair.secretKey)
            val decryptedValue: ElementModP = eAccum.data / partialDecryption
            val m = publicKey.dLog(decryptedValue)
            assertEquals(3, m)
        }
    }

    @Test
    fun multipleTrustees() {
        runTest {
            val group = productionGroup(PowRadixOption.LOW_MEMORY_USE)
            val trustees = listOf(
                elGamalKeyPairFromRandom(group),
                elGamalKeyPairFromRandom(group),
                elGamalKeyPairFromRandom(group),
            )
            val pkeys: Iterable<ElementModP> = trustees.map { it.publicKey.key}
            val publicKey = ElGamalPublicKey(with (group) { pkeys.multP()} )

            val vote = 1
            val evote1 = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))
            val evote2 = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))
            val evote3 = vote.encrypt(publicKey, group.randomElementModQ(minimum = 1))

            // tally
            val accum = listOf(evote1, evote2, evote3)
            val eAccum = accum.encryptedSum()

            //decrypt
            val shares = trustees.map { eAccum.pad powP it.secretKey.key }
            val allSharesProductM: ElementModP = with (group) { shares.multP() }
            val decryptedValue: ElementModP = eAccum.data / allSharesProductM
            val dlogM: Int = publicKey.dLog(decryptedValue)?: throw RuntimeException("dlog failed") // TODO on fail
            assertEquals(3, dlogM)

            //decrypt2
            val dlogM2 = eAccum.decryptWithShares(publicKey, shares)
            assertEquals(3, dlogM2)
        }
    }
}