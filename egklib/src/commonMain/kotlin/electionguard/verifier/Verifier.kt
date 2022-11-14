package electionguard.verifier

import com.github.michaelbull.result.*
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.core.*
import electionguard.publish.ElectionRecord

// since there's no verification spec 2.0, this is approximate
class Verifier(val record: ElectionRecord, val nthreads: Int = 11) {
    val group: GroupContext
    val manifest: Manifest
    val jointPublicKey: ElGamalPublicKey
    val qbar: ElementModQ

    init {
        if (record.stage() < ElectionRecord.Stage.INIT) {
            throw IllegalStateException("election record stage = ${record.stage()}, not initialized\n")
        }
        group = productionGroup()
        jointPublicKey = ElGamalPublicKey(record.jointPublicKey()!!)
        qbar = record.cryptoExtendedBaseHash()!!.toElementModQ(group)
        manifest = record.manifest()
    }

    fun verify(stats : Stats, showTime : Boolean = false): Boolean {
        println("\n****Verify election record in = ${record.topdir()}\n")
        val starting13 = getSystemTimeInMillis()

        if (record.stage() < ElectionRecord.Stage.INIT) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return false
        }

        val parametersOk = verifyParameters()
        println(" 1. verifyParameters= $parametersOk")

        val guardiansOk = verifyGuardianPublicKey()
        println(" 2. verifyGuardianPublicKeys= $guardiansOk")

        val publicKeyOk = verifyElectionPublicKey(record.cryptoBaseHash()!!)
        println(" 3. verifyElectionPublicKey= $publicKeyOk")

        if (record.stage() < ElectionRecord.Stage.ENCRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            val took = getSystemTimeInMillis() - starting13
            if (showTime) println("   verify 2,3 took $took millisecs")
            return true
        }

        // encryption and vote limits 4, 5, 6
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, qbar, nthreads)
        // Note we are validating all ballots, not just CAST
        val ballotResult = verifyBallots.verify(record.encryptedBallots { true }, stats, showTime)
        println(" 4,5,6. verifyEncryptedBallots $ballotResult")

        // 10 contest data for encrypted ballots

        if (record.stage() < ElectionRecord.Stage.TALLIED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally accumulation, box 7 and 9F
        val verifyAggregation = VerifyAggregation(group, verifyBallots.aggregator)
        val aggResult = verifyAggregation.verify(record.encryptedTally()!!, showTime)
        println(" 7. verifyBallotAggregation $aggResult")

        if (record.stage() < ElectionRecord.Stage.DECRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally decryption
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, qbar)
        val tallyResults = verifyTally.verify(record.decryptedTally()!!, isBallot = false, stats)
        println(" 8,9. verifyTallyDecryption $tallyResults")

        // 10, 11, 12, 13, 14 spoiled ballots
        val spoiledResults =
            verifyTally.verifySpoiledBallotTallies(record.spoiledBallotTallies(), nthreads, stats, showTime)
        println(" 10,11,12,13,14. verifySpoiledBallotTallies $spoiledResults")

        val allOk = (guardiansOk is Ok) && (publicKeyOk is Ok) && (aggResult is Ok) &&
                (ballotResult is Ok) && (aggResult is Ok) && (tallyResults is Ok) && (spoiledResults is Ok)
        println("verify allOK = $allOk\n")
        return allOk
    }

    // Verification Box 1
    private fun verifyParameters(): Result<Boolean, String> {
        val check: MutableList<Result<Boolean, String>> = mutableListOf()
        val constants = this.record.constants()

        if (!constants.largePrime.contentEquals(group.constants.largePrime)) {
            check.add(Err("  1.A The large prime is not equal to the large modulus p defined in Section 3.1.1"))
        }
        if (!constants.smallPrime.contentEquals(group.constants.smallPrime)) {
            check.add(Err("  1.B The small prime is not equal to the large modulus p defined in Section 3.1.1"))
        }
        if (!constants.cofactor.contentEquals(group.constants.cofactor)) {
            check.add(Err("  1.C The cofactor is not equal to the large modulus p defined in Section 3.1.1"))
        }
        if (!constants.generator.contentEquals(group.constants.generator)) {
            check.add(Err("  1.D The small prime is non equal to the large modulus p defined in Section 3.1.1"))
        }
        return check.merge()
    }

    // Verification Box 2
    private fun verifyGuardianPublicKey(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for (guardian in this.record.guardians()) {
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val result = proof.validate()
                if (result is Err) {
                    checkProofs.add(Err("  2.A Guardian ${guardian.guardianId} has invalid proof for coefficient $index " +
                        result.unwrapError()
                    ))
                }
            }
        }
        return checkProofs.merge()
    }

    // Verification Box 3
    private fun verifyElectionPublicKey(cryptoBaseHash: UInt256): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()

        val jointPublicKeyComputed = this.record.guardians().map { it.publicKey() }.reduce { a, b -> a * b }
        if (!jointPublicKey.equals(jointPublicKeyComputed)) {
            errors.add(Err("  3.A jointPublicKey K does not equal computed K = Prod(K_i)"))
        }

        val commitments = mutableListOf<ElementModP>()
        this.record.guardians().forEach { commitments.addAll(it.coefficientCommitments()) }
        // spec 1.52, eq 17 and 3.B
        val computedQbar: UInt256 = hashElements(cryptoBaseHash, jointPublicKey, commitments)
        if (!qbar.equals(computedQbar.toElementModQ(group))) {
            errors.add(Err("  3.B qbar does not match computed = H(Q, K, {K_ij})"))
            println("qbar $qbar != computed $computedQbar")
        }

        return errors.merge()
    }

    fun verifyEncryptedBallots(stats : Stats): Result<Boolean, String> {
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, qbar, nthreads)
        return verifyBallots.verify(record.encryptedBallots { true }, stats)
    }

    fun verifyEncryptedBallots(ballots: Iterable<EncryptedBallot>, stats : Stats): Result<Boolean, String> {
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, qbar, nthreads)
        return verifyBallots.verify(ballots, stats)
    }

    fun verifyDecryptedTally(tally: DecryptedTallyOrBallot, stats: Stats): Result<Boolean, String> {
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, qbar)
        return verifyTally.verify(tally, false, stats)
    }

    fun verifyRecoveredShares(): Result<Boolean, String> {
        val verifier = VerifyRecoveredShares(group, record)
        return verifier.verify()
    }

    fun verifySpoiledBallotTallies(stats: Stats): Result<Boolean, String> {
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, qbar)
        return verifyTally.verifySpoiledBallotTallies(record.spoiledBallotTallies(), nthreads, stats, true)
    }

}

/*
class Stats(
    val forWho: String,
    val result: Result<Boolean, String>,
    val ncontests: Int,
    val nselections: Int,
) {
    override fun toString(): String {
        val errors = if (result is Err) result.unwrapError() else ""
        return "allOk=${result is Ok}, ncontests=$ncontests, nselections=$nselections, errors=$errors"
    }
}

class StatsAccum {
    var n: Int = 0
    val results= mutableListOf<Result<Boolean, String>>()

    private var ncontests: Int = 0
    private var nselections: Int = 0

    fun add(stat: Stats) {
        n++
        ncontests += stat.ncontests
        nselections += stat.nselections
        results.add(stat.result)
    }

    fun add(result: Result<Boolean, String>) {
        results.add(result)
    }

    fun result() = results.merge()

    override fun toString(): String {
        val result = result()
        val errors = if (result is Err) result.unwrapError() else ""
        return "allOk=${result is Ok}, n=$n, ncontests=$ncontests, nselections=$nselections, errors=$errors"
    }
}

 */


