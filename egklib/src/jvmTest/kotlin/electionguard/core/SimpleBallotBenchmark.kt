package electionguard.core

import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.system.measureTimeMillis

// This is a slightly more complicated benchmark than just ElGamal encryption / decryption,
// which simulates encrypting a large number of ballots, and generating all the necessary
// Chaum-Pedersen proofs.

class SimplePlaintextBallot(val selections: List<Int>)

class SimpleEncryptedBallot(val selectionsAndProofs: List<Pair<ElGamalCiphertext, RangeChaumPedersenProofKnownNonce>>, val sumProof: RangeChaumPedersenProofKnownNonce)

fun SimplePlaintextBallot.encrypt(context: GroupContext, keypair: ElGamalKeypair, seed: ElementModQ, limit : Int = 1): SimpleEncryptedBallot {
    val encryptionNonces = Nonces(seed, "encryption")
    val proofNonces = Nonces(seed, "proof")
    val plaintextWithNonce = selections.mapIndexed { i, s -> Pair(s, encryptionNonces[i]) }
    val plaintextWithNonceAndCiphertext = plaintextWithNonce.map { (p, n) -> Triple(p, n, p.encrypt(keypair, n))}
    val selectionsAndProofs = plaintextWithNonceAndCiphertext.mapIndexed { i, (p, n, c) ->
        Pair(c, c.rangeChaumPedersenProofKnownNonce(p, 1, n, keypair.publicKey, proofNonces[i]))
    }
    val encryptedSum = selectionsAndProofs.map { it.first }.encryptedSum()
    val nonceSum = plaintextWithNonce.map { it.second }.reduce { a, b -> a + b }
    val plaintextSum = selections.sum()
    // fun ElGamalCiphertext.rangeChaumPedersenProofKnownNonce(
    //  @param plaintext The actual plaintext constant value used to make the ElGamal ciphertext (L in the spec)
    // * @param limit The maximum possible value for the plaintext (inclusive)
    // * @param nonce The aggregate nonce used creating the ElGamal ciphertext (r in the spec)
    // * @param publicKey The ElGamal public key for the election
    // * @param seed Used to generate other random values here
    // * @param qbar The election extended base hash (Q')
    // * @param overrideErrorChecks Allows the creation of invalid proofs
    //): RangeChaumPedersenProofKnownNonce {

    //  * @param plaintext The total allowed votes (L in the spec)
    // * @param nonce The aggregate nonce used creating the ElGamal ciphertext (r in the spec)
    // * @param publicKey The ElGamal public key for the election
    // * @param seed Used to generate other random values here
    // * @param qbar The election extended base hash (Q')
    val sumProof = encryptedSum.rangeChaumPedersenProofKnownNonce(plaintextSum, limit, nonceSum, keypair.publicKey, seed)

    return SimpleEncryptedBallot(selectionsAndProofs, sumProof)
}

fun randomSimplePlaintextBallot(size: Int): SimplePlaintextBallot {
    val selection = Random.nextInt(size)
    return SimplePlaintextBallot((0 until size).map { if (it == selection) 1 else 0 })
}

fun main() {
    val numBallots = 500
    val numCandidates = 5

    println("Ballot encryption simulation benchmark, JDK: ${System.getProperty("java.version")}")

    runBlocking {
        ProductionMode.values()
            .forEach { mode ->
                PowRadixOption.values().filter { it != PowRadixOption.EXTREME_MEMORY_USE }
                    .forEach { powRadixOption ->
                        println("=======================================================")
                        println("Initializing benchmark for $powRadixOption, $mode")
                        val context = productionGroup(powRadixOption, mode)

                        val keypair = elGamalKeyPairFromRandom(context)
                        val nonces = Array(numBallots) { context.randomElementModQ() }
                        val ballots = Array(numBallots) { randomSimplePlaintextBallot(numCandidates) }

                        // force the PowRadix tables to be realized before we start the clock
                        1.encrypt(keypair, nonces[0]).decrypt(keypair)

                        println("Running!")

                        var results: List<SimpleEncryptedBallot>

                        val encryptionTimeMs = measureTimeMillis {
                            results = ProgressBar
                                .wrap(
                                    (0 until numBallots).asIterable().toList(),
                                    ProgressBarBuilder()
                                        .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                                        .setInitialMax(numBallots.toLong())
                                        .setUpdateIntervalMillis(50)
                                        .setSpeedUnit(ChronoUnit.SECONDS)
                                        .setUnit(" enc", 1L)
                                        .setMaxRenderedLength(100)
                                        .showSpeed()
                                )
                                .map { ballots[it].encrypt(context, keypair, nonces[it]) }
                        }
                        val encryptionTime = encryptionTimeMs / 1000.0

                        println()
                        println("SimpleBallot %.2f encryptions / sec".format(numBallots / encryptionTime))
                        println()
                    }
            }
    }
}