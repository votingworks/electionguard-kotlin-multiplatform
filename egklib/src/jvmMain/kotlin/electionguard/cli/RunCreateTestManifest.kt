package electionguard.cli

import electionguard.publish.makePublisher
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

/** Create Test Manifest CLI. */
class RunCreateTestManifest {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunCreateTestManifest")
            val ncontests by parser.option(
                ArgType.Int,
                shortName = "ncontests",
                description = "number of contests"
            ).required()
            val nselections by parser.option(
                ArgType.Int,
                shortName = "nselections",
                description = "number of selections per contest"
            ).required()
            val outputType by parser.option(
                ArgType.String,
                shortName = "type",
                description = "JSON or PROTO"
            ).default("JSON")
            val outputDir by parser.option(
                ArgType.String,
                shortName = "out",
                description = "Directory to write output Manifest"
            ).required()
            parser.parse(args)

            println(
                "RunCreateTestManifest starting\n" +
                        "   ncontests= $ncontests\n" +
                        "   nselections= $nselections\n" +
                        "   outputType= $outputType\n" +
                        "   output = $outputDir\n"
            )

            val manifest = buildTestManifest(ncontests, nselections)
            val publisher = makePublisher(outputDir, true, outputType == "JSON")
            publisher.writeManifest(manifest)

            println("RunCreateTestManifest success")
        }
    }
}