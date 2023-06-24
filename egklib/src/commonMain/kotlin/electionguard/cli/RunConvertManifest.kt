package electionguard.cli

import electionguard.core.productionGroup
import electionguard.publish.makePublisher
import electionguard.publish.readAndCheckManifestBytes
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/** Convert Manifest CLI. */
fun convertManifest(args: Array<String>) {
    val parser = ArgParser("RunConvertManifest")
    val electionManifest by parser.option(
        ArgType.String,
        shortName = "manifest",
        description = "Input manifest file or directory (json or protobuf)"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output Manifest"
    ).required()
    parser.parse(args)

    println("RunConvertManifest starting\n" +
            "   manifest= $electionManifest\n" +
            "   output = $outputDir\n"
    )

    val group = productionGroup()

    val (isJson, manifest, _) = readAndCheckManifestBytes(group, electionManifest)

    val publisher = makePublisher(outputDir, true, !isJson)

    publisher.writeManifest(manifest)

    println("RunConvertManifest success, outputType = ${ if (!isJson) "JSON" else "PROTO" }")
}
