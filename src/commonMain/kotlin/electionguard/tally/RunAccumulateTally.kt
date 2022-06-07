@file:OptIn(ExperimentalCli::class)

package electionguard.tally

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.EncryptedTally
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required
import kotlin.math.roundToInt

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

    val consumerIn = Consumer(inputDir, group)
    val electionInit: ElectionInitialized = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

    var count = 0
    val accumulator = AccumulateTally(group, electionInit.manifest(), name)
    for (encryptedBallot in consumerIn.iterateCastBallots() ) {
        accumulator.addCastBallot(encryptedBallot)
        count++
    }
    val tally: EncryptedTally = accumulator.build()

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeTallyResult(
        TallyResult( group, electionInit, tally, accumulator.ballotIds(), emptyList(),
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
