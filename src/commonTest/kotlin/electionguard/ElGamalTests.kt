package electionguard

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.Test
import kotlin.test.assertEquals

private fun smallInts() = Arb.int(min=0, max=1000)

class ElGamalTests {
    val context = productionGroup(PowRadixOption.LOW_MEMORY_USE)

    @Test
    fun noSmallKeys() {
        assertThrows<Exception>("0 is too small") {
            elGamalKeyPairFromSecret(0.toElementModQ(context))
        }
        assertThrows<Exception>("1 is too small") {
            elGamalKeyPairFromSecret(1.toElementModQ(context))
        }
        assertDoesNotThrow { elGamalKeyPairFromSecret(2.toElementModQ(context)) }
    }

    @Test
    fun encryptionBasics() {
        runProperty {
            forAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                smallInts()
            ) { keypair, nonce, message ->
                message == keypair.decrypt(keypair.encrypt(message, nonce))
            }
        }
    }

    @Test
    fun encryptionBasicsAutomaticNonces() {
        runProperty {
            checkAll(propTestFastConfig, elGamalKeypairs(context), smallInts())
                { keypair, message ->
                    val encryption = keypair.encrypt(message)
                    val decryption1 = keypair.decrypt(encryption)
                    val decryption2 = keypair.secretKey.decrypt(encryption)
                    val decryption3 = encryption.decrypt(keypair)
                    val decryption4 = encryption.decrypt(keypair.secretKey)
                    assertEquals(message, decryption1)
                    assertEquals(message, decryption2)
                    assertEquals(message, decryption3)
                    assertEquals(message, decryption4)
                }
        }
    }

    @Test
    fun decryptWithNonce() {
        runProperty {
            checkAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                smallInts()
            ) { keypair, nonce, message ->
                val encryption = keypair.encrypt(message, nonce)
                val decryption0 = keypair.decrypt(encryption)
                val decryption1 = keypair.decryptWithNonce(encryption, nonce)
                val decryption2 = keypair.publicKey.decryptWithNonce(encryption, nonce)
                val decryption3 = encryption.decryptWithNonce(keypair.publicKey, nonce)
                assertEquals(message, decryption0)
                assertEquals(message, decryption1)
                assertEquals(message, decryption2)
                assertEquals(message, decryption3)
            }
        }
    }

    @Test
    fun homomorphicAccumulation() {
        runProperty {
            forAll(
                propTestFastConfig,
                elGamalKeypairs(context),
                smallInts(),
                smallInts(),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { keypair, p1, p2, n1, n2 ->
                val c1 = keypair.encrypt(p1, n1)
                val c2 = keypair.encrypt(p2, n2)
                val csum = c1 + c2
                val d = keypair.decrypt(csum)
                p1 + p2 == d
            }
        }
    }
}
