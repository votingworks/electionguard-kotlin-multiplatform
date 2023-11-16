package electionguard.cli

import electionguard.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

import electionguard.rave.*
import electionguard.publish.*
import electionguard.util.ErrorMessages
import electionguard.util.sigfig
import kotlinx.cli.default
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger("RunVerifyPep")

/** Run election record verification CLI. */
class RunVerifyPep {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunVerifyPep")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input election record"
            ).required()
            val pepBallotDir by parser.option(
                ArgType.String,
                shortName = "pep",
                description = "Directory containing PEP output"
            ).required()
            val nthreads by parser.option(
                ArgType.Int,
                shortName = "nthreads",
                description = "Number of parallel threads to use"
            ).default(11)
            parser.parse(args)
            println("RunVerifyPep starting\n   input= $inputDir")

            // runVerifyPep(productionGroup(), inputDir, pepBallotDir, nthreads ?: 11)
            batchVerifyPep(productionGroup(), inputDir, pepBallotDir, nthreads)
        }

        fun batchVerifyPep(
            group: GroupContext,
            inputDir: String,
            pepBallotDir: String,
            nthreads: Int,
        ) {
            println(" Pep verify ballots in '${pepBallotDir}'")
            val starting = getSystemTimeInMillis() // wall clock

            val consumer = makeConsumer(group, inputDir, true)
            val record = readElectionRecord(consumer)

            val verifier = VerifierPep(group, record.extendedBaseHash()!!, ElGamalPublicKey(record.jointPublicKey()!!))

            val raveIO = RaveIO(pepBallotDir, group)
            runBlocking {
                val pepJobs = mutableListOf<Job>()
                val ballotProducer = produceBallots(raveIO)
                repeat(nthreads) {
                    pepJobs.add(
                        launchPepWorker(
                            it,
                            ballotProducer,
                            verifier,
                        )
                    )
                }

                // wait for all jobs to be done, then close everything
                joinAll(*pepJobs.toTypedArray())
            }

            val nverify = count.get()
            val took = getSystemTimeInMillis() - starting
            val msecsPerBallot = (took.toDouble() / 1000 / nverify).sigfig()
            println("PepVerify took ${took / 1000} wallclock secs for $nverify ballots = $msecsPerBallot secs/ballot with $nthreads threads")
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun CoroutineScope.produceBallots(raveIO: RaveIO): ReceiveChannel<BallotPep> =
            produce {
                for (ballot in raveIO.iteratePepBallots()) {
                    send(ballot)
                    yield()
                }
                channel.close()
            }

        val count = AtomicInteger(0)
        private fun CoroutineScope.launchPepWorker(
            id: Int,
            input: ReceiveChannel<BallotPep>,
            verifier: VerifierPep,
        ) = launch(Dispatchers.Default) {

            for (ballotPEP in input) {
                val errs = ErrorMessages("Ballot ${ballotPEP.ballotId}")
                val result = verifier.verify(ballotPEP, errs)
                if (errs.hasErrors()) {
                    println(" PEP error verifying ballot ${ballotPEP.ballotId} because $errs")
                    logger.warn { " PEP error verifying ballot ${ballotPEP.ballotId} because $errs" }
                } else {
                    println(" PEP verify ${ballotPEP.ballotId} is ${result}")
                    logger.info { " PEP verify ${ballotPEP.ballotId} is ${result}" }
                    count.getAndIncrement()
                }
                yield()
            }
            logger.debug { "Decryptor #$id done" }
        }
    }
}