package electionguard.core

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test

// This is a slightly more complicated benchmark than just ElGamal encryption / decryption,
// which simulates encrypting a large number of ballots, and generating all the necessary
// Chaum-Pedersen proofs.

class SimpleBallotBenchmark {
    // uncomment to run
    @Test
    fun simpleBallotPerfTest() {
        val numBallots = 500
        val numCandidates = 5

        println("Ballot encryption simulation benchmark, Kotlin native")

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

                            val encryptionTimeMs = measureTimeMillis {
                                (0 until numBallots).asIterable().toList()
                                    .map { ballots[it].encrypt(context, keypair, nonces[it]) }
                            }
                            val encryptionTime = encryptionTimeMs / 1000.0

                            println()
                            val speed = (numBallots / encryptionTime).toString()
                            println("SimpleBallot $speed encryptions / sec")
                            println()
                        }
                }
        }
    }


    class SimplePlaintextBallot(val selections: List<Int>)

    class SimpleEncryptedBallot(
        val selectionsAndProofs: List<Pair<ElGamalCiphertext, RangeChaumPedersenProofKnownNonce>>,
        val sumProof: RangeChaumPedersenProofKnownNonce
    )

    fun SimplePlaintextBallot.encrypt(
        context: GroupContext,
        keypair: ElGamalKeypair,
        seed: ElementModQ,
        limit : Int = 1,
    ): SimpleEncryptedBallot {
        val encryptionNonces = Nonces(seed, "encryption")
        val proofNonces = Nonces(seed, "proof")
        val plaintextWithNonce = selections.mapIndexed { i, s -> Pair(s, encryptionNonces[i]) }
        val plaintextWithNonceAndCiphertext = plaintextWithNonce.map { (p, n) -> Triple(p, n, p.encrypt(keypair, n)) }
        val selectionsAndProofs = plaintextWithNonceAndCiphertext.mapIndexed { i, (p, n, c) ->
            Pair(
                c,
                c.rangeChaumPedersenProofKnownNonce(p, 1, n, keypair.publicKey, proofNonces[i])
            )
        }
        val encryptedSum = selectionsAndProofs.map { it.first }.encryptedSum()
        val nonceSum = plaintextWithNonce.map { it.second }.reduce { a, b -> a + b }
        val plaintextSum = selections.sum()
        val sumProof = encryptedSum.rangeChaumPedersenProofKnownNonce(
            plaintextSum,
            limit,
            nonceSum,
            keypair.publicKey,
            seed,
        )

        return SimpleEncryptedBallot(selectionsAndProofs, sumProof)
    }

    fun randomSimplePlaintextBallot(size: Int): SimplePlaintextBallot {
        val selection = Random.nextInt(size)
        return SimplePlaintextBallot((0 until size).map { if (it == selection) 1 else 0 })
    }

}