package electionguard.pep

import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CakeEgkDecryptionTest {
    val group = productionGroup()

    @Test
    fun basicDecryption() {
        val keypair = elGamalKeyPairFromRandom(group) // public = K = g ^ s
        val evoteOne = 1.encrypt(keypair.publicKey)

        // Enc(σ, ξ) = (α, β) = (g^ξ mod p, K^σ · K^ξ mod p) = (g^ξ mod p, K^σ+ξ mod p). spec 2.0.0 eq 24
        //    val pad = g^ξ
        //    val data = K^σ+ξ

        val M = evoteOne.pad.powP(keypair.secretKey.key) // pad^s = g^ξs
        val T = evoteOne.data div M  // K^σ+ξ /  g^ξs = (g^s)^(σ+ξ) / g^ξs = g^sσ . g^sξ / g^ξs =  g^sσ = K^σ
        val dvote = keypair.publicKey.dLog(T, 10)
        println("dvote = $dvote")
        assertEquals(1, dvote)
    }

    @Test
    fun testEgkDecryption() {
        runEgkDecryption(1, 1, 1, 1, true)
        runEgkDecryption(1, 1, 1, 0, false)

        runEgkDecryption(2, 2, 1, 1, true)
        runEgkDecryption(2, 2, 1, 0, false)

        runEgkDecryption(3, 2, 1, 1, true)
        runEgkDecryption(3, 2, 1, 0, false)

        runEgkDecryption(6, 5, 1, 1, true)
        runEgkDecryption(6, 5, 1, 0, false)
    }

    fun runEgkDecryption(
        nguardians: Int,
        quorum: Int,
        numerator: Int,
        denominator: Int,
        expectEq: Boolean,
        show: Boolean = true
    ) {
        println("runEgkDecryption n = $quorum / $nguardians")
        val cakeEgkDecryption = makeCakeEgkDecryption(group, nguardians, quorum, (1..quorum).toList())
        val publicKeyG = ElGamalPublicKey(cakeEgkDecryption.publicKey)
        val ratioG = makeRatio(numerator, denominator, publicKeyG)
        group.showAndClearCountPowP()
        val Tg = cakeEgkDecryption.doEgkPep(ratioG, expectEq)
        if (Tg != null) {
            val expect = 12 * nguardians + 16
            println(" after doEgkPep ${group.showAndClearCountPowP()} expect = $expect")
            val isEq = Tg.equals(group.ONE_MOD_P)
            if (show) {
                print(" $numerator / $denominator, doEgkPep isEqual = $isEq T = ${Tg.toStringShort()}")
                val dvoteg = publicKeyG.dLog(Tg)
                println(" dvote = $dvoteg")
            }
            assertEquals(expectEq, isEq)
        }
    }
}

fun makeRatio(numerator : Int, denominator: Int, publicKey : ElGamalPublicKey) : ElGamalCiphertext {
    val enumerator = numerator.encrypt(publicKey) // (g^ξ, K^(σ+ξ))
    val edenominator = denominator.encrypt(publicKey) // (g^ξ', K^(σ'+ξ'))
    // (g^(ξ-ξ'), K^(σ+ξ-σ'-ξ') = (g^(ξ-ξ'), K^(ξ-ξ'+σ-σ')); σ-σ' = 0 when same
    return ElGamalCiphertext(enumerator.pad div edenominator.pad, enumerator.data div edenominator.data)
}

// why do we return T ??
data class ZpkDecryptOutput(val T : ElementModP, val challenge : ElementModQ, val response : ElementModQ)
