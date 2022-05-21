@file:OptIn(ExperimentalCli::class)

package electionguard.decryptBallot

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.fileExists
import electionguard.core.fileReadLines
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingMediator
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.readDecryptingTrustees
import electionguard.publish.ElectionRecord
import electionguard.publish.PlaintextTallySinkIF
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import mu.KotlinLogging

private val logger = KotlinLogging.logger("RunTrustedBallotDecryption")

/**
 * Run Trusted Ballot Decryption CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunTrustedBallotDecryption")
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
    val decryptSpoiledList by parser.option(
        ArgType.String,
        shortName = "spoiled",
        description = "decrypt spoiled ballots"
    )
    val nthreads by parser.option(
        ArgType.Int,
        shortName = "nthreads",
        description = "Number of parallel threads to use"
    )
    parser.parse(args)
    println(
        "RunTrustedBallotDecryption starting\n   input= $inputDir\n   trustees= $trusteeDir\n" +
                "   decryptSpoiledList = $decryptSpoiledList\n   output = $outputDir"
    )

    val group = productionGroup()
    runDecryptBallots(
        group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir),
        decryptSpoiledList, nthreads ?: 11
    )
}

fun runDecryptBallots(
    group: GroupContext,
    inputDir: String,
    outputDir: String,
    decryptingTrustees: List<DecryptingTrusteeIF>,
    decryptSpoiledList: String?,
    nthreads: Int,
) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val tallyResult: TallyResult = electionRecordIn.readTallyResult().getOrThrow { IllegalStateException(it) }
    val trusteeNames = decryptingTrustees.map { it.id() }.toSet()
    val missingGuardians =
        tallyResult.electionIntialized.guardians.filter { !trusteeNames.contains(it.guardianId) }.map { it.guardianId }

    val decryptor = DecryptingMediator(group, tallyResult, decryptingTrustees, missingGuardians)

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    val sink: PlaintextTallySinkIF = publisher.plaintextTallySink()

    var ballotIter: Iterable<EncryptedBallot> =
        when {
            (decryptSpoiledList == null) -> electionRecordIn.iterateSpoiledBallots()
            (decryptSpoiledList.trim().lowercase() == "all") -> electionRecordIn.iterateEncryptedBallots { true }
            fileExists(decryptSpoiledList) -> {
                val wanted: List<String> = fileReadLines(decryptSpoiledList)
                electionRecordIn.iterateEncryptedBallots { wanted.contains(it.ballotId) }
            }
            else -> {
                val wanted: List<String> = decryptSpoiledList.split(",")
                electionRecordIn.iterateEncryptedBallots { wanted.contains(it.ballotId) }
            }
        }

    runBlocking {
        val outputChannel = Channel<PlaintextTally>()
        val decryptorJobs = mutableListOf<Job>()
        val ballotProducer = produceBallots(ballotIter)
        repeat(nthreads) {
            decryptorJobs.add(
                launchDecryptor(
                    it,
                    ballotProducer,
                    decryptor,
                    outputChannel
                )
            )
        }
        launchSink(outputChannel, sink)

        // wait for all decryptions to be done, then close everything
        joinAll(*decryptorJobs.toTypedArray())
        outputChannel.close()
    }
    sink.close()

    val took = getSystemTimeInMillis() - starting
    val msecsPerBallot = (took.toDouble() / 1000 / count)
    println("Decrypt ballots with nthreads = $nthreads took ${took / 1000} secs for $count ballots = $msecsPerBallot secs/ballot")
}

// place the ballot reading into its own coroutine
@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineScope.produceBallots(producer: Iterable<EncryptedBallot>): ReceiveChannel<EncryptedBallot> =
    produce {
        for (ballot in producer) {
            logger.debug { "Producer sending ballot ${ballot.ballotId}" }
            send(ballot)
            yield()
        }
        channel.close()
    }

private fun CoroutineScope.launchDecryptor(
    id: Int,
    input: ReceiveChannel<EncryptedBallot>,
    decryptor: DecryptingMediator,
    output: SendChannel<PlaintextTally>,
) = launch(Dispatchers.Default) {
    for (ballot in input) {
         val decrypted: PlaintextTally = decryptor.decryptBallot(ballot)
        logger.debug { " Encryptor #$id sending ciphertext ballot ${decrypted.tallyId}" }
        println(" Encryptor #$id sending ciphertext ballot ${decrypted.tallyId}")
        output.send(decrypted)
        yield()
    }
    logger.debug { "Encryptor #$id done" }
}

// place the output writing into its own coroutine
private var count = 0
private fun CoroutineScope.launchSink(
    input: Channel<PlaintextTally>, sink: PlaintextTallySinkIF,
) = launch {
    for (tally in input) {
        sink.writePlaintextTally(tally)
        logger.debug { " Sink wrote $count ballot ${tally.tallyId}" }
        count++
    }
}
