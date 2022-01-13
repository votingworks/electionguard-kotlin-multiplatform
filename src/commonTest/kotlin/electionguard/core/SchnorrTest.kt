package electionguard.core

import io.kotest.property.checkAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SchnorrTest {
    @BeforeTest
    fun suppressLogs() {
        loggingErrorsOnly()
    }

    @Test
    fun testCorruption() {
        runTest {
            checkAll(
                elGamalKeypairs(tinyGroup()),
                elementsModQ(tinyGroup()),
                validElementsModP(tinyGroup()),
                elementsModQ(tinyGroup())
            ) { kp, n, fakeElementModP, fakeElementModQ ->
                val goodProof = kp.schnorrProof(n)
                assertTrue(kp.publicKey.hasValidSchnorrProof(goodProof))

                val badProof1 = goodProof.copy(publicKey = fakeElementModP)
                val badProof2 = goodProof.copy(commitment = fakeElementModP)
                val badProof3 = goodProof.copy(challenge = fakeElementModQ)
                val badProof4 = goodProof.copy(response = fakeElementModQ)

                // The generator might have generated replacement values equal to the
                // originals, so we need to be a little bit careful here.

                assertTrue(
                    goodProof.publicKey == fakeElementModP ||
                        !kp.publicKey.hasValidSchnorrProof(badProof1)
                )
                assertTrue(
                    goodProof.commitment == fakeElementModP ||
                        !kp.publicKey.hasValidSchnorrProof(badProof2)
                )
                assertTrue(
                    goodProof.challenge == fakeElementModQ ||
                        !kp.publicKey.hasValidSchnorrProof(badProof3)
                )
                assertTrue(
                    goodProof.response == fakeElementModQ ||
                        !kp.publicKey.hasValidSchnorrProof(badProof4)
                )
            }
        }
    }
}