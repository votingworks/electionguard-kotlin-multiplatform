@file:OptIn(ExperimentalCli::class)

package electionguard.encrypt

import electionguard.ballot.CiphertextBallot
import electionguard.ballot.ElectionRecord
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.SubmittedBallot
import electionguard.ballot.submit
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.toElementModQ
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required
import kotlin.math.roundToInt

/**
 * Run ballot encryption in batch mode.
 * Read election record from inputDir, write to outputDir.
 * Read plaintext ballots from ballotDir.
 * All ballots will be cast.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunBatchEncryption")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    val ballotDir by parser.option(
        ArgType.String,
        shortName = "ballots",
        description = "Directory to read Plaintext ballots from"
    ).required()
    val invalidDir by parser.option(
        ArgType.String,
        shortName = "invalidBallots",
        description = "Directory to write invalid Plaintext ballots to"
    ).required()
    val fixedNonces by parser.option(
        ArgType.Boolean,
        shortName = "fixedNonces",
        description = "Encrypt with fixed nonces and timestamp")
    parser.parse(args)

    runBatchEncryption(productionGroup(), inputDir, outputDir, ballotDir, invalidDir, fixedNonces?: false)
}

fun runBatchEncryption(group: GroupContext, inputDir: String, outputDir: String, ballotDir: String, invalidDir: String, fixedNonces: Boolean) {
    val consumer = Consumer(inputDir, group)
    val electionRecord: ElectionRecord = consumer.readElectionRecord()

    val manifestValidator = ManifestInputValidation(electionRecord.manifest)
    val errors = manifestValidator.validate()
    if (errors.hasErrors()) {
        println("*** ManifestInputValidation FAILED on election record in $inputDir")
        println("$errors")
        // kotlin.system.exitProcess(1) // kotlin 1.6.20
        return
    }
    val context =
        electionRecord.context ?: throw IllegalStateException("election record.context is missing in $inputDir")

    val invalidBallots = ArrayList<PlaintextBallot>()
    val ballotValidator = BallotInputValidation(electionRecord.manifest)
    val ballots: Iterable<PlaintextBallot> = consumer.iteratePlaintextBallots(ballotDir)
    val filteredBallots = ballots.filter {
        val mess = ballotValidator.validate(it)
        if (mess.hasErrors()) {
            println("*** BallotInputValidation FAILED on ballot ${it.ballotId}")
            println("$mess\n")
            invalidBallots.add(PlaintextBallot(it, mess.toString()))
            false
        } else {
            true
        }
    }

    val starting = getSystemTimeInMillis()
    val encryptor = Encryptor(group, electionRecord.manifest, ElGamalPublicKey(context.jointPublicKey), context.cryptoExtendedBaseHash)

    val encrypted: List<CiphertextBallot> =
        if (fixedNonces)
            encryptor.encryptWithFixedNonces(filteredBallots, context.cryptoExtendedBaseHash.toElementModQ(group), group.TWO_MOD_Q)
        else
            encryptor.encrypt(filteredBallots, context.cryptoExtendedBaseHash.toElementModQ(group))

    val took = getSystemTimeInMillis() - starting
    val perBallot = (took.toDouble() / encrypted.size).roundToInt()
    val ncontests: Int = encrypted.map {it.contests}.flatten().count()
    val nselections: Int = encrypted.map { it.contests}.flatten().map{ it.selections }.flatten().count()
    val perContest = (took.toDouble() / ncontests).roundToInt()
    val perSelection = (took.toDouble() / nselections).roundToInt()
    println("Took $took millisecs for ${encrypted.size} ballots = $perBallot msecs/ballot")
    println("   $ncontests contests $perContest msecs/contest")
    println("   $nselections selections $perSelection msecs/selection")
    println()

    val submitted: List<SubmittedBallot> = encrypted.map { it.submit(SubmittedBallot.BallotState.CAST) }

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionRecordProto(
        electionRecord.manifest,
        electionRecord.constants,
        electionRecord.context,
        electionRecord.guardianRecords,
        electionRecord.devices,
        submitted,
        null,
        null,
        null,
        null,
    )
    println("wrote ${submitted.size} submitted ballots to $outputDir")

    if (!invalidBallots.isEmpty()) {
        publisher.writeInvalidBallots(invalidDir, invalidBallots)
        println("wrote ${invalidBallots.size} invalid ballots to $invalidDir")
    }
    println("done")
}
