@file:OptIn(ExperimentalCli::class)

package electionguard.verifier

import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.electionRecordFromConsumer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
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
    val nthreads by parser.option(
        ArgType.Int,
        shortName = "nthreads",
        description = "Number of parallel threads to use"
    )
    val showTime by parser.option(
        ArgType.Boolean,
        shortName = "time",
        description = "Show timing"
    ).default(false)
    parser.parse(args)
    println("RunVerifier starting\n   input= $inputDir")

    runVerifier(productionGroup(), inputDir, nthreads?: 11, showTime)
}

fun runVerifier(group: GroupContext, inputDir: String, nthreads: Int, showTime : Boolean = false) {
    val starting = getSystemTimeInMillis()

    val electionRecord = electionRecordFromConsumer(Consumer(inputDir, group))
    val verifier = Verifier( electionRecord, nthreads)

    val took = (getSystemTimeInMillis() - starting)
    println("  runVerifier prep = took $took msecs")
    val allOk = verifier.verify(showTime)

    val tookAll = (getSystemTimeInMillis() - starting)
    println("RunVerifier = $allOk took $tookAll msecs alloK = ${allOk}")
}

fun verifyEncryptedBallots(group: GroupContext, inputDir: String, nthreads: Int) {
    val starting = getSystemTimeInMillis()

    val electionRecord = electionRecordFromConsumer(Consumer(inputDir, group))
    val verifier = Verifier(electionRecord, nthreads)

    val allOk = verifier.verifyEncryptedBallots()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("VerifyEncryptedBallots $allOk took $took seconds")
}

fun verifyDecryptedTally(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecord = electionRecordFromConsumer(Consumer(inputDir, group))
    val verifier = Verifier(electionRecord, 1)

    val decryptedTally = electionRecord.decryptedTally() ?: throw IllegalStateException("no decryptedTally ")
    val allOk = verifier.verifyDecryptedTally(decryptedTally)

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("verifyDecryptedTally $allOk took $took seconds")
}

fun verifyRecoveredShares(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecord = electionRecordFromConsumer(Consumer(inputDir, group))
    val verifier = Verifier(electionRecord, 1)

    val allOk = verifier.verifyRecoveredShares()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("verifyRecoveredShares $allOk took $took seconds")
}

fun verifySpoiledBallotTallies(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecord = electionRecordFromConsumer(Consumer(inputDir, group))
    val verifier = Verifier(electionRecord, 1)

    val allOk = verifier.verifySpoiledBallotTallies()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("verifyRecoveredShares $allOk took $took seconds")
}
