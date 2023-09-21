package electionguard.decrypt

import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// some sanity checks
class PepTest {
    val group = productionGroup()

    @Test
    fun basic() {
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
    fun ratios() {
        doRatio(0, 0, 0)
        doRatio(1, 1, 0)
        doRatio(1, 0, 1)
        doRatio(0, 1, null)
        doRatio(2, 2, 0)
        doRatio(2, 1, 1)
        doRatio(2, 0, 2)
        doRatio(0, 2, null)
    }

    fun doRatio(numerator : Int, denominator: Int, expect : Int?) {
        val keypair = elGamalKeyPairFromRandom(group)
        val enumerator = numerator.encrypt(keypair.publicKey) // (g^ξ, K^(σ+ξ))
        val edenominator = denominator.encrypt(keypair.publicKey) // (g^ξ', K^(σ'+ξ'))
        val ratio = ElGamalCiphertext(enumerator.pad div edenominator.pad, enumerator.data div edenominator.data)
        // (g^(ξ-ξ'), K^(σ+ξ-σ'-ξ') = (g^(ξ-ξ'), K^(ξ-ξ'+σ-σ')) σ-σ' = 0 when same, null when < 0

        val Mr = ratio.pad.powP(keypair.secretKey.key)
        val Tr = ratio.data div Mr  // K^t
        val decrypted = keypair.publicKey.dLog(Tr, 2)
        println("$numerator / $denominator = $decrypted")
        if (expect == null) {
            assertNull(decrypted)
        } else {
            assertEquals(expect, decrypted)
        }
    }

}