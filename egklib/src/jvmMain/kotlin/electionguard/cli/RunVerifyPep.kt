package electionguard.cli

import electionguard.core.*
import electionguard.pep.VerifierPep
import electionguard.publish.makeConsumer
import electionguard.publish.readElectionRecord
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/**
 * Run election record verification CLI.
 */
class RunVerifyPep {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunVerifyPep")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
            ).required()
            val pepDirectory by parser.option(
                ArgType.String,
                shortName = "pep",
                description = "Directory containing PEP output"
            ).required()
            val nthreads by parser.option(
                ArgType.Int,
                shortName = "nthreads",
                description = "Number of parallel threads to use"
            )
            parser.parse(args)
            println("RunVerifyPep starting\n   input= $inputDir")

            runVerifyPep(productionGroup(), inputDir, pepDirectory, nthreads ?: 11)
        }

        fun runVerifyPep(group: GroupContext, inputDir: String, pepDirectory : String, nthreads: Int): Boolean {
            val starting = getSystemTimeInMillis()

            val consumer = makeConsumer(group, inputDir)
            val record = readElectionRecord(consumer)
            val verifier = VerifierPep(group, record.extendedBaseHash()!!, ElGamalPublicKey(record.jointPublicKey()!!))

            var count = 0
            var allOk = true
            consumer.iteratePepBallots(pepDirectory).forEach { ballotPEP ->
                //     fun verify(ballotPEP: BallotPep): Result<Boolean, String> {
                val result = verifier.verify(ballotPEP)
                if (result is Err) {
                    println(result)
                    allOk = false
                } else {
                    println(" ${ballotPEP.ballotId} validates")
                }
                count++
            }

            val tookAll = (getSystemTimeInMillis() - starting)
            println("RunVerifier = $allOk took $tookAll msecs for $count ballots; allOk = ${allOk}")
            return allOk
        }
    }
}
