package electionguard.pep

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.makeDoerreTrustee
import electionguard.ballot.makeGuardian
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.util.ErrorMessages
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun makeCakeEgkDecryption(
    group : GroupContext,
    nguardians: Int,
    quorum: Int,
    present: List<Int>,
) : CakeEgkDecryption {
    // Run our own KeyCeremony
    val ktrustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group,"guardian$seq", seq, nguardians, quorum)
    }.sortedBy { it.xCoordinate }
    // exchange PublicKeys
    ktrustees.forEach { t1 ->
        ktrustees.forEach { t2 ->
            t1.receivePublicKeys(t2.publicKeys().unwrap())
        }
    }
    // exchange SecretKeyShares
    ktrustees.forEach { t1 ->
        ktrustees.filter { it.id != t1.id }.forEach { t2 ->
            t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.id).unwrap())
        }
    }
    ktrustees.forEach { it.isComplete() }
    val guardians = Guardians(group, ktrustees.map { makeGuardian(it) })

    // create inputs for Doerre Decryption
    val extendedBaseHash = UInt256.random()
    val dtrustees: List<DecryptingTrusteeDoerre> = ktrustees.map { makeDoerreTrustee(it, extendedBaseHash) }
    val jointPublicKey = dtrustees.map { it.guardianPublicKey() }.reduce { a, b -> a * b }
    val available = dtrustees.filter { present.contains(it.xCoordinate()) }
    val decryptor = DecryptorDoerre(group, extendedBaseHash, ElGamalPublicKey(jointPublicKey), guardians, available)
    return CakeEgkDecryption(group, quorum, extendedBaseHash, jointPublicKey, dtrustees, decryptor)
}

