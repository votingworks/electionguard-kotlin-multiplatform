package electionguard.core

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.math.max
import kotlin.math.min
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

            assertTrue(goodProof.validate(message, keypair.publicKey, hashHeader) is Ok)
            assertFalse(goodProof.validate(message, keypair.publicKey, badHashHeader) is Ok)
            assertFalse(badProof1.validate(message, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof2.validate(message, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof3.validate(message, keypair.publicKey, hashHeader) is Ok)
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
                goodProof.validate(message, keypair.publicKey, hashHeader, expectedConstant = 0)  is Ok
            )
            assertFalse(
                goodProof.validate(message, keypair.publicKey, hashHeader, expectedConstant = 1)  is Ok
            )
            assertFalse(goodProof.validate(message, keypair.publicKey, badHashHeader) is Ok)
            assertFalse(badProof1.validate(message, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof2.validate(message, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof3.validate(message, keypair.publicKey, hashHeader) is Ok)
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
                        limit = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        qbar = context.ONE_MOD_Q
                    )
                assertTrue(
                    proof.validate(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = constant
                    )  is Ok,
                    "first proof is valid"
                )
                assertFalse(
                    proof.validate(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = constant + 1
                    ) is Ok,
                    "modified constant invalidates proof"
                )
                assertFalse(
                    proof.validate(badMessage, keypair.publicKey, context.ONE_MOD_Q) is Ok,
                    "modified message invalidates proof"
                )

                val badProof =
                    badMessage.constantChaumPedersenProofKnownNonce(
                        limit = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        qbar = context.ONE_MOD_Q
                    )
                assertFalse(
                    badProof.validate(badMessage, keypair.publicKey, context.ONE_MOD_Q) is Ok,
                    "modified proof with consistent message is invalid"
                )

                val badProof2 =
                    message.constantChaumPedersenProofKnownNonce(
                        limit = badConstant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        seed = seed,
                        qbar = context.ONE_MOD_Q
                    )
                assertFalse(
                    badProof2.validate(message, keypair.publicKey, context.ONE_MOD_Q) is Ok,
                    "modified proof with inconsistent message is invalid"
                )

                //                val badProof3 = proof.copy(constant = Int.MAX_VALUE)
                //                assertFalse(badProof3.isValid(message, keypair.publicKey,
                // ONE_MOD_Q))
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
                    proof.validate(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = constant
                    )  is Ok
                )
                assertTrue(proof.validate(message, keypair.publicKey, context.ONE_MOD_Q) is Ok)
                assertFalse(
                    proof.validate(
                        message,
                        keypair.publicKey,
                        context.ONE_MOD_Q,
                        expectedConstant = badConstant
                    ) is Ok
                )
                assertFalse(proof.validate(badMessage, keypair.publicKey, context.ONE_MOD_Q) is Ok)

                val badProof =
                    badMessage.constantChaumPedersenProofKnownSecretKey(
                        constant,
                        keypair,
                        seed,
                        context.ONE_MOD_Q
                    )
                assertFalse(badProof.validate(badMessage, keypair.publicKey, context.ONE_MOD_Q) is Ok)

                val badProof2 =
                    message.constantChaumPedersenProofKnownSecretKey(
                        badConstant,
                        keypair,
                        seed,
                        context.ONE_MOD_Q
                    )
                assertFalse(badProof2.validate(message, keypair.publicKey, context.ONE_MOD_Q) is Ok)

                val badProof3 = proof.copy(constant = intTestQ - 1)
                assertFalse(badProof3.validate(message, keypair.publicKey, context.ONE_MOD_Q) is Ok)
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

        val proof = genericChaumPedersenProofOf(g, h, x, seed, arrayOf(hashHeaderX), arrayOf(hashHeaderX))
        assertTrue(proof.validate(g, gx, h, hx, arrayOf(hashHeaderX), arrayOf(hashHeaderX)) is Ok)

        if (gx != gnotx && hx != hnotx) {
            // In the degenerate case where q1 or q2 == 0, then we'd have a problem:
            // g = 1, gx = 1, and gnotx = 1. Same thing for h, hx, hnotx. This means
            // swapping in gnotx for gx doesn't actually do anything.

            assertFalse(proof.validate(g, gnotx, h, hx, hashHeader = arrayOf(hashHeaderX)) is Ok)
            assertFalse(proof.validate(g, gx, h, hnotx, hashHeader = arrayOf(hashHeaderX)) is Ok)
            assertFalse(proof.validate(g, gnotx, h, hnotx, hashHeader = arrayOf(hashHeaderX)) is Ok)
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

            assertTrue(goodProof0.validate(message0, keypair.publicKey, hashHeader) is Ok)
            assertTrue(goodProof1.validate(message1, keypair.publicKey, hashHeader) is Ok)

            assertFalse(goodProof0.validate(message1, keypair.publicKey, hashHeader) is Ok)
            assertFalse(goodProof0.validate(message1, keypair.publicKey, hashHeader) is Ok)

            assertFalse(goodProof1.validate(message0, keypair.publicKey, badHashHeader) is Ok)
            assertFalse(goodProof1.validate(message0, keypair.publicKey, badHashHeader) is Ok)

            assertFalse(badProof0.validate(message0, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof0.validate(message1, keypair.publicKey, hashHeader) is Ok)

            assertFalse(badProof1.validate(message0, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof1.validate(message1, keypair.publicKey, hashHeader) is Ok)

            assertFalse(badProof2.validate(message0, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof2.validate(message1, keypair.publicKey, hashHeader) is Ok)

            assertFalse(badProof3.validate(message1, keypair.publicKey, hashHeader) is Ok)
            assertFalse(badProof3.validate(message0, keypair.publicKey, hashHeader) is Ok)
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

                assertTrue(proof.validate(ciphertext, keypair.publicKey, hashHeader) is Ok)

                // now, swap the proofs around and verify it fails
                val badProof = proof.copy(proof0 = proof.proof1, proof1 = proof.proof0, c = proof.c)

                assertFalse(badProof.validate(ciphertext, keypair.publicKey, hashHeader) is Ok)
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

                val badProof = fakeGenericChaumPedersenProofOf(c, seed)
                assertTrue(
                    badProof.validate(g, gx, h, hNotX, arrayOf(hashHeader), checkC = false)  is Ok,
                    "if we don't check c, the proof will validate"
                )
                assertFalse(
                    badProof.validate(g, gx, h, hNotX, arrayOf(hashHeader), checkC = true)  is Ok,
                    "if we do check c, the proof will not validate"
                )
            }
        }
    }

    @Test
    fun testAccum() {
        runTest {
            val context = productionGroup()
            val constant = 42
            val key = context.ONE_MOD_P
            val nonce = context.TWO_MOD_Q
            val seed = context.TWO_MOD_Q
            val hashHeader = context.ONE_MOD_Q
            val publicKey = ElGamalPublicKey(key)

            val vote0 = 0.encrypt(publicKey, nonce)
            val vote1 = 1.encrypt(publicKey, nonce)
            val vote41 = 41.encrypt(publicKey, nonce)
            val nonceAccum = nonce + nonce + nonce // we're using it three times

            val texts: List<ElGamalCiphertext> = listOf(vote0, vote1, vote41)
            val message: ElGamalCiphertext = texts.encryptedSum()

            val proof =
                message.constantChaumPedersenProofKnownNonce(
                    limit = constant,
                    nonce = nonceAccum,
                    publicKey = publicKey,
                    seed = seed,
                    qbar = hashHeader
                )

            assertTrue(
                proof.validate(
                    message,
                    publicKey = publicKey,
                    qbar = hashHeader,
                    expectedConstant = constant
                ) is Ok,
                "proof not valid"
            )
        }
    }

    @Test
    fun testAccumDifferentNonces() {
        runTest {
            val context = productionGroup()
            val constant = 42
            val contestNonce = context.randomElementModQ(2)
            val seed = context.randomElementModQ(2)
            val hashHeader = context.randomElementModQ(2)
            val keyPair = elGamalKeyPairFromSecret(context.randomElementModQ(2))
            val publicKey = keyPair.publicKey

            val randomQ = context.randomElementModQ(2)
            val nonceSequence = Nonces(randomQ, contestNonce)

            val vote0 = 0.encrypt(publicKey, nonceSequence.get(0))
            val vote1 = 1.encrypt(publicKey, nonceSequence.get(1))
            val vote41 = 41.encrypt(publicKey, nonceSequence.get(2))
            val nonceAccum = nonceSequence.get(0) + nonceSequence.get(1) + nonceSequence.get(2)

            val texts: List<ElGamalCiphertext> = listOf(vote0, vote1, vote41)
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()

            val proof =
                ciphertextAccumulation.constantChaumPedersenProofKnownNonce(
                    limit = constant,
                    nonce = nonceAccum,
                    publicKey = publicKey,
                    seed = seed,
                    qbar = hashHeader
                )

            assertTrue(
                proof.validate(
                    ciphertextAccumulation,
                    publicKey = publicKey,
                    qbar = hashHeader,
                    expectedConstant = constant
                ) is Ok,
                "proof not valid"
            )
        }
    }

    @Test
    fun testGenericProof() {
        runTest {
            val context = productionGroup()
            val keypair = elGamalKeyPairFromSecret(context.randomElementModQ())
            val ciphertext: ElGamalCiphertext = 42.encrypt(keypair.publicKey, context.randomElementModQ())
            val extendedBaseHash = context.randomElementModQ()

            val partialDecryption: ElementModP = ciphertext.computeShare(keypair.secretKey)
            val publicKey = keypair.publicKey.key

            val g = context.G_MOD_P
            val h = ciphertext.pad
            val x = context.randomElementModQ()
            val seed = context.randomElementModQ()
            val header = context.randomElementModQ()

            val gx1 = g powP x
            val hx1 = h powP x

            val proof1 = genericChaumPedersenProofOf(g, h, x, seed, arrayOf(header))
            assertTrue(proof1.validate(g, gx1, h, hx1, arrayOf(header)) is Ok)

            // proof that we know the secret, x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                context.G_MOD_P,
                ciphertext.pad,
                x,
                context.randomElementModQ(),
                arrayOf(extendedBaseHash, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption),
            )

            // so we need
            val gx = context.G_MOD_P powP x
            val hx = ciphertext.pad powP x

            assertTrue(proof.validate(
                context.G_MOD_P,
                gx,
                ciphertext.pad,
                hx,
                arrayOf(extendedBaseHash, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption)
            ) is Ok)
        }
    }

    @Test
    fun testPartialDecryptAndProof() {
        runTest {
            val context = productionGroup()
            val keypair = elGamalKeyPairFromSecret(context.randomElementModQ())
            val ciphertext: ElGamalCiphertext = 42.encrypt(keypair.publicKey, context.randomElementModQ())
            val extendedBaseHash = context.randomElementModQ()

            val partialDecryption: ElementModP = ciphertext.computeShare(keypair.secretKey)
            val publicKey = keypair.publicKey.key

            // proof that we know the secret, x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                context.G_MOD_P,
                ciphertext.pad,
                x = keypair.secretKey.key,
                context.randomElementModQ(),
                arrayOf(extendedBaseHash, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption),
            )

            assertTrue(proof.validate(
                context.G_MOD_P,
                publicKey,
                ciphertext.pad,
                partialDecryption,
                arrayOf(extendedBaseHash, publicKey, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption)
            ) is Ok)
        }
    }

    @Test
    fun testCompensatedDecryptAndProof() {
        runTest {
            val context = productionGroup()
            val jointKey = elGamalKeyPairFromSecret(context.randomElementModQ())
            val ciphertext: ElGamalCiphertext = 42.encrypt(jointKey.publicKey, context.randomElementModQ())
            val extendedBaseHash = context.randomElementModQ()

            // val backup: SecretKeyShare = this.secretKeyShares[missingGuardianId]
            // val generatingGuardianValue: ElementModQ = backup.generatingGuardianValue
            val generatingGuardianValue: ElementModQ = context.randomElementModQ()
            val partialDecryption: ElementModP = ciphertext.computeShare(ElGamalSecretKey(generatingGuardianValue))

            // proof that we know the secret, x
            val proof: GenericChaumPedersenProof = genericChaumPedersenProofOf(
                g = context.G_MOD_P,
                h = ciphertext.pad,
                x = generatingGuardianValue,
                seed = context.randomElementModQ(),
                hashHeader = arrayOf(extendedBaseHash, jointKey.publicKey.key, ciphertext.pad, ciphertext.data), // section 7
                hashFooter = arrayOf(partialDecryption),
            )

            // so we need
            val gx = context.G_MOD_P powP generatingGuardianValue
            val hx = ciphertext.pad powP generatingGuardianValue // Mij
            assertTrue(proof.validate(
                g = context.G_MOD_P,
                gx,
                h = ciphertext.pad,
                hx,
                arrayOf(extendedBaseHash, jointKey.publicKey.key, ciphertext.pad, ciphertext.data), // section 7
                arrayOf(partialDecryption)
            ) is Ok)
        }
    }

    @Test
    fun testRangeProofsSimple() {
        val context = tinyGroup()
        val secretKey = 2.toElementModQ(context)
        val keypair = elGamalKeyPairFromSecret(secretKey)
        val publicKey = keypair.publicKey
        val nonce = 3.toElementModQ(context)
        val seed = 10.toElementModQ(context)
        val qbar = 42.toElementModQ(context)
        val ciphertext0 = 0.encrypt(keypair, nonce)
        val ciphertext2 = 2.encrypt(keypair, nonce)

        val rangeProof01 = ciphertext0.rangeChaumPedersenProofKnownNonce(0, 1, nonce, publicKey, seed, qbar)
        val valid01 = rangeProof01.validate(ciphertext0, publicKey, qbar, 1)

        val rangeProof02 = ciphertext0.rangeChaumPedersenProofKnownNonce(0, 2, nonce, publicKey, seed, qbar)
        val valid02 = rangeProof02.validate(ciphertext0, publicKey, qbar, 2)

        val rangeProof22 = ciphertext2.rangeChaumPedersenProofKnownNonce(2, 2, nonce, publicKey, seed, qbar)
        val valid22 = rangeProof22.validate(ciphertext2, publicKey, qbar, 2)

        if (valid01 is Err) {
            println("Error in valid01: $valid01")
        }

        if (valid02 is Err) {
            println("Error in valid02: $valid02")
        }

        if (valid22 is Err) {
            println("Error in valid02: $valid22")
        }

        assertTrue(valid01 is Ok && valid02 is Ok && valid22 is Ok)
    }

    @Test
    fun testRangeProofs() {
        val context = tinyGroup()
        runTest {
            checkAll(
                Arb.int(0, 5),
                Arb.int(0, 5),
                elementsModQNoZero(context),
                elGamalKeypairs(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { p0, p1, nonce, keypair, seed, qbar ->
                val plaintext = min(p0, p1)
                val rangeLimit = max(p0, p1)

                val ciphertext = plaintext.encrypt(keypair, nonce)
                val proof = ciphertext.rangeChaumPedersenProofKnownNonce(
                    plaintext,
                    rangeLimit,
                    nonce,
                    keypair.publicKey,
                    seed,
                    qbar
                )

                val proofValidation = proof.validate(ciphertext, keypair.publicKey, qbar, rangeLimit)
                assertTrue(proofValidation is Ok)
            }
        }
    }

    @Test
    fun testBadRangeProofs() {
        val context = tinyGroup()
        runTest {
            checkAll(
                Arb.int(0, 5),
                Arb.int(1, 5),
                elementsModQNoZero(context),
                elGamalKeypairs(context),
                elementsModQ(context),
                elementsModQ(context)
            ) { p0, p1, nonce, keypair, seed, qbar ->
                // we're deliberately making the plaintext greater than the range limit!
                val rangeLimit = p0
                val plaintext = p0 + p1

                val ciphertext = plaintext.encrypt(keypair, nonce)
                val proof = ciphertext.rangeChaumPedersenProofKnownNonce(
                    plaintext,
                    rangeLimit,
                    nonce,
                    keypair.publicKey,
                    seed,
                    qbar,
                    true // disables checks that would otherwise reject this proof
                )

                val proofValidation = proof.validate(ciphertext, keypair.publicKey, qbar, rangeLimit)
                assertTrue(proofValidation is Err)
            }
        }
    }
}