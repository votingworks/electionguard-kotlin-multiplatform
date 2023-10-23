package electionguard.cli

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap

import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.Guardians
import electionguard.pep.BallotPep
import electionguard.pep.PepAlgorithm
import electionguard.pep.PepBlindTrust
import electionguard.pep.PepTrustee
import electionguard.publish.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("RunTrustedPep")

/**
 * Compare encrypted ballots with local trustees CLI. Multithreaded: each ballot gets its own coroutine.
 *
 * Read election record from inputDir, which is assumed to have an electionInitialized file in the inputDir directory,
 * and encrypted ballots in inputDir/encrypted_ballots. This is the "standard election record layout".
 *
 * All encrypted ballots in subdirectories of inputDir/encrypted_ballots are read, and the corresponding ballot
 * (matched by ballot_id) is looked for in the scannedBallotDir. If not found, the ballot is skipped.
 * The subdirectories correspond to the "device".
 *
 * The scannedBallotDir directly contains the "scanned" encrypted ballots matching the ones in the election record.
 * TODO: perhaps should look under subdirectories?
 *
 * This has access to all the trustees for decrypting and blinding. So it is used when the guardians trust each other.
 * The decrypting trustees could be isolated into seperate webapps, although this class does not yet have that option.
 */
class RunTrustedPep {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunTrustedPep")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Top directory of the input election record"
            ).required()
            val scannedBallotDir by parser.option(
                ArgType.String,
                shortName = "scanned",
                description = "Directory containing scanned ballots"
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
            val missing by parser.option(
                ArgType.String,
                shortName = "missing",
                description = "missing guardians' xcoord, comma separated, eg '2,4'"
            )
            val nthreads by parser.option(
                ArgType.Int,
                shortName = "nthreads",
                description = "Number of parallel threads to use"
            ).default(11)

            parser.parse(args)
            println(
                "RunTrustedPep starting\n   -in $inputDir\n   -scanned $scannedBallotDir\n   -trustees $trusteeDir\n" +
                        "   -out $outputDir\n   -nthreads $nthreads"
            )
            if (missing != null) {
                println("   -missing $missing")
            }

            val group = productionGroup()
            val decryptingTrustees = RunTrustedTallyDecryption.readDecryptingTrustees(group, inputDir, trusteeDir, missing)

            batchTrustedPep(group, inputDir, scannedBallotDir, outputDir, decryptingTrustees, nthreads)
        }

        fun batchTrustedPep(
            group: GroupContext,
            inputDir: String,
            scannedBallotDir: String,
            outputDir: String,
            decryptingTrustees: List<DecryptingTrusteeIF>,
            nthreads: Int,
        ) {
            println(" PepBlindTrust compare ballots in '${inputDir}' to ballots in '$scannedBallotDir'")
            val starting = getSystemTimeInMillis() // wall clock

            val consumer = makeConsumer(group, inputDir)
            val electionRecord = readElectionRecord(consumer)
            val electionInitialized = electionRecord.electionInit()!!

            val blindingTrustees = mutableListOf<PepTrustee>()
            repeat(3) {
                blindingTrustees.add(PepTrustee(it, group))
            }
            val guardians = Guardians(group, electionInitialized.guardians)

            val pep = PepBlindTrust(
                group,
                electionInitialized.extendedBaseHash,
                ElGamalPublicKey(electionInitialized.jointPublicKey),
                guardians, // all guardians
                blindingTrustees,
                decryptingTrustees,
            )

            val publisher = makePublisher(outputDir, createNew = false, jsonSerialization = true) // always json for now
            val sink: DecryptedTallyOrBallotSinkIF = publisher.decryptedTallyOrBallotSink()

            try {
                runBlocking {
                    val outputChannel = Channel<BallotPep>()
                    val pepJobs = mutableListOf<Job>()
                    val ballotProducer = produceBallots(consumer, scannedBallotDir)
                    repeat(nthreads) {
                        pepJobs.add(
                            launchPepWorker(
                                it,
                                ballotProducer,
                                pep,
                                outputChannel
                            )
                        )
                    }
                    launchSink(outputChannel, publisher.pepBallotSink(outputDir))

                    // wait for all decryptions to be done, then close everything
                    joinAll(*pepJobs.toTypedArray())
                    outputChannel.close()
                }
            } finally {
                sink.close()
            }

            pep.stats.show(5)
            val count = pep.stats.count()

            val took = getSystemTimeInMillis() - starting
            val msecsPerBallot = (took.toDouble() / 1000 / count).sigfig()
            println("PepBlindTrust took ${took / 1000} wallclock secs for $count ballots = $msecsPerBallot secs/ballot with $nthreads threads")
        }

        // parallelize over ballots
        // place the ballot reading into its own coroutine
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun CoroutineScope.produceBallots(consumer: Consumer, scannedBallotDir: String): ReceiveChannel<Pair<EncryptedBallot, EncryptedBallot>> =
            produce {
                for (ballot in consumer.iterateAllCastBallots()) {
                    val readResult = consumer.readEncryptedBallot(scannedBallotDir, ballot.ballotId )
                    if (readResult is Err) {
                        logger.warn { "SKIPPING ballot ${ballot.ballotId} because $readResult" }
                    } else {
                        send(Pair(ballot, readResult.unwrap()))
                        yield()
                    }
                }
                channel.close()
            }

        private fun CoroutineScope.launchPepWorker(
            id: Int,
            input: ReceiveChannel<Pair<EncryptedBallot, EncryptedBallot>>,
            pep: PepAlgorithm,
            output: SendChannel<BallotPep>,
        ) = launch(Dispatchers.Default) {

            for (ballotPair in input) {
                val result: Result<BallotPep, String> = pep.doEgkPep(ballotPair.first, ballotPair.second)
                if (result is Err) {
                    logger.warn { " PEP error on ballot ${ballotPair.first.ballotId} because $result" }
                } else {
                    val pepBallot = result.unwrap()
                    logger.info { " PEP compared ${pepBallot.ballotId} equal=${pepBallot.isEq}" }
                    output.send(pepBallot)
                    yield()
                }
            }
            logger.debug { "Decryptor #$id done" }
        }

        // place the output writing into its own coroutine
        private fun CoroutineScope.launchSink(
            input: Channel<BallotPep>, sink: PepBallotSinkIF,
        ) = launch {
            for (result in input) {
                sink.writePepBallot(result)
            }
        }
    }
}