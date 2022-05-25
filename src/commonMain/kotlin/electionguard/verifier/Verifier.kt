package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.getErrorOrElse
import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.Guardian
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.hasValidSchnorrProof
import electionguard.core.hashElements
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
        println(" 2. verifyGuardianPublicKey= ${guardiansOk is Ok}")
        println("  ${guardiansOk.getErrorOrElse { "" }}")

        val publicKeyOk = verifyElectionPublicKey(decryption.tallyResult.electionInitialized.cryptoBaseHash)
        println(" 3. verifyElectionPublicKey= $publicKeyOk")
        println("  ${publicKeyOk.getErrorOrElse { "" }}")

        val verifyBallots = VerifyEncryptedBallots(jointPublicKey, cryptoExtendedBaseHash, nthreads)
        // Note we are validating all ballots, not just CAST
        val ballotStats = verifyBallots.verifyEncryptedBallots(electionRecord.iterateEncryptedBallots { true })
        println(" 4. verifyEncryptedBallots $ballotStats\n")
        println("  ${ballotStats.errors.joinToString("\n")}")

        val verifyTally = VerifyDecryptedTally(group, jointPublicKey, cryptoExtendedBaseHash, guardians)
        val tallyStats = verifyTally.verifyDecryptedTally(decryption.decryptedTally)
        println(" 6. verifyDecryptedTally $tallyStats\n")

        val spoiledStats =
            verifyTally.verifySpoiledBallotTallies(electionRecord.iterateSpoiledBallotTallies(), nthreads)
        println(" verifySpoiledBallotTallies $spoiledStats\n")

        return (guardiansOk is Ok) && (publicKeyOk is Ok) && ballotStats.allOk && tallyStats.allOk && spoiledStats.allOk
    }

    private fun verifyGuardianPublicKey(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for (guardian in this.guardians) {
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val publicKey = ElGamalPublicKey(guardian.coefficientCommitments[index])
                if (!publicKey.hasValidSchnorrProof(proof)) {
                    checkProofs.add(Err("  Guardian ${guardian.guardianId} has invalid proof for coefficient $index"))
                } else {
                    checkProofs.add(Ok(true))
                }
            }
        }
        val errors = checkProofs.getAllErrors()
        return if (errors.isNotEmpty()) Err(errors.joinToString("\n")) else Ok(true)
    }

    private fun verifyElectionPublicKey(q: UInt256): Result<Boolean, String> {
        val jointPublicKeyComputed = this.guardians.map { it.publicKey() }.reduce { a, b -> a * b }
        val errors = mutableListOf<String>()
        if (!jointPublicKey.equals(jointPublicKeyComputed)) {
            errors.add("  3.A jointPublicKey does not equal computed")
        }
        val computedQbar = hashElements(q, jointPublicKey);
        if (!cryptoExtendedBaseHash.equals(computedQbar)) {
            errors.add("  3.B qbar does not equal computed qbar")
        }
        return if (errors.isNotEmpty()) Err(errors.joinToString("\n")) else Ok(true)
    }

    fun verifyEncryptedBallots(): StatsAccum {
        val verifyBallots = VerifyEncryptedBallots(jointPublicKey, cryptoExtendedBaseHash, nthreads)
        return verifyBallots.verifyEncryptedBallots(electionRecord.iterateEncryptedBallots { true })
    }

}

class Stats(
    val forWho: String,
    val allOk: Boolean,
    val ncontests: Int,
    val nselections: Int,
    val errors: String,
    val nshares: Int = 0,
) {
    override fun toString(): String {
        return "$forWho allOk=$allOk, ncontests=$ncontests, nselections=$nselections, nshares=$nshares"
    }
}

class StatsAccum() {
    var allOk: Boolean = true
    var n: Int = 0
    val errors = mutableListOf<String>()
    private var ncontests: Int = 0
    private var nselections: Int = 0
    private var nshares: Int = 0

    fun add(stat: Stats) {
        allOk = allOk && stat.allOk
        n++
        ncontests += stat.ncontests
        nselections += stat.nselections
        nshares += stat.nshares
        errors.add(stat.errors)
    }

    override fun toString(): String {
        return "allOk=$allOk, n=$n, ncontests=$ncontests, nselections=$nselections, nshares=$nshares"
    }
}


