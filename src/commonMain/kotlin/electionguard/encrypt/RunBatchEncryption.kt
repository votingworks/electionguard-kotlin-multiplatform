@file:OptIn(ExperimentalCli::class, ExperimentalCoroutinesApi::class)

package electionguard.encrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.Manifest
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import electionguard.publish.EncryptedBallotSinkIF
import electionguard.verifier.VerifyEncryptedBallots
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.default
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
import kotlin.math.roundToInt
import mu.KotlinLogging

private val logger = KotlinLogging.logger("RunBatchEncryption")

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
        description = "Directory containing input ElectionInitialized.protobuf file"
    ).required()
    val ballotDir by parser.option(
        ArgType.String,
        shortName = "ballots",
        description = "Directory to read Plaintext ballots from"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    val invalidDir by parser.option(
        ArgType.String,
        shortName = "invalid",
        description = "Directory to write invalid input ballots to"
    )
    val fixedNonces by parser.option(
        ArgType.Boolean,
        shortName = "fixed",
        description = "Encrypt with fixed nonces and timestamp"
    ).default( false)
    val check by parser.option(
        ArgType.Choice<CheckType>(),
        shortName = "check",
        description = "Check encryption"
    ).default( CheckType.None)
    val nthreads by parser.option(
        ArgType.Int,
        shortName = "nthreads",
        description = "Number of parallel threads to use"
    ).default( 11)
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created"
    )
    parser.parse(args)

    println("RunBatchEncryption starting\n   input= $inputDir\n   ballots = $ballotDir\n   output = $outputDir" +
            "\n   nthreads = $nthreads" +
            "\n   fixedNonces = $fixedNonces" +
            "\n   check = $check"
    )

    batchEncryption(
        productionGroup(),
        inputDir,
        outputDir,
        ballotDir,
        invalidDir,
        fixedNonces,
        nthreads,
        createdBy,
        check,
    )
}

enum class CheckType { None, Verify, EncryptTwice }

fun batchEncryption(
    group: GroupContext, inputDir: String, outputDir: String, ballotDir: String,
    invalidDir: String?, fixedNonces: Boolean, nthreads: Int, createdBy: String?, check: CheckType = CheckType.None
) {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val electionInit: ElectionInitialized =
        electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }

    // ManifestInputValidation
    val manifestValidator = ManifestInputValidation(electionInit.manifest())
    val errors = manifestValidator.validate()
    if (errors.hasErrors()) {
        println("*** ManifestInputValidation FAILED on election record in $inputDir")
        println("$errors")
        // kotlin.system.exitProcess(1) // kotlin 1.6.20
        return
    }
    // Map<BallotStyle: String, selectionCount: Int>
    val styleCount = manifestValidator.countEncryptions()

    // BallotInputValidation
    var countEncryptions = 0
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
            countEncryptions += styleCount[it.ballotStyleId] ?: 0
            true
        }
    }

    val codeSeed: ElementModQ = electionInit.cryptoExtendedBaseHash.toElementModQ(group)
    val masterNonce = if (fixedNonces) group.TWO_MOD_Q else null
    val starting = getSystemTimeInMillis()
    val encryptor = Encryptor(
        group,
        electionInit.manifest(),
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.cryptoExtendedBaseHash
    )
    val encrypt = RunEncryption(group, encryptor, codeSeed, masterNonce, electionInit.manifest(),
        electionInit.jointPublicKey, electionInit.cryptoExtendedBaseHash, check)

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink()

    runBlocking {
        val outputChannel = Channel<EncryptedBallot>()
        val encryptorJobs = mutableListOf<Job>()
        val ballotProducer = produceBallots(electionRecordIn.iteratePlaintextBallots(ballotDir, filter))
        repeat(nthreads) {
            encryptorJobs.add(
                launchEncryptor(
                    it,
                    ballotProducer,
                    outputChannel
                ) { ballot -> encrypt.encrypt(ballot) }
            )
        }
        launchSink(outputChannel, sink)

        // wait for all encryptions to be done, then close everything
        joinAll(*encryptorJobs.toTypedArray())
        outputChannel.close()
    }
    sink.close()

    // LOOK i dont know if this is a good idea
    publisher.writeElectionInitialized(
        electionInit.addMetadata(
            Pair("Used", createdBy ?: "RunBatchEncryption"),
            Pair("UsedOn", getSystemDate().toString()),
            Pair("CreatedFromDir", inputDir)
        )
    )
    if (invalidDir != null && !invalidBallots.isEmpty()) {
        publisher.writePlaintextBallot(invalidDir, invalidBallots)
        println(" wrote ${invalidBallots.size} invalid ballots to $invalidDir")
    }

    val took = getSystemTimeInMillis() - starting
    val msecsPerBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
    println("Encryption with nthreads = $nthreads took $took millisecs for $count ballots = $msecsPerBallot msecs/ballot")
    val msecPerEncryption = (took.toDouble() / countEncryptions)
    val encryptionPerBallot = (countEncryptions / count)
    println("    $countEncryptions total encryptions = $encryptionPerBallot per ballot = $msecPerEncryption millisecs/encryption")
}

