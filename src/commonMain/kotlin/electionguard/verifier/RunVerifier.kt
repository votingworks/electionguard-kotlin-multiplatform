@file:OptIn(ExperimentalCli::class)

package electionguard.verifier

import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
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
    val parser = ArgParser("RunVerifier")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    parser.parse(args)
    println("RunVerifier starting\n   input= $inputDir")

    runVerifier(productionGroup(), inputDir)
}

fun runVerifier(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val verifier = Verifier(group, electionRecordIn)

    val allOk = verifier.verify()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("RunVerifier $allOk took $took seconds")
}
