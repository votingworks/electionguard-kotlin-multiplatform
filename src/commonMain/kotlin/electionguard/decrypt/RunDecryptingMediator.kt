@file:OptIn(ExperimentalCli::class)

package electionguard.decrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required

/**
 * Run DecryptingMediator CLI.
 * Read election record from inputDir, write to outputDir.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("runDecryptingMediator")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    val trusteeDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to read trustees from"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    parser.parse(args)

    val group = productionGroup()
    runDecryptingMediator(group, inputDir, trusteeDir, outputDir)
}

fun runDecryptingMediator(group: GroupContext, inputDir: String, trusteeDir: String, outputDir: String) {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val tallyResult: TallyResult = electionRecordIn.readTallyResult().getOrThrow { IllegalStateException( it ) }
    val guardians: List<DecryptingTrusteeIF> = readDecryptingTrustees(group, inputDir, trusteeDir)
    val decryptor = DecryptingMediator(group, tallyResult, guardians, emptyList())
    val decryptedTally = with (decryptor) { tallyResult.ciphertextTally.decrypt() }

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeDecryptionResult(
        DecryptionResult(
            tallyResult,
            decryptedTally,
            decryptor.availableGuardians,
        )
    )
}

fun readDecryptingTrustees(group: GroupContext, inputDir: String, trusteeDir: String): List<DecryptingTrusteeIF> {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val init = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
    val consumer = ElectionRecord(trusteeDir, group)
    return init.guardians.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}
