package electionguard.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

class HashedElGamalTest {
    @Test
    fun streamCipherWorks() {
        runTest {
            // we need the production mode, not the test mode, because 32-bit keys are too small
            val group =
                productionGroup(
                    acceleration = PowRadixOption.LOW_MEMORY_USE,
                    mode = ProductionMode.Mode3072
                )
            checkAll(
                propTestFastConfig,
                Arb.byteArray(Arb.int(min = 1, max = 5).map { it * 32 }, Arb.byte()),
                elGamalKeypairs(group),
                elementsModQ(group, minimum = 2)
            ) { bytes, kp, nonce ->
                val ciphertext = bytes.hashedElGamalEncrypt(kp, nonce)

                val plaintext = ciphertext.decrypt(kp)
                assertNotNull(plaintext, "decrypt with secret key failed")
                assertContentEquals(bytes, plaintext)

                val noncePlaintext = ciphertext.decryptWithNonce(kp, nonce)
                assertNotNull(noncePlaintext, "decrypt with nonce failed")
                assertContentEquals(bytes, noncePlaintext)

                val beta = kp.publicKey.key powP nonce
                val betaPlaintext = ciphertext.decryptWithBeta(beta)
                assertNotNull(betaPlaintext, "decrypt with beta failed")
                assertContentEquals(bytes, betaPlaintext)
            }
        }
    }
}