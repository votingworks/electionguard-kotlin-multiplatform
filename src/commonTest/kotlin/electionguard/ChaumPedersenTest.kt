package electionguard

import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.generators.SourceDSL.integers

private const val MAX_TEST_TIME_SECONDS = 5L

class ChaumPedersenTest {
    @Test
    fun testCCKnownNonceProofsSimpleEncryptionZero() {
        val keypair = elGamalKeyPairFromSecret(TWO_MOD_Q)
        val nonce = ONE_MOD_Q
        val seed = TWO_MOD_Q
        val message = keypair.encrypt(0, nonce)
        val badMessage1 = keypair.encrypt(1, nonce)
        val badMessage2 = keypair.encrypt(0, TWO_MOD_Q)
        val hashHeader = ONE_MOD_Q
        val badHashHeader = TWO_MOD_Q
        val goodProof =
            message.constantChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof1 =
            message.constantChaumPedersenProofKnownNonce(
                1,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof2 =
            badMessage1.constantChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof3 =
            badMessage2.constantChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )

        assertTrue(goodProof.isValid(message, keypair.publicKey, hashHeader))
        assertFalse(goodProof.isValid(message, keypair.publicKey, badHashHeader))
        assertFalse(badProof1.isValid(message, keypair.publicKey, hashHeader))
        assertFalse(badProof2.isValid(message, keypair.publicKey, hashHeader))
        assertFalse(badProof3.isValid(message, keypair.publicKey, hashHeader))
    }

    @Test
    fun testCCKnownSecretProofsSimpleEncryptionZero() {
        val keypair = elGamalKeyPairFromSecret(TWO_MOD_Q)
        val nonce = ONE_MOD_Q
        val seed = TWO_MOD_Q
        val message = keypair.encrypt(0, nonce)
        val badMessage1 = keypair.encrypt(1, nonce)
        val badMessage2 = keypair.encrypt(0, TWO_MOD_Q)
        val hashHeader = ONE_MOD_Q
        val badHashHeader = TWO_MOD_Q
        val goodProof =
            message.constantChaumPedersenProofKnownSecretKey(0, keypair, seed, hashHeader)
        val badProof1 =
            message.constantChaumPedersenProofKnownSecretKey(1, keypair, seed, hashHeader)
        val badProof2 =
            badMessage1.constantChaumPedersenProofKnownSecretKey(0, keypair, seed, hashHeader)
        val badProof3 =
            badMessage2.constantChaumPedersenProofKnownSecretKey(0, keypair, seed, hashHeader)

        assertTrue(goodProof.isValid(message, keypair.publicKey, hashHeader, expectedConstant = 0))
        assertFalse(goodProof.isValid(message, keypair.publicKey, hashHeader, expectedConstant = 1))
        assertFalse(goodProof.isValid(message, keypair.publicKey, badHashHeader))
        assertFalse(badProof1.isValid(message, keypair.publicKey, hashHeader))
        assertFalse(badProof2.isValid(message, keypair.publicKey, hashHeader))
        assertFalse(badProof3.isValid(message, keypair.publicKey, hashHeader))
    }

    @Test
    fun testCCProofsKnownNonce() {
        qt().withTestingTime(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS)
            .forAll(
                tuples(
                    elGamalKeypairs("key"),
                    elementsModQNoZero("nonce"),
                    elementsModQ("seed"),
                    integers().between(0, 100),
                    integers().between(0, 100)
                )
            )
            .checkAssert { (keypair, nonce, seed, constant, constant2) ->
                // we need the constants to be different
                val badConstant = if (constant == constant2) constant + 1 else constant2

                val message = keypair.encrypt(constant, nonce)
                val badMessage = keypair.encrypt(badConstant, nonce)

                val proof =
                    message.constantChaumPedersenProofKnownNonce(
                        plaintext = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        hashHeader = ONE_MOD_Q
                    )
                assertTrue(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        ONE_MOD_Q,
                        expectedConstant = constant
                    )
                )
                assertFalse(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        ONE_MOD_Q,
                        expectedConstant = constant + 1
                    )
                )
                assertFalse(proof.isValid(badMessage, keypair.publicKey, ONE_MOD_Q))

                val badProof =
                    badMessage.constantChaumPedersenProofKnownNonce(
                        plaintext = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        hashHeader = ONE_MOD_Q
                    )
                assertFalse(badProof.isValid(badMessage, keypair.publicKey, ONE_MOD_Q))

                val badProof2 =
                    message.constantChaumPedersenProofKnownNonce(
                        plaintext = badConstant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        hashHeader = ONE_MOD_Q
                    )
                assertFalse(badProof2.isValid(message, keypair.publicKey, ONE_MOD_Q))

