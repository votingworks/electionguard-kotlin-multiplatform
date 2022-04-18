@file:OptIn(ExperimentalCli::class)

package electionguard.tally

import electionguard.ballot.CiphertextTally
import electionguard.ballot.ElectionRecord
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required

/**
 * Run tally accumulation. Read election record from inputDir, write to outputDir.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunAccumulateTally")
    val inputDir by
        parser
            .option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
            )
            .required()
    val outputDir by
        parser
            .option(
                ArgType.String,
                shortName = "out",
                description = "Directory to write output election record"
            )
            .required()
    val name by
        parser.option(ArgType.String, shortName = "name", description = "Name of accumulation")
    parser.parse(args)

    val group = productionGroup()
    runAccumulateTally(group, inputDir, outputDir, name ?: "RunAccumulateTally")
}

fun runAccumulateTally(group: GroupContext, inputDir: String, outputDir: String, name: String) {
    val consumer = Consumer(inputDir, group)
    val electionRecord: ElectionRecord = consumer.readElectionRecord()
    val accumulator = AccumulateTally(group, electionRecord.manifest, name)
    for (submittedBallot in consumer.iterateSubmittedBallots()) {
        accumulator.addCastBallot(submittedBallot)
    }
    val tally: CiphertextTally = accumulator.build()

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionRecordProto(
        electionRecord.manifest,
        electionRecord.constants,
        electionRecord.context,
        electionRecord.guardianRecords,
        electionRecord.devices,
        consumer.iterateSubmittedBallots(),
        tally,
        null,
        null,
        null,
    )
}
