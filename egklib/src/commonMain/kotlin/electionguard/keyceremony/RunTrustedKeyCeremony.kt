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
import electionguard.publish.readManifest
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
    )
    val electionManifest by parser.option(
        ArgType.String,
        shortName = "manifest",
        description = "Manifest file or directory (json or protobuf)"
    )
    val nguardians by parser.option(
        ArgType.Int,
        shortName = "nguardians",
        description = "number of guardians"
    )
    val quorum by parser.option(
        ArgType.Int,
        shortName = "quorum",
        description = "quorum size"
    )
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
    var createdFrom : String

    val config: ElectionConfig = if (electionManifest != null && nguardians != null && quorum != null) {
        val manifest = readManifest(electionManifest!!, group)
        createdFrom = electionManifest!!
        println(
            "RunRemoteKeyCeremony\n" +
                    "  electionManifest = '$electionManifest'\n" +
                    "  nguardians = $nguardians quorum = $quorum\n" +
                    "  outputDir = '$outputDir'\n"
        )
        ElectionConfig(group.constants, manifest.unwrap(), nguardians!!, quorum!!,
            mapOf(
                Pair("CreatedBy", createdBy ?: "RunRemoteKeyCeremony"),
                Pair("CreatedFromElectionManifest", electionManifest!!),
            ))
    } else {
        val consumerIn = makeConsumer(inputDir!!, group)
        createdFrom = inputDir!!
        println(
            "RunRemoteKeyCeremony\n" +
                    "  inputDir = '$inputDir'\n" +
                    "  outputDir = '$outputDir'\n"
        )
        consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }
    }

    val result = runKeyCeremony(group, createdFrom, config, outputDir, trusteeDir, createdBy)
    println("runKeyCeremony result = $result")
}

fun runKeyCeremony(
    group: GroupContext,
    createdFrom: String,
    config: ElectionConfig,
    outputDir: String,
    trusteeDir: String,
    createdBy: String?
): Result<Boolean, String> {
    val starting = getSystemTimeInMillis()

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
            Pair("CreatedFrom", createdFrom),
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