                val badProof3 = proof.copy(constant = Int.MAX_VALUE)
                assertFalse(badProof3.isValid(message, keypair.publicKey, ONE_MOD_Q))
            }
    }

    @Test
    fun testCCProofsKnownSecretKey() {
        qt().withTestingTime(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS)
            .forAll(
                tuples(
                    elGamalKeypairs("key"),
                    elementsModQNoZero("nonce"),
                    elementsModQ("seed"),
                    integers().between(0, 100),
                    integers().between(0, 100)
                )
            )
            .checkAssert { (keypair, nonce, seed, constant, constant2) ->
                // we need the constants to be different
                val badConstant = if (constant == constant2) constant + 1 else constant2

                val message = keypair.encrypt(constant, nonce)
                val badMessage = keypair.encrypt(badConstant, nonce)

                val proof =
                    message.constantChaumPedersenProofKnownSecretKey(
                        constant,
                        keypair,
                        seed,
                        ONE_MOD_Q
                    )
                assertTrue(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        ONE_MOD_Q,
                        expectedConstant = constant
                    )
                )
                assertTrue(proof.isValid(message, keypair.publicKey, ONE_MOD_Q))
                assertFalse(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        ONE_MOD_Q,
                        expectedConstant = badConstant
                    )
                )
                assertFalse(proof.isValid(badMessage, keypair.publicKey, ONE_MOD_Q))

                val badProof =
                    badMessage.constantChaumPedersenProofKnownSecretKey(
                        constant,
                        keypair,
                        seed,
                        ONE_MOD_Q
                    )
                assertFalse(badProof.isValid(badMessage, keypair.publicKey, ONE_MOD_Q))

                val badProof2 =
                    message.constantChaumPedersenProofKnownSecretKey(
                        badConstant,
                        keypair,
                        seed,
                        ONE_MOD_Q
                    )
                assertFalse(badProof2.isValid(message, keypair.publicKey, ONE_MOD_Q))

                val badProof3 = proof.copy(constant = Int.MAX_VALUE)
                assertFalse(badProof3.isValid(message, keypair.publicKey, ONE_MOD_Q))
            }
    }

    @Test
    fun testGcpProof() {
        qt().withTestingTime(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS)
            .forAll(
                tuples(
                    elementsModQ("q1"),
                    elementsModQ("q2"),
                    elementsModQ("x"),
                    elementsModQ("x2"),
                    elementsModQ("seed"),
                    elementsModQ("hashHeader")
                )
            )
            .checkAssert { (q1, q2, x, x2, seed, hashHeader) ->
                // we need x != notx, and using assuming() would slow us down
                val notx = if (x == x2) x + ONE_MOD_Q else x2
                helperTestGcp(q1, q2, x, notx, seed, hashHeader)
            }
    }

    @Test
    fun testGcpProofSimple() {
        helperTestGcp(TWO_MOD_Q, 3.toElementModQ(), 5.toElementModQ(), TWO_MOD_Q, ZERO_MOD_Q, null)
        helperTestGcp(ONE_MOD_Q, ONE_MOD_Q, ZERO_MOD_Q, ONE_MOD_Q, ZERO_MOD_Q, null)
    }

    private fun helperTestGcp(
        q1: ElementModQ,
        q2: ElementModQ,
        x: ElementModQ,
        notx: ElementModQ,
        seed: ElementModQ,
        hashHeader: ElementModQ?
    ) {
        val g = gPowP(q1)
        val h = gPowP(q2)
        val gx = g powP x
        val hx = h powP x
        val gnotx = g powP notx
        val hnotx = h powP notx

        val hashHeaderX = hashHeader ?: ZERO_MOD_Q

        val proof = genericChaumPedersenProofOf(g, h, x, seed, hashHeader = hashHeaderX)
        assertTrue(proof.isValid(g, gx, h, hx, hashHeader = hashHeaderX))

        if (gx != gnotx && hx != hnotx) {
            // In the degenerate case where q1 or q2 == 0, then we'd have a problem:
            // g = 1, gx = 1, and gnotx = 1. Same thing for h, hx, hnotx. This means
            // swapping in gnotx for gx doesn't actually do anything.

            assertFalse(proof.isValid(g, gnotx, h, hx, hashHeader = hashHeaderX))
            assertFalse(proof.isValid(g, gx, h, hnotx, hashHeader = hashHeaderX))
            assertFalse(proof.isValid(g, gnotx, h, hnotx, hashHeader = hashHeaderX))
        }
    }

    @Test
    fun testDisjunctiveProofKnownNonceSimple() {
        val keypair = elGamalKeyPairFromSecret(TWO_MOD_Q)
        val nonce = ONE_MOD_Q
        val seed = TWO_MOD_Q
        val message0 = keypair.encrypt(0, nonce)
        val message1 = keypair.encrypt(1, nonce)
        val badMessage0 = keypair.encrypt(0, TWO_MOD_Q)
        val badMessage1 = keypair.encrypt(1, TWO_MOD_Q)

        val hashHeader = ONE_MOD_Q
        val badHashHeader = TWO_MOD_Q
        val goodProof0 =
            message0.disjunctiveChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val goodProof1 =
            message1.disjunctiveChaumPedersenProofKnownNonce(
                1,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof0 =
            message0.disjunctiveChaumPedersenProofKnownNonce(
                1,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof1 =
            message1.disjunctiveChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof2 =
            badMessage0.disjunctiveChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )
        val badProof3 =
            badMessage1.disjunctiveChaumPedersenProofKnownNonce(
                0,
                nonce,
                keypair.publicKey,
                seed,
                hashHeader
            )

        assertTrue(goodProof0.isValid(message0, keypair.publicKey, hashHeader))
        assertTrue(goodProof1.isValid(message1, keypair.publicKey, hashHeader))

        assertFalse(goodProof0.isValid(message1, keypair.publicKey, hashHeader))
        assertFalse(goodProof0.isValid(message1, keypair.publicKey, hashHeader))

        assertFalse(goodProof1.isValid(message0, keypair.publicKey, badHashHeader))
        assertFalse(goodProof1.isValid(message0, keypair.publicKey, badHashHeader))

        assertFalse(badProof0.isValid(message0, keypair.publicKey, hashHeader))
        assertFalse(badProof0.isValid(message1, keypair.publicKey, hashHeader))

        assertFalse(badProof1.isValid(message0, keypair.publicKey, hashHeader))
        assertFalse(badProof1.isValid(message1, keypair.publicKey, hashHeader))

        assertFalse(badProof2.isValid(message0, keypair.publicKey, hashHeader))
        assertFalse(badProof2.isValid(message1, keypair.publicKey, hashHeader))

        assertFalse(badProof3.isValid(message1, keypair.publicKey, hashHeader))
        assertFalse(badProof3.isValid(message0, keypair.publicKey, hashHeader))
    }

    @Test
    fun disjunctiveProofs() {
        qt().withTestingTime(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS)
            .forAll(
                tuples(
                    integers().between(0, 1),
                    elementsModQNoZero("nonce"),
                    elGamalKeypairs("key"),
                    elementsModQ("seed"),
                    elementsModQ("hashHeader")
                )
            )
            .checkAssert { (constant, nonce, keypair, seed, hashHeader) ->
                val ciphertext = keypair.encrypt(constant, nonce)
                val proof =
                    ciphertext.disjunctiveChaumPedersenProofKnownNonce(
                        constant,
                        nonce,
                        keypair.publicKey,
                        seed,
                        hashHeader
                    )
                assertTrue(proof.isValid(ciphertext, keypair.publicKey, hashHeader))

                // now, swap the proofs around and verify it fails
                val badProof =
                    proof.copy(
                        proof0 = proof.proof1,
                        proof1 = proof.proof0,
                        c =
                            hashElements(
                                hashHeader,
                                ciphertext.pad,
                                ciphertext.data,
                                proof.proof1.a,
                                proof.proof1.b,
                                proof.proof0.a,
                                proof.proof0.b
                            )
                    )
                assertFalse(badProof.isValid(ciphertext, keypair.publicKey, hashHeader))
                assertFalse(
                    badProof.copy(c = proof.c).isValid(ciphertext, keypair.publicKey, hashHeader)
                )
            }
    }

    @Test
    fun fakeGenericProofsDontValidate() {
        qt().withTestingTime(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS)
            .forAll(
                tuples(
                    elementsModQNoZero("q1"),
                    elementsModQNoZero("q2"),
                    elementsModQ("x"),
                    elementsModQ("notX"),
                    elementsModQ("c"),
                    elementsModQ("seed"),
                    elementsModQ("hashHeader")
                )
            )
            .checkAssert { (q1, q2, x, maybeNotX, c, seed, hashHeader) ->
                val notX = if (x == maybeNotX) x + ONE_MOD_Q else maybeNotX
                val g = gPowP(q1)
                val h = gPowP(q2)
                val gx = g powP x
                val hNotX = h powP notX

                val badProof = fakeGenericChaumPedersenProofOf(g, gx, h, hNotX, c, seed)
                assertTrue(
                    badProof.isValid(g, gx, h, hNotX, hashHeader, checkC = false),
                    "if we don't check c, the proof will validate"
                )
                assertFalse(
                    badProof.isValid(g, gx, h, hNotX, hashHeader, checkC = true),
                    "if we do check c, the proof will not validate"
                )
            }
    }
}
