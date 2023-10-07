package electionguard.pep

import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CakeDecryptionsWithGuardiansTest {
    val group = productionGroup()
    val pepWithGuardians = CakeDecryptionWithGuardians(group, 3)

    @Test
    fun testOneGuardian() {
        val numerator = 1
        val denominator = 1
        val expectEq = true
        val pepWithOneGuardian = CakeDecryptionWithGuardians(group, 1)

        // since we have a different publicKey, have to regen the ratio
        val publicKeyG = ElGamalPublicKey(pepWithOneGuardian.publicKey)
        val ratioG = makeRatio(numerator, denominator, publicKeyG)
        val Tg = pepWithOneGuardian.doCakePep(ratioG, expectEq)
        val isEq = Tg.equals(group.ONE_MOD_P)
        print("$numerator / $denominator doCakePep isEqual = $isEq T = ${Tg.toStringShort()}")
        val dvoteg = publicKeyG.dLog(Tg)
        println(" dvote = $dvoteg")
        assertEquals(expectEq, isEq)
    }

    fun makeRatio(numerator : Int, denominator: Int, publicKey : ElGamalPublicKey) : ElGamalCiphertext {
        val enumerator = numerator.encrypt(publicKey) // (g^ξ, K^(σ+ξ))
        val edenominator = denominator.encrypt(publicKey) // (g^ξ', K^(σ'+ξ'))
        // (g^(ξ-ξ'), K^(σ+ξ-σ'-ξ') = (g^(ξ-ξ'), K^(ξ-ξ'+σ-σ')); σ-σ' = 0 when same
        return ElGamalCiphertext(enumerator.pad div edenominator.pad, enumerator.data div edenominator.data)
    }

    @Test
    fun testProof() {
        val numerator = 1
        val denominator = 1
        val expectEq = true
        val pepWithOneGuardian = CakeDecryptionWithGuardians(group, 1)

        // since we have a different publicKey, have to regen the ratio
        val publicKeyG = ElGamalPublicKey(pepWithOneGuardian.publicKey)
        val ratioG = makeRatio(numerator, denominator, publicKeyG)
        val Tg = pepWithOneGuardian.doCakePep(ratioG, expectEq)
        val isEq = Tg.equals(group.ONE_MOD_P)
        print("$numerator / $denominator doCakePep isEqual = $isEq T = ${Tg.toStringShort()}")
        val dvoteg = publicKeyG.dLog(Tg)
        println(" dvote = $dvoteg")
        assertEquals(expectEq, isEq)
    }

    @Test
    fun ratios() {
        doRatio(0, 0, true)
        doRatio(1, 1, true)
        doRatio(1, 0, false)
        doRatio(2, 2, true)
        doRatio(2, 1, false)
        doRatio(2, 0, false)

        doRatio(0, 1, false)
        doRatio(0, 2, false)
        doRatio(0, 200, false)
    }

    fun doRatio(numerator : Int, denominator: Int, expectEq : Boolean) {
        val keypair = elGamalKeyPairFromRandom(group)
        val ratio = makeRatio(numerator, denominator, keypair.publicKey)

        val Trs = doRatioSimple(ratio, keypair)
        val isEqs = Trs.equals(group.ONE_MOD_P)
        print("$numerator / $denominator doRatioSimple isEqual = $isEqs T = ${Trs.toStringShort()}")
        assertEquals(expectEq, isEqs)
        val dvote = keypair.publicKey.dLog(Trs)
        println(" dvote = $dvote")

        val Tr = doRatioCakeWithOneGuardian(ratio, keypair, expectEq)
        val isEq = Tr.equals(group.ONE_MOD_P)
        print("$numerator / $denominator doRatioCakeWithOneGuardian isEqual = $isEq T = ${Tr.toStringShort()}")
        val dvote2 = keypair.publicKey.dLog(Tr)
        println(" dvote = $dvote2")
        assertEquals(expectEq, isEq)

        // since we have a different publicKey, have to regen the ratio
        val publicKeyG = ElGamalPublicKey(pepWithGuardians.publicKey)
        val ratioG = makeRatio(numerator, denominator, publicKeyG)
        val Tg = pepWithGuardians.doCakePep(ratioG, expectEq)
        val isEqg = Tg.equals(group.ONE_MOD_P)
        print("$numerator / $denominator doCakePep isEqual = $isEqg T = ${Tg.toStringShort()}")
        val dvoteg = publicKeyG.dLog(Tg)
        println(" dvote = $dvoteg")
        assertEquals(expectEq, isEqg)

        println()
    }

    fun doRatioSimple(ratio : ElGamalCiphertext, keypair : ElGamalKeypair) : ElementModP {
        return DecryptWithSecret(ratio, keypair.secretKey.key)
    }

    // the case where theres only one guardian
    fun doRatioCakeWithOneGuardian(ratio : ElGamalCiphertext, keypair : ElGamalKeypair, expectEq : Boolean) : ElementModP {
        val K = keypair.publicKey.key

        // Step 1 (for each Gi)
        // (1.a) ξi ←− Zq
        // (1.b) Compute Ai = α^ξi mod p and Bi = β^ξi mod p
        val eps1 = group.randomElementModQ(minimum = 2)
        val A1 = ratio.pad powP eps1 // g^((ξ-ξ') * ξ1)
        val B1 = ratio.data powP eps1 // g^((ξ-ξ'+σ-σ') * ξ1*s)

        // DecryptWithSecret:
        // keypair = (s, K) = (s, g^s)
        // text = (g^((ξ-ξ') * ξ1), g^((ξ-ξ'+σ-σ') * ξ1*s)
        //
        //    val M = text.pad.powP(secretKey) // g^((ξ-ξ')*ξ1*s)
        //    val T = text.data div M  // g^((ξ-ξ'+σ-σ') * ξ1*s) / g^((ξ-ξ')*ξ1*s)
        //    g^((ξ-ξ')*ξ1*s) * g^((σ-σ')*ξ1*s) / g^((ξ-ξ')*ξ1*s) = g^((σ-σ')*ξ1*s); still = 1 when σ == σ', but when not, dloG does not give σ-σ'
        // if thats all, could we just adjust ratio and still use existing algo?
        // is there something in a, b that could leak (σ-σ') ? its because you dont want to use the same nonce, you could cancel them out.

        // (1.e) ui ←− Zq
        // (1.f) Compute ai = α^u1 mod p and bi = β^ui mod p
        val u1 = group.randomElementModQ(minimum = 2)
        val a1 = ratio.pad powP u1 // g^((ξ-ξ') * u1)
        val b1 = ratio.data powP u1 // K^((ξ-ξ'+σ-σ') * u1)

        // (1.h) Compute A = Prod_j(Aj), B = Prod_j(Bj), a = Prod_j(aj), b = Prod_j(bj) , c = H(cons0; cons1, K, α, β, A, B, a, b)
        val A = A1
        val B = B1
        val a = a1
        val b = b1
        val extendedBaseHash = UInt256.random()
        val c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), K,
            ratio.pad, ratio.data, A, B, a, b
        ).toElementModQ(group)

        // (1.i) Send vj = uj − c * ξj to Gj for any j != i
        val v1 = u1 - c * eps1

        // (1.j) Verify if aj = α^vj * Aj^c and bj = β^vj * Bj^c for any j ̸= i. Otherwise, stop.
        val verifya = (ratio.pad powP v1) * (A1 powP c)
        assertEquals(a1, verifya)
        val verifyb = (ratio.data powP v1) * (B1 powP c)
        assertEquals(b1, verifyb)

        // Step 2 (All guardians), identical to eg decryption
        // (2.1) Compute v = Sum_j(vj),
        val v = v1

        // (2.2) T = DistDec((A, B); K)
        //       Set IsEq = T == 1
        val ciphertextAB = ElGamalCiphertext(A, B)
        val T = DecryptWithSecret(ciphertextAB, keypair.secretKey.key)
        val isEq = T.equals(group.ONE_MOD_P)
        assertEquals(expectEq, isEq)
        val dlog = keypair.publicKey.dLog(T, 10)
        println("isEq = $isEq, dLog = $dlog")

        // (2.3) (c′, v′) := ZKPDecrypt (T, (A, B)).
        // Send (IsEq, c, v, T, c′, v′, A, B) to V
        val zkpDecrypt = ZpkDecryptWithOneGuardian(ciphertextAB, keypair, extendedBaseHash)
        val c_prime = zkpDecrypt.challenge
        val v_prime = zkpDecrypt.response
        assertEquals(T, zkpDecrypt.T)

        // Step 3 (verifier V)
        // (3.a) Compute a = α^v * A^c and b = β^v * B^c,
        //       verify c = H(cons0 ; cons1 , K, α, β, A, B, a, b), otherwise, output “reject”.
        val verifier_a = (ratio.pad powP v) * (A powP c)
        val verifier_b = (ratio.data powP v) * (B powP c)
        val verifier_c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), K,
            ratio.pad, ratio.data, A, B, verifier_a, verifier_b
        ).toElementModQ(group)
        assertEquals(c, verifier_c)

        // (3.b) Compute M = B / T mod p, a′ = g^v' * K^c' mod p,  b′ = A^v' * M^c' mod p
        //       verify  v′ ∈ Zq and c′ = hash(K, A, B, a′, b′ , M ) LOOK, otherwise, output “reject”.
        val M = B div T
        val a_prime = group.gPowP(v_prime) * (K powP c_prime)
        val b_prime = (A powP v_prime) * (M powP c_prime)
        val verify_c_prime = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), K,
            A, B, a_prime, b_prime, M
        ).toElementModQ(group)
        assertEquals(c_prime, verify_c_prime)

        // (3.c)
        //       Set IsEq = T == 1
        //       If (!IsEq) output “accept(unequal)”
        //       If IsEq and (A, B) != (1, 1), output “accept(equal)” else output “reject”.

        val isEqual = T.equals(group.ONE_MOD_P)
        assertEquals(expectEq, isEqual)
        return T
    }

    fun ZpkDecryptWithOneGuardian(ciphertextAB : ElGamalCiphertext, keypair : ElGamalKeypair, extendedBaseHash: UInt256) : ZpkDecryptOutput {
        val s1 = keypair.secretKey.key
        val K = keypair.publicKey.key
        val A = ciphertextAB.pad
        val B = ciphertextAB.data

        // 1. Gi : Compute share Mi = A^si mod p and send Mi to Gj for any j ̸= i
        val Mi = A powP s1

        // 2. Gi :
        // 2.1 Compute M = Prod_j(Mj mod p)
        val M = Mi
        // 2.2 T = B / M
        val T = B div M
        // 2.3 choose ui from Z∗q , then compute ai = g^ui mod p and bi = A^ui mod p and send (ai , bi) to Gj
        val u1 = group.randomElementModQ(minimum = 2)
        val a1 = group.gPowP(u1)
        val b1 = A powP u1 // K^((ξ-ξ'+σ-σ') * u1)

        // 3. Gi : Compute a = Prod_j(aj), b = Prod_j(bj), c = H(cons0; cons1, K, A, B, a, b, M )
        val a = a1
        val b = b1
        val c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), K,
            A, B, a, b, M
        ).toElementModQ(group)

        // 4. Gi : Send vi = ui − c * si to Gj for any j  != i
        val v1 = u1 - c * s1

        // 5. Gi : Compute a′j = g^vj * Kj^c and b′j = A^vj * Mj^c and verify aj = a′j and bj = b′j
        val ap1 = group.gPowP(v1) * (K powP c)
        val bp1 = (A powP v1) * (M powP c)
        assertEquals(a1, ap1)
        assertEquals(b1, bp1)

        // 6. All guardians: Compute v = Sumj(vj) and send the plaintext T and the proof (c, v) to V
        val v = v1

        // 7. V : Compute M = B / T, a = g^v * K^c and b = A^v * M^c
        //        verify c = H(cons0 ; cons1 , K, A, B, a, b, M ). Otherwise, reject.
        val vM = B div T
        val va = group.gPowP(v) * (K powP c)
        val vb = (A powP v) * (M powP c)
        val vc = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), K,
            A, B, va, vb, vM
        ).toElementModQ(group)
        assertEquals(c, vc)

        return ZpkDecryptOutput(T, c, v)
    }
}
