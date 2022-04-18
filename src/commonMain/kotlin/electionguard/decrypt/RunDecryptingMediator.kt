@file:OptIn(ExperimentalCli::class)

package electionguard.decrypt

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
    val parser = ArgParser("runDecryptingMediator")
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
    parser.parse(args)

    val group = productionGroup()
    runDecryptingMediator(group, inputDir, outputDir)
}

fun runDecryptingMediator(group: GroupContext, inputDir: String, outputDir: String) {
    val consumer = Consumer(inputDir, group)
    val electionRecord: ElectionRecord = consumer.readElectionRecord()
    require(electionRecord.context != null)
    require(electionRecord.encryptedTally != null)
    val decryptor = DecryptingMediator(group, electionRecord.context, emptyList())
    val decryptedTally = with (decryptor) { electionRecord.encryptedTally.decrypt() }

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionRecordProto(
        electionRecord.manifest,
        electionRecord.constants,
        electionRecord.context,
        electionRecord.guardianRecords,
        electionRecord.devices,
        consumer.iterateSubmittedBallots(),
        electionRecord.encryptedTally,
        decryptedTally,
        null,
        null,
    )
}
