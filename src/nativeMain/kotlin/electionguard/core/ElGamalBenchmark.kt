package electionguard.core

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

// this is a simple benchmark that just measures how fast ElGamal encryption runs

fun elGamalPerfTest() {
    val N = 1000

    PowRadixOption.values()
        .filter { it != PowRadixOption.EXTREME_MEMORY_USE }
        .forEach { powRadixOption ->
            println("Initializing benchmark for $powRadixOption")
            val context = runBlocking { productionGroup(powRadixOption) }

            val keypair = elGamalKeyPairFromRandom(context)
            val nonces = Array(N) { context.randomElementModQ() }
            val prng = Random.Default // not secure, but we don't care
            val messages = Array(N) { prng.nextInt(1000) }

            // force the PowRadix tables to be realized before we start the clock
            messages[0].encrypt(keypair, nonces[0]).decrypt(keypair)

            println("Running!")

            var ciphertexts: List<ElGamalCiphertext> = emptyList()

            val encryptionTimeMs = measureTimeMillis {
                ciphertexts = (0..N - 1).map { messages[it].encrypt(keypair, nonces[it]) }
            }
            val encryptionTime = encryptionTimeMs / 1000.0

            val ciphertextArray = ciphertexts.toTypedArray()

            var decryptions: List<Int?> = emptyList()
            val decryptionTimeMs = measureTimeMillis {
                decryptions = (0..N - 1) .map { ciphertextArray[it].decrypt(keypair) }
            }
            val decryptionTime = decryptionTimeMs / 1000.0

            println(
                "ElGamal ${(N / encryptionTime).toFloat()} encryptions/sec, ${(N / decryptionTime).toFloat()} decryptions/sec"
            )

            if (decryptions.contains(null)) {
                println("------- Unexpected decryption failure! -------")
                exitProcess(1)
            }

            val decryptionsNoNull = decryptions.filterNotNull()

            if (decryptionsNoNull != messages.toList()) {
                println("------- Unexpected decryption not inverse of encryption! -------")
                exitProcess(1)
            }
        }
}