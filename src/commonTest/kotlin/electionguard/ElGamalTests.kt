package electionguard

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals

private fun smallInts() = Arb.int(min=0, max=1000)

class ElGamalTests {

    @Test
    fun noSmallKeys() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)

            assertThrows<Exception>("0 is too small") {
                elGamalKeyPairFromSecret(0.toElementModQ(context))
            }
            assertThrows<Exception>("1 is too small") {
                elGamalKeyPairFromSecret(1.toElementModQ(context))
            }
            assertDoesNotThrow { elGamalKeyPairFromSecret(2.toElementModQ(context)) }
        }
    }

    @Test
    fun encryptionBasicsLg() {
        encryptionBasics { productionGroup(PowRadixOption.LOW_MEMORY_USE) }
    }

    @Test
    fun encryptionBasicsSm() {
        encryptionBasics { testGroup() }
    }

    fun encryptionBasics(contextF: suspend () -> GroupContext) {
        runTest {
            val context = contextF()
            forAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                smallInts()
            ) { keypair, nonce, message ->
                message == message.encrypt(keypair, nonce).decrypt(keypair)
            }
        }
    }

    @Test
    fun encryptionBasicsAutomaticNonces() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)

            checkAll(propTestFastConfig, elGamalKeypairs(context), smallInts())
                { keypair, message ->
                    val encryption = message.encrypt(keypair)
                    val decryption1 = encryption.decrypt(keypair)
                    val decryption2 = encryption.decrypt(keypair.secretKey)
                    assertEquals(message, decryption1)
                    assertEquals(message, decryption2)
                }
        }
    }

    @Test
    fun decryptWithNonce() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)

            checkAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                smallInts()
            ) { keypair, nonce, message ->
                val encryption = message.encrypt(keypair, nonce)
                val decryption = encryption.decryptWithNonce(keypair.publicKey, nonce)
                assertEquals(message, decryption)
            }
        }
    }

    @Test
    fun homomorphicAccumulation() {
        runTest {
            val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)

            forAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                smallInts(),
                smallInts(),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { keypair, p1, p2, n1, n2 ->
                val c1 = p1.encrypt(keypair, n1)
                val c2 = p2.encrypt(keypair, n2)
                val csum = c1 + c2
                val d = csum.decrypt(keypair)
                p1 + p2 == d
            }
        }
    }
}
