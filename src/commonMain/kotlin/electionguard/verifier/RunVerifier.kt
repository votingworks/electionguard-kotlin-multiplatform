@file:OptIn(ExperimentalCli::class)

package electionguard.verifier

import com.github.michaelbull.result.getOrThrow
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
    val nthreads by parser.option(
        ArgType.Int,
        shortName = "nthreads",
        description = "Number of parallel threads to use"
    )
    parser.parse(args)
    println("RunVerifier starting\n   input= $inputDir")

    runVerifier(productionGroup(), inputDir, nthreads?: 11)
}

fun runVerifier(group: GroupContext, inputDir: String, nthreads: Int) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val verifier = Verifier(group, electionRecordIn, nthreads)

    val allOk = verifier.verify()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("RunVerifier $allOk took $took seconds")
}

fun verifyEncryptedBallots(group: GroupContext, inputDir: String, nthreads: Int) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val verifier = Verifier(group, electionRecordIn, nthreads)

    val allOk = verifier.verifyEncryptedBallots()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("VerifyEncryptedBallots $allOk took $took seconds")
}

fun verifyDecryptedTally(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecord = ElectionRecord(inputDir, group)
    val verifier = Verifier(group, electionRecord, 1)

    val decryptionResult = electionRecord.readDecryptionResult().getOrThrow { throw IllegalStateException(it) }
    val allOk = verifier.verifyDecryptedTally(decryptionResult.decryptedTally)

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("verifyDecryptedTally $allOk took $took seconds")
}

fun verifyRecoveredShares(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecord = ElectionRecord(inputDir, group)
    val verifier = Verifier(group, electionRecord, 1)

    val decryptionResult = electionRecord.readDecryptionResult().getOrThrow { throw IllegalStateException(it) }
    val allOk = verifier.verifyRecoveredShares(decryptionResult)

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("verifyRecoveredShares $allOk took $took seconds")
}

fun verifySpoiledBallotTallies(group: GroupContext, inputDir: String) {
    val starting = getSystemTimeInMillis()

    val electionRecord = ElectionRecord(inputDir, group)
    val verifier = Verifier(group, electionRecord, 1)

    val allOk = verifier.verifySpoiledBallotTallies()

    val took = ((getSystemTimeInMillis() - starting) / 1000.0).roundToInt()
    println("verifyRecoveredShares $allOk took $took seconds")
}
