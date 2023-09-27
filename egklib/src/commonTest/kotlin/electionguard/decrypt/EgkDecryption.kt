package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.makeDoerreTrustee
import electionguard.ballot.makeGuardian
import electionguard.core.*
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// test non-CAKE PEP with EGK decryption. run with PepTest.testEgkDecryption() {)
fun makeEgkDecryption(
    group : GroupContext,
    nguardians: Int,
    quorum: Int,
    present: List<Int>,
) : PlaintextEquivalenceProof {

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
    val dtrustees: List<DecryptingTrusteeDoerre> = ktrustees.map { makeDoerreTrustee(it) }
    val jointPublicKey = dtrustees.map { it.guardianPublicKey() }.reduce { a, b -> a * b }
    val missing = dtrustees.filter {!present.contains(it.xCoordinate())}.map { it.id }
    val available = dtrustees.filter {present.contains(it.xCoordinate())}
    val extendedBaseHash = UInt256.random()
    return PlaintextEquivalenceProof(group, extendedBaseHash, ElGamalPublicKey(jointPublicKey), guardians, dtrustees)
}

