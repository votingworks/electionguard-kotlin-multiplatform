package electionguard.cli

import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.sigfig
import electionguard.publish.readElectionRecord
import electionguard.verifier.Verifier
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlin.math.roundToInt

/**
 * Run election record verification CLI.
 */
class RunVerifier {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunVerifier")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
            ).required()
            val nthreads by parser.option(
                ArgType.Int,
                shortName = "nthreads",
                description = "Number of parallel threads to use"
            ).default(11)
            val showTime by parser.option(
                ArgType.Boolean,
                shortName = "time",
                description = "Show timing"
            ).default(false)
            parser.parse(args)
            println("RunVerifier starting\n   input= $inputDir")

            runVerifier(productionGroup(), inputDir, nthreads, showTime)
        }

        fun runVerifier(group: GroupContext, inputDir: String, nthreads: Int, showTime: Boolean = false): Boolean {
            val starting = getSystemTimeInMillis()

            val electionRecord = readElectionRecord(group, inputDir)
            val verifier = Verifier(electionRecord, nthreads)
            val stats = Stats()
            val allOk = verifier.verify(stats, showTime)
            if (showTime) {
                stats.show()
            }

            val tookAll = (getSystemTimeInMillis() - starting)
            println("RunVerifier = $allOk took $tookAll msecs alloK = ${allOk}")
            return allOk
        }

        fun verifyEncryptedBallots(group: GroupContext, inputDir: String, nthreads: Int) {
            val starting = getSystemTimeInMillis()

            val electionRecord = readElectionRecord(group, inputDir)
            val verifier = Verifier(electionRecord, nthreads)

            val stats = Stats()
            val allOk = verifier.verifyEncryptedBallots(stats)
            stats.show()

            val took = ((getSystemTimeInMillis() - starting) / 1000.0).sigfig()
            println("VerifyEncryptedBallots $allOk took $took seconds")
        }

        fun verifyDecryptedTally(group: GroupContext, inputDir: String) {
            val starting = getSystemTimeInMillis()

            val electionRecord = readElectionRecord(group, inputDir)
            val verifier = Verifier(electionRecord, 1)

            val decryptedTally = electionRecord.decryptedTally() ?: throw IllegalStateException("no decryptedTally ")
            val stats = Stats()
            val allOk = verifier.verifyDecryptedTally(decryptedTally, stats)
            stats.show()

            val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
            println("verifyDecryptedTally $allOk took $took seconds wallclock")
        }

        fun verifyChallengedBallots(group: GroupContext, inputDir: String) {
            val starting = getSystemTimeInMillis()

            val electionRecord = readElectionRecord(group, inputDir)
            val verifier = Verifier(electionRecord, 1)

            val stats = Stats()
            val allOk = verifier.verifySpoiledBallotTallies(stats)
            stats.show()

            val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
            println("verifyRecoveredShares $allOk took $took seconds wallclock")
        }

        fun verifyTallyBallotIds(group: GroupContext, inputDir: String) {
            val electionRecord = readElectionRecord(group, inputDir)
            println("$inputDir stage=${electionRecord.stage()} ncast_ballots=${electionRecord.encryptedTally()!!.castBallotIds.size}")

            val verifier = Verifier(electionRecord, 1)
            val allOk = verifier.verifyTallyBallotIds()
            println("   verifyTallyBallotIds= $allOk")
        }
    }
}
