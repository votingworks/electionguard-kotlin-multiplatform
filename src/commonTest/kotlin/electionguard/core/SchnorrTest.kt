package electionguard.core

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
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
                assertTrue(goodProof.validate() is Ok)

                val badProof1 = goodProof.copy(challenge = fakeElementModQ)
                val badProof2 = goodProof.copy(response = fakeElementModQ)

                // The generator might have generated replacement values equal to the
                // originals, so we need to be a little bit careful here.

                assertTrue(goodProof.challenge == fakeElementModQ || badProof1.validate() is Err)
                assertTrue(goodProof.response == fakeElementModQ || badProof2.validate() is Err)
                assertTrue(kp.publicKey.key == fakeElementModP || badProof2.validate() is Err)
            }
        }
    }
}