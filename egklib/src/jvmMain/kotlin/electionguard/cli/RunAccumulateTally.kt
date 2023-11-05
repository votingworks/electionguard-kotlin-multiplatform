package electionguard.cli

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.tally.AccumulateTally
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlin.math.roundToInt

/**
 * Run tally accumulation CLI.
 * Read election record from inputDir, write to outputDir.
 */
class RunAccumulateTally {

    companion object {
        private val logger = KotlinLogging.logger("RunAccumulateTally")

        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunAccumulateTally")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input ElectionInitialized record and encrypted ballots"
            ).required()
            val outputDir by parser.option(
                ArgType.String,
                shortName = "out",
                description = "Directory to write output election record"
            ).required()
            val name by parser.option(
                ArgType.String,
                shortName = "name",
                description = "Name of tally"
            )
            val createdBy by parser.option(
                ArgType.String,
                shortName = "createdBy",
                description = "who created"
            )
            parser.parse(args)
            println("RunAccumulateTally starting\n   input= $inputDir\n   output = $outputDir")

            val group = productionGroup()
            try {
                runAccumulateBallots(
                    group,
                    inputDir,
                    outputDir,
                    name ?: "RunAccumulateTally",
                    createdBy ?: "RunAccumulateTally"
                )
            } catch (t: Throwable) {
                logger.error { "Exception= ${t.message} ${t.stackTraceToString()}" }
            }
        }

        fun runAccumulateBallots(
            group: GroupContext,
            inputDir: String,
            outputDir: String,
            name: String,
            createdBy: String
        ) {
            val starting = getSystemTimeInMillis()

            val consumerIn = makeConsumer(group, inputDir)
            val initResult = consumerIn.readElectionInitialized()
            if (initResult is Err) {
                println("readElectionInitialized failed ${initResult.error}")
                return
            }
            val electionInit = initResult.unwrap()
            val manifest = consumerIn.makeManifest(electionInit.config.manifestBytes)

            var countBad = 0
            var countOk = 0
            val accumulator = AccumulateTally(group, manifest, name, electionInit.extendedBaseHash, electionInit.jointPublicKey())
            for (encryptedBallot in consumerIn.iterateAllCastBallots()) {
                val errs = ErrorMessages("RunAccumulateTally ballotId=${encryptedBallot.ballotId}")
                accumulator.addCastBallot(encryptedBallot, errs)
                if (errs.hasErrors()) {
                    println(errs)
                    logger.error{ errs.toString() }
                    countBad++
                } else {
                    countOk++
                }
            }
            val tally: EncryptedTally = accumulator.build()

            val publisher = makePublisher(outputDir, false, consumerIn.isJson())
            publisher.writeTallyResult(
                TallyResult(
                    electionInit, tally, listOf(name),
                    mapOf(
                        Pair("CreatedBy", createdBy),
                        Pair("CreatedOn", getSystemDate()),
                        Pair("CreatedFromDir", inputDir)
                    )
                )
            )

            val took = getSystemTimeInMillis() - starting
            val msecPerEncryption = if (countOk == 0) 0 else (took.toDouble() / countOk).roundToInt()
            println("AccumulateTally processed $countOk good ballots, $countBad bad ballots, took $took millisecs, $msecPerEncryption msecs per good ballot")
            println("  ballots ids accumulated = ${tally.castBallotIds.joinToString(",")}")
        }
    }
}
