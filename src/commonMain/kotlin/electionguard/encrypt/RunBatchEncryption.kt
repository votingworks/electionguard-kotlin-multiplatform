@file:OptIn(ExperimentalCli::class)

package electionguard.encrypt

import electionguard.ballot.CiphertextBallot
import electionguard.ballot.ElectionRecord
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.SubmittedBallot
import electionguard.ballot.submit
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.toElementModQ
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required

/**
 * Run ballot encryption in batch mode.
 * Read election record from inputDir, write to outputDir.
 * Read plaintext ballots from ballotDir.
 * All ballots will be cast.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunBatchEncryption")
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
    val ballotDir by parser.option(
        ArgType.String,
        shortName = "ballots",
        description = "Directory to read Plaintext ballots from"
    ).required()
    // hmmm not used, wtf?
    val device by parser.option(ArgType.String, shortName = "device", description = "Name of encryption device")
        .required()
    parser.parse(args)

    val group = productionGroup()
    runBatchEncryption(inputDir, outputDir, ballotDir, group)
}

fun runBatchEncryption(inputDir: String, outputDir: String, ballotDir: String, group: GroupContext) {
    val consumer = Consumer(inputDir, group)
    val electionRecord: ElectionRecord = consumer.readElectionRecord()
    val context =
        electionRecord.context ?: throw IllegalStateException("election record.context is missing in $inputDir")

    val ballots: Iterable<PlaintextBallot> = consumer.iteratePlaintextBallots(ballotDir)
    val encryptor = Encryptor(group, electionRecord.manifest, context)
    val encrypted: List<CiphertextBallot> =
        encryptor.encrypt(ballots, context.cryptoExtendedBaseHash.toElementModQ(group))
    val submitted: List<SubmittedBallot> = encrypted.map { it.submit(SubmittedBallot.BallotState.CAST) }

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionRecordProto(
        electionRecord.manifest,
        electionRecord.constants,
        electionRecord.context,
        electionRecord.guardianRecords,
        electionRecord.devices,
        submitted,
        null,
        null,
        null,
        null,
    )
}
