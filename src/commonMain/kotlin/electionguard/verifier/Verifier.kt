package electionguard.verifier

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.Guardian
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.hasValidSchnorrProof
import electionguard.publish.ElectionRecord

// quick proof verification - not necessarily the verification spec
class Verifier(val group: GroupContext, private val electionRecord: ElectionRecord, private val nthreads: Int = 11) {
    val jointPublicKey: ElGamalPublicKey
    val cryptoExtendedBaseHash: ElementModQ
    val decryption: DecryptionResult
    val guardians: List<Guardian>

    init {
        decryption = electionRecord.readDecryptionResult().getOrThrow { throw IllegalStateException(it) }
        jointPublicKey = decryption.tallyResult.jointPublicKey()
        cryptoExtendedBaseHash = decryption.tallyResult.cryptoExtendedBaseHash()
        guardians = decryption.tallyResult.electionInitialized.guardians
    }

    fun verify(): Boolean {
        println("Verify election record in = ${electionRecord.topdir()}\n")
        val guardiansOk = verifyGuardianPublicKey()
        println(" verifyGuardianPublicKey= $guardiansOk\n")

        val verifyTally = VerifyDecryptedTally(group, jointPublicKey, cryptoExtendedBaseHash, guardians)
        val tallyStats = verifyTally.verifyDecryptedTally(decryption.decryptedTally)
        println(" verifyDecryptedTally $tallyStats\n")

        val verifyBallots = VerifyEncryptedBallots(jointPublicKey, cryptoExtendedBaseHash, nthreads)
        val ballotStats = verifyBallots.verifyEncryptedBallots(electionRecord.iterateEncryptedBallots { true })
        println(" verifyEncryptedBallots $ballotStats\n")

        val spoiledStats =
            verifyTally.verifySpoiledBallotTallies(electionRecord.iterateSpoiledBallotTallies(), nthreads)
        println(" verifySpoiledBallotTallies $spoiledStats\n")

        return guardiansOk && ballotStats.allOk && tallyStats.allOk && spoiledStats.allOk
    }

    fun verifyEncryptedBallots(): Boolean {
        val verifyBallots = VerifyEncryptedBallots(jointPublicKey, cryptoExtendedBaseHash, nthreads)
        return verifyBallots.verifyEncryptedBallots(electionRecord.iterateEncryptedBallots { true }).allOk
    }

    private fun verifyGuardianPublicKey(): Boolean {
        var allValid = true
        for (guardian in this.guardians) {
            var guardianOk = true
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val publicKey = ElGamalPublicKey(guardian.coefficientCommitments[index])
                val validProof = publicKey.hasValidSchnorrProof(proof)
                guardianOk = guardianOk && validProof
            }
            // println(" Guardian ${guardian.guardianId} ok = ${guardianOk}")
            allValid = allValid && guardianOk
        }

        val jointPublicKeyComputed = this.guardians.map { it.publicKey() }.reduce { a, b -> a * b }
        allValid = allValid && jointPublicKey.equals(jointPublicKeyComputed)
        return allValid
    }
}

class Stats(
    val allOk: Boolean,
    val ncontests: Int,
    val nselections: Int,
    val nshares: Int = 0,
) {
    override fun toString(): String {
        return "allOk=$allOk, ncontests=$ncontests, nselections=$nselections, nshares=$nshares"
    }
}

class StatsAccum {
    var n: Int = 0
    var allOk: Boolean = true
    private var ncontests: Int = 0
    private var nselections: Int = 0
    private var nshares: Int = 0

    fun add(stat: Stats) {
        n++
        allOk = allOk && stat.allOk
        ncontests += stat.ncontests
        nselections += stat.nselections
        nshares += stat.nshares
    }

    override fun toString(): String {
        return "n=$n, allOk=$allOk, ncontests=$ncontests, nselections=$nselections, nshares=$nshares"
    }
}


