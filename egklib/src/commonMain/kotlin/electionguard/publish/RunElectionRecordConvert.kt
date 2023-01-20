package electionguard.publish

import electionguard.core.GroupContext
import electionguard.core.productionGroup
import io.ktor.utils.io.core.use
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
    val producer = makePublisher(outputDir, createNew, !consumer.isJson())
    val electionRecord = electionRecordFromConsumer(consumer)

    val config = electionRecord.config()
    producer.writeElectionConfig(config)
    println(" config written")

    val init = electionRecord.electionInit()
    if (init != null) {
        producer.writeElectionInitialized(init)
        println(" electionInit written")
    }

    var ecount = 0
    producer.encryptedBallotSink().use { esink ->
        consumer.iterateEncryptedBallots { true }.forEach() {
            esink.writeEncryptedBallot(it)
            ecount++
        }
    }
    println(" $ecount encryptedBallots written")

    val tally = electionRecord.tallyResult()
    if (tally != null) {
        producer.writeTallyResult(tally)
        println(" tally result written")
    }

    val dtally = electionRecord.decryptionResult()
    if (dtally != null) {
        producer.writeDecryptionResult(dtally)
        println(" decrypted tally result written")
    }

    var dcount = 0
    producer.decryptedTallyOrBallotSink().use { dsink ->
        consumer.iterateDecryptedBallots().forEach() {
            dsink.writeDecryptedTallyOrBallot(it)
            dcount++
        }
    }
    println(" $dcount decryptedBallots written")
}
