package electionguard.cli

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.DecryptorDoerre
import electionguard.decrypt.Guardians
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.makeTrusteeSource
import electionguard.publish.readElectionRecord
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/**
 * Run Trusted Tally Decryption CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 * A version of this where each Trustee is in its own process space is implemented in the webapps modules.
 */
class RunTrustedTallyDecryption {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunTrustedTallyDecryption")
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
            val missing by parser.option(
                ArgType.String,
                shortName = "missing",
                description = "missing guardians' xcoord, comma separated, eg '2,4'"
            )
            parser.parse(args)
            println("RunTrustedTallyDecryption starting\n   input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")

            val group = productionGroup()
            runDecryptTally(
                group,
                inputDir,
                outputDir,
                readDecryptingTrustees(group, inputDir, trusteeDir, missing),
                createdBy
            )
        }

        fun readDecryptingTrustees(
            group: GroupContext,
            inputDir: String,
            trusteeDir: String,
            missing: String? = null
        ): List<DecryptingTrusteeIF> {
            val consumerIn = makeConsumer(inputDir, group)
            val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val trusteeSource = makeTrusteeSource(trusteeDir, group, consumerIn.isJson())
            val allGuardians = init.guardians.map { trusteeSource.readTrustee(trusteeDir, it.guardianId) }
            if (missing.isNullOrEmpty()) {
                return allGuardians
            }
            // remove missing guardians
            val missingX = missing.split(",").map { it.toInt() }
            return allGuardians.filter { !missingX.contains(it.xCoordinate()) }
        }

        fun runDecryptTally(
            group: GroupContext,
            inputDir: String,
            outputDir: String,
            decryptingTrustees: List<DecryptingTrusteeIF>,
            createdBy: String?
        ) {
            val starting = getSystemTimeInMillis()

            val electionRecord = readElectionRecord(group, inputDir)
            val electionInit = electionRecord.electionInit()!!
            val tallyResult: TallyResult = electionRecord.tallyResult()!!

            val trusteeNames = decryptingTrustees.map { it.id() }.toSet()
            val missingGuardians =
                electionInit.guardians.filter { !trusteeNames.contains(it.guardianId) }.map { it.guardianId }
            println("runDecryptTally present = $trusteeNames missing = $missingGuardians")

            val guardians = Guardians(group, electionInit.guardians)
            val decryptor = DecryptorDoerre(
                group,
                electionInit.extendedBaseHash,
                electionInit.jointPublicKey(),
                guardians,
                decryptingTrustees,
            )
            val decryptedTally = with(decryptor) { tallyResult.encryptedTally.decrypt() }

            val publisher = makePublisher(outputDir, false, electionRecord.isJson())
            publisher.writeDecryptionResult(
                DecryptionResult(
                    tallyResult,
                    decryptedTally,
                    mapOf(
                        Pair("CreatedBy", createdBy ?: "RunTrustedTallyDecryption"),
                        Pair("CreatedOn", getSystemDate()),
                        Pair("CreatedFromDir", inputDir)
                    )
                )
            )

            val took = getSystemTimeInMillis() - starting
            println("DecryptTally took $took millisecs")
        }
    }
}
