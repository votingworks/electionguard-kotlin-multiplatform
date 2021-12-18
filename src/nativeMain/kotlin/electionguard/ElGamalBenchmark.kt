package electionguard

import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

// this is a simple benchmark that just measures how fast ElGamal encryption runs

fun elGamalPerfTest() {
    val N = 100

    PowRadixOption.values()
        .filter { it != PowRadixOption.EXTREME_MEMORY_USE }
        .forEach { powRadixOption ->
            println("Initializing benchmark for $powRadixOption")
            val context = productionGroup(powRadixOption)

            val keypair = elGamalKeyPairFromRandom(context)
            val nonces = Array(N) { context.randomElementModQ() }
            val random = Random(randomInt())
            println("Running!")

            val deltaTimeMs =
                measureTimeMillis {
                            (0..N - 1).forEach { i ->
                            val nonce = nonces[i]
                            val message = random.nextInt(1000)
                            val ciphertext = keypair.encrypt(message, nonce)
                            val plaintext = ciphertext.decrypt(keypair)
                            if (plaintext == null) {
                                print("Unexpected decryption failure")
                                exitProcess(1)
                            }
                            if (plaintext != message) {
                                print("Decryption isn't the inverse of encryption?")
                                exitProcess(1)
                            }
                        }
                }
            val deltaTime = deltaTimeMs / 1000.0
            println(
                "$N ElGamal encryption/decryption operations in ${deltaTime} seconds\n  = ${N / deltaTime} ops/sec"
            )
        }
}