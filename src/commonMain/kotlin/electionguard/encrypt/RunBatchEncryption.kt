@file:OptIn(ExperimentalCli::class)

package electionguard.encrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.CiphertextBallot
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.SubmittedBallot
import electionguard.ballot.submit
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import electionguard.publish.SubmittedBallotSinkIF
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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
    val nthreads by parser.option(
        ArgType.Int,
        shortName = "nthreads",
        description = "Number of parellel threads to use")
    parser.parse(args)

    if (nthreads == 1) {
        batchEncryption(productionGroup(), inputDir, outputDir, ballotDir, invalidDir, fixedNonces?: false)
    } else {
        channelEncryption(productionGroup(), inputDir, outputDir, ballotDir, invalidDir,
            fixedNonces ?: false, nthreads?: 6)
    }
}

// single threaded
fun batchEncryption(group: GroupContext, inputDir: String, outputDir: String, ballotDir: String, invalidDir: String, fixedNonces: Boolean) {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val electionInit: ElectionInitialized = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

    val manifestValidator = ManifestInputValidation(electionInit.manifest())
    val errors = manifestValidator.validate()
    if (errors.hasErrors()) {
        println("*** ManifestInputValidation FAILED on election record in $inputDir")
        println("$errors")
        // kotlin.system.exitProcess(1) // kotlin 1.6.20
        return
    }

    val invalidBallots = ArrayList<PlaintextBallot>()
    val ballotValidator = BallotInputValidation(electionInit.manifest())
    val filteredBallots: Iterable<PlaintextBallot> = electionRecordIn.iteratePlaintextBallots(ballotDir) {
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
    val encryptor = Encryptor(group, electionInit.manifest(), ElGamalPublicKey(electionInit.jointPublicKey), electionInit.cryptoExtendedBaseHash)

    val encrypted: List<CiphertextBallot> =
        if (fixedNonces)
            encryptor.encryptWithFixedNonces(filteredBallots, electionInit.cryptoExtendedBaseHash.toElementModQ(group), group.TWO_MOD_Q)
        else
            encryptor.encrypt(filteredBallots, electionInit.cryptoExtendedBaseHash.toElementModQ(group))

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
    publisher.writeEncryptions(
        electionInit,
        submitted,
    )
    println("wrote ${submitted.size} submitted ballots to $outputDir")

    if (!invalidBallots.isEmpty()) {
        publisher.writeInvalidBallots(invalidDir, invalidBallots)
        println("wrote ${invalidBallots.size} invalid ballots to $invalidDir")
    }
    println("done")
}

// multi threaded
fun channelEncryption(group: GroupContext, inputDir: String, outputDir: String, ballotDir: String,
                      invalidDir: String, fixedNonces: Boolean, nthreads: Int) {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val electionInit: ElectionInitialized = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    val sink: SubmittedBallotSinkIF = publisher.submittedBallotSink()

    // ManifestInputValidation
    val manifestValidator = ManifestInputValidation(electionInit.manifest())
    val errors = manifestValidator.validate()
    if (errors.hasErrors()) {
        println("*** ManifestInputValidation FAILED on election record in $inputDir")
        println("$errors")
        // kotlin.system.exitProcess(1) // kotlin 1.6.20
        return
    }

    // BallotInputValidation
    val invalidBallots = ArrayList<PlaintextBallot>()
    val ballotValidator = BallotInputValidation(electionInit.manifest())
    val filter: ((PlaintextBallot) -> Boolean) = {
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

    val codeSeed: ElementModQ = electionInit.cryptoExtendedBaseHash.toElementModQ(group)
    val masterNonce = if (fixedNonces) group.TWO_MOD_Q else null
    val starting = getSystemTimeInMillis()
    val encryptor = Encryptor(group, electionInit.manifest(), ElGamalPublicKey(electionInit.jointPublicKey), electionInit.cryptoExtendedBaseHash)

    runBlocking {
        val outputChannel = Channel<CiphertextBallot>()
        val encryptorJobs = mutableListOf<Job>()
        val ballotProducer = produceBallots(electionRecordIn.iteratePlaintextBallots(ballotDir, filter))
        repeat(nthreads) {
            encryptorJobs.add(launchEncryptor(it, group, ballotProducer, encryptor, codeSeed, masterNonce, outputChannel))
        }
        launchSink(outputChannel, sink)

        // wait for all encryptions to be done, then close everything
        joinAll(*encryptorJobs.toTypedArray())
        outputChannel.close()
    }
    sink.close()

    val took = getSystemTimeInMillis() - starting
    val perBallot = (took.toDouble() / count).roundToInt()
    println("Took $took millisecs for ${count} ballots = $perBallot msecs/ballot")

    publisher.writeElectionInitialized(electionInit)
    if (!invalidBallots.isEmpty()) {
        publisher.writeInvalidBallots(invalidDir, invalidBallots)
        println("wrote ${invalidBallots.size} invalid ballots to $invalidDir")
    }
    println("done")
}

// place the ballot reading into its own coroutine
fun CoroutineScope.produceBallots(producer: Iterable<PlaintextBallot>): ReceiveChannel<PlaintextBallot> = produce {
    for (ballot in producer) {
        println("Producer sending plaintext ballot ${ballot.ballotId}")
        send(ballot)
        yield()
    }
    channel.close()
}

// multiple encryptors allow parallelism at the ballot level
// LOOK not possible to do ballot chaining
fun CoroutineScope.launchEncryptor(id: Int,
                                   group: GroupContext,
                                   input: ReceiveChannel<PlaintextBallot>,
                                   encryptor: Encryptor,
                                   codeSeed: ElementModQ,
                                   masterNonce: ElementModQ?,
                                   output: SendChannel<CiphertextBallot>,
) = launch(Dispatchers.Default) {
    for (ballot in input) {
        val encrypted = if (masterNonce != null) // make deterministic
            encryptor.encrypt(ballot, codeSeed, masterNonce, 0)
        else
            encryptor.encrypt(ballot, codeSeed, group.randomElementModQ())

        println(" Encryptor #$id sending ciphertext ballot ${encrypted.ballotId}")
        output.send(encrypted)
        yield()
    }
    println("Encryptor #$id done")
}

// place the ballot writing into its own coroutine
// LOOK we assume that this is thread confined
private var count = 0
fun CoroutineScope.launchSink(input: Channel<CiphertextBallot>, sink: SubmittedBallotSinkIF,
) = launch {
    for (ballot in input) {
        sink.writeSubmittedBallot(ballot.submit(SubmittedBallot.BallotState.CAST))
        println(" Sink wrote $count submitted ballot ${ballot.ballotId}")
        count++
    }
}

/* actor only available in the jvm
private var count = 0
fun CoroutineScope.counterActor(sink: SubmittedBallotSinkIF) = actor<CiphertextBallot> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        sink.writeSubmittedBallot(it.submit(SubmittedBallot.BallotState.CAST))
        println(" Sink wrote $count submitted ballot ${it.ballotId}")
        count++
    }
} */
