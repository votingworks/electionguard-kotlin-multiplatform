package electionguard.tally

import electionguard.ballot.EncryptedTally
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.readElectionRecord
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlin.math.roundToInt

/**
 * Run tally accumulation CLI.
 * Read election record from inputDir, write to outputDir.
 */
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
    runAccumulateBallots(group, inputDir, outputDir, name?: "RunAccumulateTally", createdBy?: "RunAccumulateTally")
}

fun runAccumulateBallots(group: GroupContext, inputDir: String, outputDir: String, name: String, createdBy: String) {
    val starting = getSystemTimeInMillis()

    val consumerIn = makeConsumer(inputDir, group)
    val electionRecord = readElectionRecord(consumerIn)
    val electionInit = electionRecord.electionInit()!!

    var count = 0
    val accumulator = AccumulateTally(group, electionRecord.manifest(), name)
    for (encryptedBallot in consumerIn.iterateAllCastBallots() ) {
        accumulator.addCastBallot(encryptedBallot)
        println(" accumulate ${encryptedBallot.ballotId}")
        count++
    }
    val tally: EncryptedTally = accumulator.build()

    val publisher = makePublisher(outputDir, false, electionRecord.isJson())
    publisher.writeTallyResult(
        TallyResult( electionInit, tally, listOf(name),
            mapOf(
                Pair("CreatedBy", createdBy),
                Pair("CreatedOn", getSystemDate().toString()),
                Pair("CreatedFromDir", inputDir))
            )
    )

    val took = getSystemTimeInMillis() - starting
    val msecPerEncryption = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
    println("AccumulateTally processed $count ballots, took $took millisecs, $msecPerEncryption msecs per ballot")
}
