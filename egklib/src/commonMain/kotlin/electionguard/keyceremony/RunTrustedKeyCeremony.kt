package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/**
 * Run KeyCeremony CLI.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 * A version of this where each Trustee is in its own process space is implemented in the webapps modules.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunTrustedKeyCeremony")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input ElectionConfig record"
    ).required()
    val trusteeDir by parser.option(
        ArgType.String,
        shortName = "trustees",
        description = "Directory to write private trustees"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output ElectionInitialized record"
    ).required()
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created"
    )
    parser.parse(args)
    println("RunTrustedKeyCeremony starting\n   input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")

    val group = productionGroup()
    val result = runKeyCeremony(group, inputDir, outputDir, trusteeDir, createdBy)
    println("runKeyCeremony result = $result")
}

fun runKeyCeremony(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    createdBy: String?
): Result<Boolean, String> {
    val starting = getSystemTimeInMillis()

    val consumerIn = makeConsumer(configDir, group)
    val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    // Generate the KeyCeremonyTrustees here, which means this is a trusted situation.
    val trustees: List<KeyCeremonyTrustee> = List(config.numberOfGuardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "trustee$seq", seq, config.quorum)
    }

    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        return exchangeResult
    }
    val keyCeremonyResults = exchangeResult.unwrap()
    val electionInitialized = keyCeremonyResults.makeElectionInitialized(
        config,
        mapOf(
            Pair("CreatedBy", createdBy ?: "runKeyCeremony"),
            Pair("CreatedFromDir", configDir),
        )
    )

    val publisher = makePublisher(outputDir)
    publisher.writeElectionInitialized(electionInitialized)

    // store the trustees in some private place.
    val trusteePublisher = makePublisher(trusteeDir)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    val took = getSystemTimeInMillis() - starting
    println("RunTrustedKeyCeremony took $took millisecs")
    return Ok(true)
}
