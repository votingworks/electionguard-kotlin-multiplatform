package electionguard.core

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChaumPedersenTest {
    @BeforeTest
    fun suppressLogs() {
        loggingErrorsOnly()
    }

    @Test
    fun testCCKnownNonceProofsSimpleEncryptionZero() {
        runTest {
            val context = tinyGroup()
            val keypair = elGamalKeyPairFromSecret(context.TWO_MOD_Q)
            val nonce = context.ONE_MOD_Q
            val seed = context.TWO_MOD_Q
            val message = 0.encrypt(keypair, nonce)
            val badMessage1 = 1.encrypt(keypair, nonce)
            val badMessage2 = 0.encrypt(keypair, context.TWO_MOD_Q)
            val hashHeader = context.ONE_MOD_Q
            val badHashHeader = context.TWO_MOD_Q
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
    }

    @Test
    fun testCCKnownSecretProofsSimpleEncryptionZero() {
        runTest {
            val context = tinyGroup()
            val keypair = elGamalKeyPairFromSecret(context.TWO_MOD_Q)
            val nonce = context.ONE_MOD_Q
            val seed = context.TWO_MOD_Q
            val message = 0.encrypt(keypair, nonce)
            val badMessage1 = 1.encrypt(keypair, nonce)
            val badMessage2 = 0.encrypt(keypair, context.TWO_MOD_Q)
            val hashHeader = context.ONE_MOD_Q
            val badHashHeader = context.TWO_MOD_Q
            val goodProof =
                message.constantChaumPedersenProofKnownSecretKey(0, keypair, seed, hashHeader)
            val badProof1 =
                message.constantChaumPedersenProofKnownSecretKey(1, keypair, seed, hashHeader)
            val badProof2 =
                badMessage1.constantChaumPedersenProofKnownSecretKey(0, keypair, seed, hashHeader)
            val badProof3 =
                badMessage2.constantChaumPedersenProofKnownSecretKey(0, keypair, seed, hashHeader)

            assertTrue(
                goodProof.isValid(message, keypair.publicKey, hashHeader, expectedConstant = 0)
            )
            assertFalse(
                goodProof.isValid(message, keypair.publicKey, hashHeader, expectedConstant = 1)
            )
            assertFalse(goodProof.isValid(message, keypair.publicKey, badHashHeader))
            assertFalse(badProof1.isValid(message, keypair.publicKey, hashHeader))
            assertFalse(badProof2.isValid(message, keypair.publicKey, hashHeader))
            assertFalse(badProof3.isValid(message, keypair.publicKey, hashHeader))
        }
    }

    @Test
    fun testCCProofsKnownNonce() {
        runTest {
            val context = tinyGroup()
            checkAll(
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                elementsModQ(context),
                Arb.int(min=0, max=100),
                Arb.int(min=0, max=100)
            ) { keypair, nonce, seed, constant, constant2 ->
                // we need the constants to be different
                val badConstant = if (constant == constant2) constant + 1 else constant2

                val message = constant.encrypt(keypair, nonce)
                val badMessage = badConstant.encrypt(keypair, nonce)

                val proof =
                    message.constantChaumPedersenProofKnownNonce(
                        plaintext = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        hashHeader = context.ONE_MOD_Q
                    )
                assertTrue(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = constant
                    )
                )
                assertFalse(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = badConstant
                    )
                )
                assertFalse(proof.isValid(badMessage, keypair.publicKey, context.ONE_MOD_Q))

                val badProof =
                    badMessage.constantChaumPedersenProofKnownNonce(
                        plaintext = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        hashHeader = context.ONE_MOD_Q
                    )
                assertFalse(badProof.isValid(badMessage, keypair.publicKey, context.ONE_MOD_Q))

                val badProof2 =
                    message.constantChaumPedersenProofKnownNonce(
                        plaintext = badConstant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        hashHeader = context.ONE_MOD_Q
                    )
                assertFalse(badProof2.isValid(message, keypair.publicKey, context.ONE_MOD_Q))

                val badProof3 = proof.copy(constant = intTestQ - 1)
                assertFalse(badProof3.isValid(message, keypair.publicKey, context.ONE_MOD_Q))
            }
        }
    }

    @Test
    fun testCCProofsKnownSecretKey() {
        runTest {
            val context = tinyGroup()
            checkAll(
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                elementsModQ(context),
                Arb.int(min = 0, max = 100),
                Arb.int(min = 0, max = 100),
            ) { keypair, nonce, seed, constant, constant2 ->
                // we need the constants to be different
                val badConstant = if (constant == constant2) constant + 1 else constant2

                val message = constant.encrypt(keypair, nonce)
                val badMessage = badConstant.encrypt(keypair, nonce)

                val proof =
                    message.constantChaumPedersenProofKnownSecretKey(
                        constant,
                        keypair,
                        seed,
                        context.ONE_MOD_Q
                    )
                assertTrue(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = constant
                    )
                )
                assertTrue(proof.isValid(message, keypair.publicKey, context.ONE_MOD_Q))
                assertFalse(
                    proof.isValid(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = badConstant
                    )
                )
                assertFalse(proof.isValid(badMessage, keypair.publicKey, context.ONE_MOD_Q))

                val badProof =
                    badMessage.constantChaumPedersenProofKnownSecretKey(
                        constant,
                        keypair,
                        seed,
                        context.ONE_MOD_Q
                    )
                assertFalse(badProof.isValid(badMessage, keypair.publicKey, context.ONE_MOD_Q))

                val badProof2 =
                    message.constantChaumPedersenProofKnownSecretKey(
                        badConstant,
                        keypair,
                        seed,
                        context.ONE_MOD_Q
                    )
                assertFalse(badProof2.isValid(message, keypair.publicKey, context.ONE_MOD_Q))

                val badProof3 = proof.copy(constant = intTestQ - 1)
                assertFalse(badProof3.isValid(message, keypair.publicKey, context.ONE_MOD_Q))
            }
        }
    }

    @Test
    fun testGcpProof() {
        runTest {
            val context = tinyGroup()
            checkAll(
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { q1, q2, x, x2, seed, hashHeader ->
                // we need x != notx, and using assuming() would slow us down
                val notx = if (x == x2) x + context.ONE_MOD_Q else x2
                helperTestGcp(q1, q2, x, notx, seed, hashHeader)
            }
        }
    }

    @Test
    fun testGcpProofSimple() {
        runTest {
            val context = tinyGroup()
            helperTestGcp(
                context.TWO_MOD_Q,
                3.toElementModQ(context),
                5.toElementModQ(context),
                context.TWO_MOD_Q,
                context.ZERO_MOD_Q,
                null
            )
            helperTestGcp(
                context.ONE_MOD_Q,
                context.ONE_MOD_Q,
                context.ZERO_MOD_Q,
                context.ONE_MOD_Q,
                context.ZERO_MOD_Q,
                null
            )
        }
    }

    private fun helperTestGcp(
        q1: ElementModQ,
        q2: ElementModQ,
        x: ElementModQ,
        notx: ElementModQ,
        seed: ElementModQ,
        hashHeader: ElementModQ?
    ) {
        val context = q1.context
        val g = context.gPowP(q1)
        val h = context.gPowP(q2)
        val gx = g powP x
        val hx = h powP x
        val gnotx = g powP notx
        val hnotx = h powP notx

        val hashHeaderX = hashHeader ?: context.ZERO_MOD_Q

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
        runTest {
            val context = tinyGroup()
            val keypair = elGamalKeyPairFromSecret(context.TWO_MOD_Q)
            val nonce = context.ONE_MOD_Q
            val seed = context.TWO_MOD_Q
            val message0 = 0.encrypt(keypair, nonce)
            val message1 = 1.encrypt(keypair, nonce)
            val badMessage0 = 0.encrypt(keypair, context.TWO_MOD_Q)
            val badMessage1 = 1.encrypt(keypair, context.TWO_MOD_Q)

            val hashHeader = context.ONE_MOD_Q
            val badHashHeader = context.TWO_MOD_Q
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
    }

    @Test
    fun disjunctiveProofs() {
        runTest {
            val context = tinyGroup()
            checkAll(
                Arb.int(min=0, max=1),
                elementsModQNoZero(context),
                elGamalKeypairs(context),
                elementsModQ(context),
                elementsModQ(context),
            ) { constant, nonce, keypair, seed, hashHeader ->
                val ciphertext = constant.encrypt(keypair, nonce)
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
                            ).toElementModQ(context)
                    )

                assertFalse(badProof.isValid(ciphertext, keypair.publicKey, hashHeader))

                assertFalse(
                    badProof.copy(c = proof.c).isValid(ciphertext, keypair.publicKey, hashHeader)
                )
            }
        }
    }

    @Test
    fun fakeGenericProofsDontValidate() {
        runTest {
            val context = tinyGroup()
            checkAll(
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { q1, q2, x, maybeNotX, c, seed, hashHeader ->
                val notX = if (x == maybeNotX) x + context.ONE_MOD_Q else maybeNotX
                val g = context.gPowP(q1)
                val h = context.gPowP(q2)
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
}