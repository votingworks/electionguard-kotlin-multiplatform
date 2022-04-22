@file:OptIn(ExperimentalCli::class)

package electionguard.tally

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.CiphertextTally
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
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
        description = "Directory containing input election record"
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
    runAccumulateTally(group, inputDir, outputDir, name?: "RunAccumulateTally")
}

fun runAccumulateTally(group: GroupContext, inputDir: String, outputDir: String, name: String) {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val electionInit: ElectionInitialized = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

    val accumulator = AccumulateTally(group, electionInit.manifest(), name)
    for (submittedBallot in electionRecordIn.iterateSubmittedBallots()) {
        accumulator.addCastBallot(submittedBallot)
    }
    val tally: CiphertextTally = accumulator.build()

    val electionRecordOut = ElectionRecord(outputDir, group, PublisherMode.createIfMissing)
    electionRecordOut.writeTallyResult(
        TallyResult( group, electionInit, tally)
    )
}
