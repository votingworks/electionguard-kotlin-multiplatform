package electionguard.cli

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

/**
 * Run KeyCeremony CLI.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 * A version of this where each Trustee is in its own process space is implemented in the webapps modules.
 */
class RunTrustedKeyCeremony {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunTrustedKeyCeremony")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
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
            ).default("RunTrustedKeyCeremony")
            parser.parse(args)
            println("RunTrustedKeyCeremony starting\n   input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")

            val group = productionGroup()
            val electionRecord = readElectionRecord(group, inputDir)

            val result = runKeyCeremony(
                group,
                inputDir,
                electionRecord.config(),
                outputDir,
                trusteeDir,
                electionRecord.isJson(),
                createdBy
            )
            println("runKeyCeremony result = $result")
            require(result is Ok)
        }

        fun runKeyCeremony(
            group: GroupContext,
            createdFrom: String,
            config: ElectionConfig,
            outputDir: String,
            trusteeDir: String,
            isJson: Boolean,
            createdBy: String?
        ): Result<Boolean, String> {
            val starting = getSystemTimeInMillis()

            // Generate all KeyCeremonyTrustees here, which means this is a trusted situation.
            val trustees: List<KeyCeremonyTrustee> = List(config.numberOfGuardians) {
                val seq = it + 1
                KeyCeremonyTrustee(group, "trustee$seq", seq, nguardians = config.numberOfGuardians, quorum = config.quorum )
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

            val publisher = makePublisher(outputDir, false, isJson)
            publisher.writeElectionInitialized(electionInitialized)

            // store the trustees in some private place.
            val trusteePublisher = makePublisher(trusteeDir, false, isJson)
            trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it, electionInitialized.extendedBaseHash) }

            val took = getSystemTimeInMillis() - starting
            println("RunTrustedKeyCeremony took $took millisecs")
            return Ok(true)
        }
    }
}
