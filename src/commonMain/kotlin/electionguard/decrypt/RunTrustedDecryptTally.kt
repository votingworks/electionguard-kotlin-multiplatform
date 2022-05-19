@file:OptIn(ExperimentalCli::class)

package electionguard.decrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
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
 * Run DecryptingMediator for Tally CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunTrustedDecryptTally")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    val trusteeDir by parser.option(
        ArgType.String,
        shortName = "trustees",
        description = "Directory to read private trustees"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created"
    )
    parser.parse(args)
    println("RunTrustedDecryptTally starting\n   input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")

    val group = productionGroup()
    runDecryptingMediator(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir), createdBy)
}

fun readDecryptingTrustees(group: GroupContext, inputDir: String, trusteeDir: String): List<DecryptingTrusteeIF> {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val init = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
    val consumer = ElectionRecord(trusteeDir, group)
    return init.guardians.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}

fun runDecryptingMediator(
    group: GroupContext,
    inputDir: String,
    outputDir: String,
    decryptingTrustees: List<DecryptingTrusteeIF>,
    createdBy: String?
) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val tallyResult: TallyResult = electionRecordIn.readTallyResult().getOrThrow { IllegalStateException(it) }
    val trusteeNames = decryptingTrustees.map { it.id()}.toSet()
    val missingGuardians =
        tallyResult.electionIntialized.guardians.filter { !trusteeNames.contains(it.guardianId)}.map { it.guardianId}

    val decryptor = DecryptingMediator(group, tallyResult, decryptingTrustees, missingGuardians)
    val decryptedTally = with(decryptor) { tallyResult.ciphertextTally.decrypt() }

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeDecryptionResult(
        DecryptionResult(
            tallyResult,
            decryptedTally,
            decryptor.availableGuardians,
            mapOf(
                Pair("CreatedBy", createdBy ?: "RunTrustedDecryptTally"),
                Pair("CreatedOn", getSystemDate().toString()),
                Pair("CreatedFromDir", inputDir))
        )
    )

    val took = getSystemTimeInMillis() - starting
    println("RunTrustedDecryptTally took $took millisecs")
}
