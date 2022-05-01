@file:OptIn(ExperimentalCli::class)

package electionguard.tally

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.CiphertextTally
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required

/**
 * Run tally accumulation.
 * Read election record from inputDir, write to outputDir.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunAccumulateTally")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record and encrypted ballots"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    val name by parser.option(
        ArgType.String,
        shortName = "name",
        description = "Name of accumulation"
    )
    parser.parse(args)

    val group = productionGroup()
    runAccumulateBallots(group, inputDir, outputDir, name?: "RunAccumulateTally")
}

fun runAccumulateBallots(group: GroupContext, inputDir: String, outputDir: String, name: String) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val electionInit: ElectionInitialized = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

    var count = 0
    val accumulator = AccumulateTally(group, electionInit.manifest(), name)
    for (submittedBallot in electionRecordIn.iterateSubmittedBallots()) {
        accumulator.addCastBallot(submittedBallot)
        count++
    }
    val tally: CiphertextTally = accumulator.build()

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeTallyResult(
        TallyResult( group, electionInit, tally, accumulator.ballotIds(), emptyList())
    )
    val took = getSystemTimeInMillis() - starting
    println("AccumulateTally processed $count ballots, took $took millisecs")
}
