package electionguard.cli

import electionguard.core.*
import electionguard.rave.MixnetBallot
import electionguard.rave.publishJson
import electionguard.publish.makeConsumer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.FileOutputStream

class RunMakeMixnetInput {

    companion object {
        val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunMakeMixnetInput")
            val encryptedBallotsDir by parser.option(
                ArgType.String,
                shortName = "eballots",
                description = "Directory containing input encrypted ballots (EB)"
            ).required()
            val outputFile by parser.option(
                ArgType.String,
                shortName = "out",
                description = "Write to this filename"
            ).required()
            val isJson by parser.option(
                ArgType.Boolean,
                shortName = "json",
                description = "ENcrypted ballots are JSON"
            ).default(true)
            parser.parse(args)

            // create output directory if needed
            val outputDir = outputFile.substringBeforeLast("/")
            createDirectories(outputDir)

            runMakeMixnetInput(productionGroup(), encryptedBallotsDir, outputFile, isJson)
        }

        fun runMakeMixnetInput(group: GroupContext, encryptedBallotsDir: String, outputFile: String, isJson : Boolean) {
            val consumer = makeConsumer(group, encryptedBallotsDir, isJson)
            val mixnetBallots = mutableListOf<MixnetBallot>()
            consumer.iterateEncryptedBallotsFromDir(encryptedBallotsDir, null).forEach { encryptedBallot ->
                val ciphertexts = mutableListOf<ElGamalCiphertext>()
                ciphertexts.add(encryptedBallot.encryptedSn!!) // always the first one
                encryptedBallot.contests.forEach { contest ->
                    contest.selections.forEach { selection ->
                        ciphertexts.add(selection.encryptedVote)
                    }
                }
                mixnetBallots.add(MixnetBallot(ciphertexts))
            }

            val json = mixnetBallots.publishJson()
            FileOutputStream(outputFile).use { out ->
                jsonReader.encodeToStream(json, out)
            }
        }
    }
}