// orchestrates the encryption
private class RunEncryption(
    val group: GroupContext, val encryptor: Encryptor, val codeSeed: ElementModQ, val masterNonce: ElementModQ?,
    manifest: Manifest,
    jointPublicKey: ElementModP,
    cryptoExtendedBaseHash: UInt256,
    val check: CheckType
) {
    val verifier: VerifyEncryptedBallots?
    init {
        verifier = if (check == CheckType.Verify) VerifyEncryptedBallots(group, manifest,
            ElGamalPublicKey(jointPublicKey), cryptoExtendedBaseHash.toElementModQ(group), 1) else null
    }
    fun encrypt(ballot: PlaintextBallot): EncryptedBallot {
        val encrypted = if (masterNonce != null) // make deterministic
            encryptor.encrypt(ballot, codeSeed, masterNonce, 0)
        else
            encryptor.encrypt(ballot, codeSeed, group.randomElementModQ())
        if (check == CheckType.EncryptTwice) {
            val encrypted2 = encryptor.encrypt(ballot, codeSeed, encrypted.masterNonce, encrypted.timestamp)
            if (encrypted2.cryptoHash != encrypted.cryptoHash) {
                logger.warn { "encrypted.cryptoHash doesnt match" }
            }
            if (encrypted2 != encrypted) {
                logger.warn { "encrypted doesnt match" }
            }
        } else if (check == CheckType.Verify && verifier != null) {
            // LOOK its possible VerifyEncryptedBallots is doing more work than needed here
            val submitted = encrypted.submit(EncryptedBallot.BallotState.CAST)
            val verifyStats = verifier.verifyEncryptedBallot(submitted)
            if (!verifyStats.allOk) {
                logger.warn { "encrypted doesnt verify = ${verifyStats.errors}" }
            }
        }
        return encrypted.submit(EncryptedBallot.BallotState.CAST)
    }
}

// the ballot reading is in its own coroutine
private fun CoroutineScope.produceBallots(producer: Iterable<PlaintextBallot>): ReceiveChannel<PlaintextBallot> =
    produce {
        for (ballot in producer) {
            logger.debug { "Producer sending PlaintextBallot ${ballot.ballotId}" }
            send(ballot)
            yield()
        }
        channel.close()
    }

// coroutines allow parallel encryption at the ballot level
// LOOK not possible to do ballot chaining, since the order is indeterminate?
//    or do we just have to work harder??
private fun CoroutineScope.launchEncryptor(
    id: Int,
    input: ReceiveChannel<PlaintextBallot>,
    output: SendChannel<EncryptedBallot>,
    encrypt: (PlaintextBallot) -> EncryptedBallot,
) = launch(Dispatchers.Default) {
    for (ballot in input) {
        val encrypted = encrypt(ballot)
        logger.debug { " Encryptor #$id sending CiphertextBallot ${encrypted.ballotId}" }
        output.send(encrypted)
        yield()
    }
    logger.debug { "Encryptor #$id done" }
}

// the encrypted ballot writing is in its own coroutine
private var count = 0
private fun CoroutineScope.launchSink(
    input: Channel<EncryptedBallot>, sink: EncryptedBallotSinkIF,
) = launch {
    for (ballot in input) {
        sink.writeEncryptedBallot(ballot)
        logger.debug { " Sink wrote $count submitted ballot ${ballot.ballotId}" }
        count++
    }
}