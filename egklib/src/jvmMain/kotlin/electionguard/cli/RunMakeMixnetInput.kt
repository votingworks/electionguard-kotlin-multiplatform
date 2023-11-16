package electionguard.cli

import electionguard.core.*
import electionguard.mixnet.MixnetBallot
import electionguard.mixnet.publishJson
import electionguard.publish.makeConsumer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
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
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
            ).required()
            val outputFile by parser.option(
                ArgType.String,
                shortName = "out",
                description = "Write to this filename"
            ).required()
            parser.parse(args)

            // create output directory if needed
            val outputDir = outputFile.substringBeforeLast("/")
            createDirectories(outputDir)

            runMakeMixnetInput(productionGroup(), inputDir, outputFile)
        }

        fun runMakeMixnetInput(group: GroupContext, inputDir: String, outputFile: String) {
            val consumer = makeConsumer(group, inputDir)
            val mixnetBallots = mutableListOf<MixnetBallot>()
            consumer.iterateAllCastBallots().forEach { encryptedBallot ->
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