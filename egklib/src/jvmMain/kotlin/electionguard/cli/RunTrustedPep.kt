package electionguard.cli

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
import kotlinx.cli.required
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import mu.KotlinLogging

private val logger = KotlinLogging.logger("RunTrustedPep")
private val debug = false

/**
 * Compare encrypted ballots with local trustees CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 * A version of this where each Trustee is in its own process space is implemented in the webapps modules.
 */
class RunTrustedPep {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunTrustedPep")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
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
            )
            parser.parse(args)
            println(
                "RunTrustedPep starting\n   input= $inputDir\n   scannedBallotDir= $scannedBallotDir\n   trustees= $trusteeDir\n" +
                        "   output = $outputDir\n   nthreads = $nthreads\n"
            )

            val group = productionGroup()
            runTrustedPep(
                group, inputDir, scannedBallotDir, outputDir,
                RunTrustedTallyDecryption.readDecryptingTrustees(group, inputDir, trusteeDir, missing),
                nthreads ?: 11
            )
        }

        fun runTrustedPep(
            group: GroupContext,
            inputDir: String,
            scannedBallotDir: String,
            outputDir: String,
            decryptingTrustees: List<DecryptingTrusteeIF>,
            nthreads: Int,
        ): Int {
            println(" runTrustedPep on ballots in ${inputDir} compare to Ballots in $scannedBallotDir")
            val starting = getSystemTimeInMillis() // wall clock

            val consumer = makeConsumer(group, inputDir)
            val electionRecord = readElectionRecord(consumer)
            val electionInitialized = electionRecord.electionInit()!!
            val guardians = Guardians(group, electionInitialized.guardians)

            val btrustees = mutableListOf<PepTrustee>()
            repeat(3) {
                btrustees.add(PepTrustee(it, group))
            }
            val pep = PepBlindTrust(
                group,
                electionInitialized.extendedBaseHash,
                ElGamalPublicKey(electionInitialized.jointPublicKey),
                guardians, // all guardians
                btrustees,
                decryptingTrustees,
            )

            val publisher = makePublisher(outputDir, false, true) // always json for now
            val sink: DecryptedTallyOrBallotSinkIF = publisher.decryptedTallyOrBallotSink()

            try {
                runBlocking {
                    val outputChannel = Channel<Result<BallotPep, String>>()
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
            println(" Pep compare ballots took ${took / 1000} wallclock secs for $count ballots = $msecsPerBallot secs/ballot")

            return count
        }

        // parallelize over ballots
        // place the ballot reading into its own coroutine
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun CoroutineScope.produceBallots(consumer: Consumer, scannedBallotDir: String): ReceiveChannel<Pair<EncryptedBallot, EncryptedBallot>> =
            produce {
                for (ballot in consumer.iterateAllCastBallots()) {
                    logger.debug { "Producer sending ballot ${ballot.ballotId}" }
                    val scannedBallot = consumer.readEncryptedBallot(scannedBallotDir,ballot.ballotId )
                    send(Pair(ballot, scannedBallot.unwrap()))
                    yield()
                }
                channel.close()
            }

        private fun CoroutineScope.launchPepWorker(
            id: Int,
            input: ReceiveChannel<Pair<EncryptedBallot, EncryptedBallot>>,
            pep: PepAlgorithm,
            output: SendChannel<Result<BallotPep, String>>,
        ) = launch(Dispatchers.Default) {
            for (ballotPair in input) {
                val decrypted: Result<BallotPep, String> = pep.doEgkPep(ballotPair.first, ballotPair.second)
                logger.debug { " coroutine #$id pep compare ${ballotPair.first.ballotId}" }
                if (debug) println(" coroutine #$id pep compare ${ballotPair.first.ballotId}")
                output.send(decrypted)
                yield()
            }
            logger.debug { "Decryptor #$id done" }
        }

        // place the output writing into its own coroutine
        private fun CoroutineScope.launchSink(
            input: Channel<Result<BallotPep, String>>, sink: PepBallotSinkIF,
        ) = launch {
            for (result in input) {
                sink.writePepBallot(result.unwrap())
            }
        }
    }
}