package electionguard.decrypt

import electionguard.core.*
import kotlin.test.assertEquals

// test pep using multiple guardians, but using guardian secret key instead of keyShare
class CakeDecryptionWithGuardians(val group: GroupContext, nguardians : Int) {
    val extendedBaseHash = UInt256.random()
    val guardians = mutableListOf<PepGuardian>()
    val publicKey : ElementModP
    val secretKey : ElementModQ

    init {
        repeat(nguardians) { guardians.add(PepGuardian("Guardian${it + 1}"))  }
        publicKey = with (group) { guardians.map { it.guardianKeypair.publicKey.key }.multP() }
        val keys = guardians.map { it.guardianKeypair.secretKey.key }
        secretKey = with (group) { keys.addQ() }
        assertEquals(group.gPowP(secretKey), publicKey)
    }

    // The CAKE PEP protocol (modified)
    fun doCakePep(ratio: ElGamalCiphertext, expectEq: Boolean): ElementModP {

        val step1s = guardians.map { it.cakeStep1(ratio) }
        val vis = guardians.map { it.cakeStep1h(ratio, step1s) }

        val A = with(group) { step1s.map { it.bigAi }.multP() }
        val B = with(group) { step1s.map { it.bigBi }.multP() }
        val a = with(group) { step1s.map { it.ai }.multP() }
        val b = with(group) { step1s.map { it.bi }.multP() }
        val c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
            ratio.pad, ratio.data, A, B, a, b
        ).toElementModQ(group)

        // Step 2 (All guardians), identical to eg decryption LOOK only done by me (the "verifier")
        // (2.1) Compute v = Sum_j(vj),
        val v = with (group) { vis.addQ() }

        // (2.2) T = DistDec((A, B); K)
        //       Set IsEq = T == 1
        val ciphertextAB = ElGamalCiphertext(A, B)
        val T = DecryptWithSecret(ciphertextAB, secretKey)
        val isEq = T.equals(group.ONE_MOD_P)
        assertEquals(expectEq, isEq)

        // (2.3) (c′, v′) := ZKPDecrypt (T, (A, B)).
        // Send (IsEq, c, v, T, c′, v′, A, B) to V
        val zkpDecrypt = ZpkDecrypt(ciphertextAB)
        val c_prime = zkpDecrypt.challenge
        val v_prime = zkpDecrypt.response

