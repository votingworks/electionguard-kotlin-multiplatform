package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/**
 * Convert election record in inputDir, to opposite type, writing to outputDir.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunElectionRecordConvert")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input Election Record"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory containing output Election Record"
    ).required()
    val createNew by parser.option(
        ArgType.Boolean,
        shortName = "createNew",
        description = "should output directly be wiped clean first"
    )
    parser.parse(args)
    println("RunElectionRecordConvert starting\n   input= $inputDir\n   output = $outputDir")

    val group = productionGroup()
    runElectionRecordConvert(group, inputDir, outputDir, createNew ?: false)
}

fun runElectionRecordConvert(group: GroupContext, inputDir: String, outputDir: String, createNew : Boolean) {
    val consumer = makeConsumer(inputDir, group)
    val publisher = makePublisher(outputDir, createNew, !consumer.isJson())
    val electionRecord = readElectionRecord(consumer)

    //publisher.writeManifestFile(electionRecord.manifestFile())
    //println(" manifestFile written")

    // TODO problem is that manifest bytes here are proto
    //   that is, writeElectionConfig assumes that you are keeping the same type
    //   manifest bytes must not change change, else wont validate.
    //   only solution is to use original manifest
    publisher.writeElectionConfig(electionRecord.config())
    println(" config written")

    val init = electionRecord.electionInit()
    if (init != null) {
        publisher.writeElectionInitialized(init)
        println(" electionInit written")
    }

    var ecount = 0
    val esink = publisher.encryptedBallotSink("fake")
    try {
        consumer.iterateAllEncryptedBallots { true }.forEach() {
            esink.writeEncryptedBallot(it)
            ecount++
        }
    } finally {
        esink.close()
    }
    println(" $ecount encryptedBallots written")

    val tally = electionRecord.tallyResult()
    if (tally != null) {
        publisher.writeTallyResult(tally)
        println(" tally result written")
    }

    val dtally = electionRecord.decryptionResult()
    if (dtally != null) {
        publisher.writeDecryptionResult(dtally)
        println(" decrypted tally result written")
    }

    var dcount = 0
    val dsink = publisher.decryptedTallyOrBallotSink()
    try {
        consumer.iterateDecryptedBallots().forEach() {
            dsink.writeDecryptedTallyOrBallot(it)
            dcount++
        }
    } finally {
        dsink.close()
    }
    println(" $dcount decryptedBallots written")
}