// test Cake PEP using Egk Decryption
class CakeEgkDecryption(val group: GroupContext, val quorum: Int, val extendedBaseHash: UInt256, val publicKey : ElementModP,
                        val dtrustees: List<DecryptingTrusteeDoerre>, val decryptor : DecryptorDoerre) {
    val guardians = mutableListOf<PepGuardian>()

    init {
        dtrustees.forEach { guardians.add(PepGuardian(it))  }
    }

    // The CAKE PEP protocol, modified to use Egk
    fun doEgkPep(ratio: ElGamalCiphertext, expectEq: Boolean): ElementModP? {
        // step 1a: extra nonce protects (m1 - m2) from leaking. having multiple ones from each guardian is not needed?
        val stepValues = guardians.map { it.cakeStep1(ratio) }
        val stepMap = stepValues.associateBy { it.id }
        val A = with(group) { stepValues.map { it.bigAi }.multP() }
        val B = with(group) { stepValues.map { it.bigBi }.multP() }
        val ciphertextAB = ElGamalCiphertext(A, B)

        // step 1b: using the a,b with another extra nonce, which are included in the challenge
        val a = with(group) { stepValues.map { it.ai }.multP() }
        val b = with(group) { stepValues.map { it.bi }.multP() }
        val c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
            ratio.pad, ratio.data, A, B, a, b
        ).toElementModQ(group)
        // the response: vj = uj − c * ξj, has both of the new nonces in it
        guardians.map { it.cakeStep3(stepMap[it.id()]!!, c) } // 1b.h,i

        // 1b.3 (4n)
        guardians.forEach {
            val myStep1 = stepMap[it.id()]!!
            // (1b.4 (j) Verify if aj = α^vj * Aj^c and bj = β^vj * Bj^c
            val verifya = (ratio.pad powP myStep1.vi) * (myStep1.bigAi powP c)
            assertEquals(myStep1.ai, verifya)
            val verifyb = (ratio.data powP myStep1.vi) * (myStep1.bigBi powP c)
            assertEquals(myStep1.bi, verifyb)
        }

        //  Step 2 V:
        //  (T, ChaumPedersenProof(c',v')) = EGDecrypt(A, B)
        val egkDecryptOutput = EgkDecrypt(ciphertextAB, decryptor)
        if (egkDecryptOutput == null) {
            println("EgkDecrypt failed")
            return null
        }
        val T = egkDecryptOutput.T
        val c_prime = egkDecryptOutput.proof.c
        val v_prime = egkDecryptOutput.proof.r

        // Step 3 (verifier V)
        val v = with (group) { stepValues.map { it.vi }.addQ() }

        // (3.a) LOOK this is whats in addition (4)
        // (a) Compute a = α^v * A^c and b = β^v * B^c
        // verify if c = H(cons0 ; cons1 , K, α, β, A, B, a, b). Otherwise, output “reject”.
        val verifier_a = (ratio.pad powP v) * (A powP c)
        val verifier_b = (ratio.data powP v) * (B powP c)
        val verifier_c = hashFunction(
            extendedBaseHash.bytes, 0x30.toByte(), publicKey,
            ratio.pad, ratio.data, A, B, verifier_a, verifier_b
        ).toElementModQ(group)
        assertEquals(c, verifier_c)

        assertEquals(a, verifier_a) // LOOK ?
        assertEquals(b, verifier_b) // LOOK ?

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

        // this proof validation is the same as step 3.b
        assertTrue( egkDecryptOutput.proof.verifyDecryption(extendedBaseHash, publicKey, ciphertextAB, T))
        //    val M: ElementModP = encryptedVote.data / bOverM
        //    val a = group.gPowP(this.r) * (publicKey powP this.c)
        //    val b = (encryptedVote.pad powP this.r) * (M powP this.c)
        //    val challenge = hashFunction(extendedBaseHash.bytes, 0x30.toByte(), publicKey, encryptedVote.pad, encryptedVote.data, a, b, M)
        //    return (challenge.toElementModQ(group) == this.c)

        // (3.c)
        //       Set IsEq = T == 1
        //       If (!IsEq) output “accept(unequal)”
        //       If IsEq and (A, B) != (1, 1), output “accept(equal)” else output “reject”.

        val isEqual = T.equals(group.ONE_MOD_P)
        assertEquals(expectEq, isEqual)
        return T
    }

    data class CakeValues (val id : String,
                          val bigAi : ElementModP, val bigBi : ElementModP, val ai : ElementModP, val bi : ElementModP,
                          val epsi : ElementModQ, val ui : ElementModQ) {

        var vi : ElementModQ = productionGroup().ONE_MOD_Q
    }

    inner class PepGuardian(val dtrustee : DecryptingTrusteeDoerre) {
        fun id() = dtrustee.id

        fun cakeStep1(
            ratio: ElGamalCiphertext,
        ): CakeValues {
            // Step 1a.1
            // (a) ξi ←− Zq
            // (b) Compute Ai = α^ξi mod p and Bi = β^ξi mod p
            val epsi = group.randomElementModQ(minimum = 2)
            val Ai = ratio.pad powP epsi // g^((ξ-ξ') * ξ1)
            val Bi = ratio.data powP epsi // K^((ξ-ξ'+σ-σ') * ξ1)

            // Step 1b.1
            // (e) ui ←− Zq
            // (f) Compute ai = α^u1 mod p and bi = β^ui mod p
            val ui = group.randomElementModQ(minimum = 2)
            val ai = ratio.pad powP ui // g^((ξ-ξ') * u1)
            val bi = ratio.data powP ui // K^((ξ-ξ'+σ-σ') * u1)
            return CakeValues(id(), Ai, Bi, ai, bi, epsi, ui)
        }

        // Step 1b.3 (i) respond to challenge with vj = uj − c * ξj
        fun cakeStep3(
            cakeValues: CakeValues,
            c: ElementModQ,
        ) {
            cakeValues.vi = cakeValues.ui - c * cakeValues.epsi
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    fun EgkDecrypt(ciphertextAB : ElGamalCiphertext, decryptor: DecryptorDoerre) : EgkDecryptOutput? {
        val eTally = makeTallyForSingleCiphertext(ciphertextAB, extendedBaseHash)
        //group.showAndClearCountPowP()
        val errs = ErrorMessages("CakeEgkDecrypt")
        val dTally = decryptor.decryptPep(eTally, errs)
        if (dTally == null) {
            println("CakeEgkDecrypt failed errors = $errs")
            return null
        }
        //println("EgkDecrypt = ${group.showAndClearCountPowP()} expect ${4 * decryptor.nguardians + 4}")
        val dSelection = dTally.contests[0].selections[0]
        return EgkDecryptOutput(dSelection.bOverM, dSelection.proof)
    }
}

data class EgkDecryptOutput(val T: ElementModP, val proof : ChaumPedersenProof)

fun makeTallyForSingleCiphertext(ciphertext : ElGamalCiphertext, extendedBaseHash : UInt256) : EncryptedTally {
    val selection = EncryptedTally.Selection("Selection1", 1, ciphertext)
    val contest = EncryptedTally.Contest("Contest1", 1, listOf(selection))
    return EncryptedTally("tallyId", listOf(contest), emptyList(), extendedBaseHash)
}