        // Step 3 (verifier V)
        // (3.a) Compute a = α^v * A^c and b = β^v * B^c,
        //       verify c = H(cons0 ; cons1 , K, α, β, A, B, a, b), otherwise, output “reject”.
        val verifier_a = (ratio.pad powP v) * (A powP c)
        val verifier_b = (ratio.data powP v) * (B powP c)
        val verifier_c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
            ratio.pad, ratio.data, A, B, verifier_a, verifier_b
        ).toElementModQ(group)
        assertEquals(a, verifier_a)
        assertEquals(b, verifier_b)
        assertEquals(c, verifier_c)

        // (3.b) Compute M = B / T mod p, a′ = g^v' * K^c' mod p,  b′ = A^v' * M^c' mod p
        //       verify  v′ ∈ Zq and c′ = hash(K, A, B, a′, b′ , M ) LOOK, otherwise, output “reject”.
        val M = B div T
        val a_prime = group.gPowP(v_prime) * (publicKey powP c_prime)
        val b_prime = (A powP v_prime) * (M powP c_prime)
        val verify_c_prime = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
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

    data class CakeStep1 (val id : String,
                          val bigAi : ElementModP, val bigBi : ElementModP, val ai : ElementModP, val bi : ElementModP,
                          val epsi : ElementModQ, val ui : ElementModQ)

    inner class PepGuardian(val id : String) {
        val guardianKeypair = elGamalKeyPairFromRandom(group) // public = K = g ^ s

        fun cakeStep1(
            ratio: ElGamalCiphertext,
        ): CakeStep1 {
            // Step 1 (for each Gi)
            // (1.a) ξi ←− Zq
            // (1.b) Compute Ai = α^ξi mod p and Bi = β^ξi mod p
            val epsi = group.randomElementModQ(minimum = 2)
            val Ai = ratio.pad powP epsi // g^((ξ-ξ') * ξ1)
            val Bi = ratio.data powP epsi // K^((ξ-ξ'+σ-σ') * ξ1)

            // (1.e) ui ←− Zq
            // (1.f) Compute ai = α^u1 mod p and bi = β^ui mod p
            val ui = group.randomElementModQ(minimum = 2)
            val ai = ratio.pad powP ui // g^((ξ-ξ') * u1)
            val bi = ratio.data powP ui // K^((ξ-ξ'+σ-σ') * u1)
            return CakeStep1(id, Ai, Bi, ai, bi, epsi, ui)
        }

        // return vi = response to challenge
        fun cakeStep1h(
            ratio: ElGamalCiphertext,
            step1s: List<CakeStep1>,
        ): ElementModQ {

            // (1.h) Compute A = Prod_j(Aj), B = Prod_j(Bj), a = Prod_j(aj), b = Prod_j(bj)
            val A = with(group) { step1s.map { it.bigAi }.multP() }
            val B = with(group) { step1s.map { it.bigBi }.multP() }
            val a = with(group) { step1s.map { it.ai }.multP() }
            val b = with(group) { step1s.map { it.bi }.multP() }

            // c = H(cons0; cons1, K, α, β, A, B, a, b)
            val c = hashFunction(
                extendedBaseHash.bytes, 0x30.toByte(), publicKey,
                ratio.pad, ratio.data, A, B, a, b
            ).toElementModQ(group)

            val myStep1 = step1s.find { it.id == id } ?: throw RuntimeException("cant find step1 $id")

            // (1.i) Send vj = uj − c * ξj to Gj for any j != i
            val vi = myStep1.ui - c * myStep1.epsi

            // (1.j) Verify if aj = α^vj * Aj^c and bj = β^vj * Bj^c for any j ̸= i. LOOK just mine
            val verifya = (ratio.pad powP vi) * (myStep1.bigAi powP c)
            assertEquals(myStep1.ai, verifya)
            val verifyb = (ratio.data powP vi) * (myStep1.bigBi powP c)
            assertEquals(myStep1.bi, verifyb)

            return vi
        }

        // the guardian specific part of the ZpkDecrypt
        fun decryptStep1(ciphertextAB : ElGamalCiphertext) : ElementModP {
            // 1. Gi : Compute share Mi = A^si mod p and send Mi to Gj for any j ̸= i
            val Mi = ciphertextAB.pad powP guardianKeypair.secretKey.key
            return Mi
        }

        fun decryptStep2p3(ciphertextAB : ElGamalCiphertext): DecryptValuesFromGuardian {
            // 2.3 choose ui from Z∗q , then compute ai = g^ui mod p and bi = A^ui mod p and send (ai , bi) to Gj
            val ui = group.randomElementModQ(minimum = 2)
            val ai = group.gPowP(ui)
            val bi = ciphertextAB.pad powP ui

            return DecryptValuesFromGuardian(id, ui, ai, bi)
        }

        // return the response
        fun decryptStep4(guardianValue: DecryptValuesFromGuardian?, c : ElementModQ) : ElementModQ {
            if (guardianValue == null) throw RuntimeException("cant find step2p3 $id")
            // 4. Gi : Send vi = ui − c * si to Gj for any j  != i
            guardianValue.vi= guardianValue.ui - c * guardianKeypair.secretKey.key
            return guardianValue.vi
        }

        // verify the response
        fun decryptStep5(guardianValue: DecryptValuesFromGuardian?, c : ElementModQ, A : ElementModP, M : ElementModP) {
            if (guardianValue == null) throw RuntimeException("cant find step2p3 $id")
            // 5. Gi : Compute a′j = g^vj * Kj^c and b′j = A^vj * Mj^c and verify aj = a′j and bj = b′j
            val api = group.gPowP(guardianValue.vi!!) * (guardianKeypair.publicKey.key powP c)
            val bpi = (A powP guardianValue.vi!!) * (guardianValue.Mi powP c)
            assertEquals(guardianValue.ai, api)
            assertEquals(guardianValue.bi, bpi)
        }

    }

    inner class DecryptValuesFromGuardian (val id : String,
                                     val ui : ElementModQ, val ai : ElementModP, val bi : ElementModP,
                                     var Mi : ElementModP = group.ONE_MOD_P, var vi : ElementModQ = group.ONE_MOD_Q)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    fun ZpkDecrypt(ciphertextAB : ElGamalCiphertext) : ZpkDecryptOutput {
        val A = ciphertextAB.pad
        val B = ciphertextAB.data

        val Mis = guardians.map { it.decryptStep1(ciphertextAB) }

        // 2.1 Compute M = Prod_j(Mj mod p)
        val M = with(group) { Mis.multP() }
        // 2.2 T = B / M
        val T = B div M

        // 2.3 choose ui from Z∗q , then compute ai = g^ui mod p and bi = A^ui mod p and send (ai , bi) to Gj
        val step2p3s = guardians.map { it.decryptStep2p3(ciphertextAB) }
        val guardianValues = step2p3s.associateBy { it.id }

        // add the Mi to the guardianValues
        step2p3s.forEachIndexed { idx, obj -> obj.Mi = Mis[idx] }

        // 3. Gi : Compute a = Prod_j(aj), b = Prod_j(bj), c = H(cons0; cons1, K, A, B, a, b, M )
        val a = with(group) { step2p3s.map { it.ai }.multP() }
        val b = with(group) { step2p3s.map { it.bi }.multP() }
        val c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
            A, B, a, b, M
        ).toElementModQ(group)

        // 4. Gi : Send vi = ui − c * si to Gj for any j  != i
        val vis = guardians.map { it.decryptStep4(guardianValues[it.id], c) }

        // 5. Gi : Compute a′j = g^vj * Kj^c and b′j = A^vj * Mj^c and verify aj = a′j and bj = b′j
        guardians.map { it.decryptStep5(guardianValues[it.id], c, A, M) }

        // 6. Compute v = Sumj(vj)
        val v = with (group) { vis.addQ() }

        // 7. V : Compute M = B / T, a = g^v * K^c and b = A^v * M^c
        //        verify c = H(cons0 ; cons1 , K, A, B, a, b, M ). Otherwise, reject.
        val vM = B div T
        val va = group.gPowP(v) * (publicKey powP c)
        val vb = (A powP v) * (M powP c)
        val vc = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
            A, B, va, vb, vM
        ).toElementModQ(group)
        assertEquals(c, vc)

        return ZpkDecryptOutput(T, c, v)
    }
}

data class ZpkDecryptOutput(val T: ElementModP, val challenge: ElementModQ, val response: ElementModQ)

// keypair = (s, K) = (s, g^s)
// text = (g^ξ, K^(σ+ξ)) = (g^ξ, g^s*(σ+ξ)) = (g^ξ, g^σ^s * g^ξ^*s)
fun DecryptWithSecret(text : ElGamalCiphertext, secretKey : ElementModQ) : ElementModP {
    val M = text.pad.powP(secretKey) // g^ξ^s
    val T = text.data div M  // K^(σ+ξ) / g^ξ^s  = g^s^σ * g^s^ξ / g^ξ^s = g^σ^s = K^σ
    return T
}
