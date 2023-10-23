package electionguard.core

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SchnorrTest {
    @Test
    fun testCorruption() {
        runTest {
            checkAll(
                elGamalKeypairs(tinyGroup()),
                Arb.int(1, 11),
                Arb.int(0, 10),
                elementsModQ(tinyGroup()),
                validResiduesOfP(tinyGroup()),
                elementsModQ(tinyGroup())
            ) { kp, i, j, nonce, fakeElementModP, fakeElementModQ ->
                val goodProof = kp.schnorrProof(i, j, nonce)
                // hp : UInt256, guardianXCoord: Int, coeff: Int
                assertTrue(goodProof.validate(i, j) is Ok)

                val badProof1 = goodProof.copy(challenge = fakeElementModQ)
                val badProof2 = goodProof.copy(response = fakeElementModQ)

                // The generator might have generated replacement values equal to the
                // originals, so we need to be a little bit careful here.

                assertTrue(goodProof.challenge == fakeElementModQ || badProof1.validate(i, j) is Err)
                assertTrue(goodProof.response == fakeElementModQ || badProof2.validate(i, j) is Err)
                assertTrue(kp.publicKey.key == fakeElementModP || badProof2.validate(i, j) is Err)
            }
        }
    }
}