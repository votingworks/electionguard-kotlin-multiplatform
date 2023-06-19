@file:OptIn(ExperimentalCoroutinesApi::class)

package electionguard.encrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.Stats
import electionguard.core.UInt256
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.core.toElementModQ
import electionguard.decryptBallot.DecryptWithNonce
import electionguard.input.BallotInputValidation
import electionguard.input.ManifestInputValidation
import electionguard.publish.EncryptedBallotSinkIF
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.verifier.VerifyEncryptedBallots
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
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
 * Run ballot encryption in batch mode CLI.
 * Read ElectionConfig from inputDir, write electionInit to outputDir.
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
    val chainCodes by parser.option(
        ArgType.Boolean,
        shortName = "chainCodes",
        description = "Chain confirmationCodes"
    ).default( false)

    parser.parse(args)

    println("RunBatchEncryption starting\n   input= $inputDir\n   ballots = $ballotDir\n   output = $outputDir" +
            "\n   nthreads = $nthreads" +
            "\n   fixedNonces = $fixedNonces" +
            "\n   check = $check" +
            "\n   chainCodes = $chainCodes"
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
        chainCodes,
    )
}

enum class CheckType { None, Verify, EncryptTwice, DecryptNonce }

// encrypt ballots in inputDir
fun batchEncryption(
    group: GroupContext, inputDir: String, outputDir: String, ballotDir: String,
    invalidDir: String?, fixedNonces: Boolean, nthreads: Int, createdBy: String?, check: CheckType = CheckType.None,
    chainCodes : Boolean = false
) {
    val consumerIn = makeConsumer(inputDir, group)
    return batchEncryption(group, inputDir, outputDir, consumerIn.iteratePlaintextBallots(ballotDir, null),
        invalidDir, fixedNonces, nthreads, createdBy, check, chainCodes)
}

// encrypt the ballots in Iterable<PlaintextBallot>
fun batchEncryption(
    group: GroupContext,
    inputDir: String,
    outputDir: String,
    ballots: Iterable<PlaintextBallot>,
    invalidDir: String?,
    fixedNonces: Boolean,
    nthreads: Int,
    createdBy: String?,
    check: CheckType = CheckType.None,
    chainCodes : Boolean = false,
) {
    val consumerIn = makeConsumer(inputDir, group)
    val electionInit: ElectionInitialized =
        consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }

    // ManifestInputValidation
    val manifestValidator = ManifestInputValidation(electionInit.manifest())
    val errors = manifestValidator.validate()
    if (errors.hasErrors()) {
        println("*** ManifestInputValidation FAILED on election record in $inputDir")
        println("$errors")
        // kotlin.system.exitProcess(1) // kotlin 1.6.20
        return
    }
    // debugging
    // Map<BallotStyle: String, selectionCount: Int>
    val styleCount = manifestValidator.countEncryptions()

    // BallotInputValidation
    var countEncryptions = 0
    val invalidBallots = ArrayList<PlaintextBallot>()
    val ballotValidator = BallotInputValidation(electionInit.manifest())
    val validate: ((PlaintextBallot) -> Boolean) = {
        val mess = ballotValidator.validate(it)
        if (mess.hasErrors()) {
            println("*** BallotInputValidation FAILED on ballot ${it.ballotId}")
            println("$mess\n")
            invalidBallots.add(PlaintextBallot(it, mess.toString()))
            false
        } else {
            countEncryptions += styleCount[it.ballotStyle] ?: 0
            true
        }
    }
    val starting = getSystemTimeInMillis() // start timing here

    val useThreads = if (chainCodes) 1 else nthreads

    val ballotNonce = if (fixedNonces) UInt256.TWO else null
    val encryptor = Encryptor(
        group,
        electionInit.manifest(),
        ElGamalPublicKey(electionInit.jointPublicKey),
        electionInit.extendedBaseHash
    )
    val runEncryption = EncryptionRunner(group, encryptor, ballotNonce, electionInit.manifest(),
        electionInit.jointPublicKey, electionInit.extendedBaseHash, check, chainCodes)

    val publisher = makePublisher(outputDir)
    val sink: EncryptedBallotSinkIF = publisher.encryptedBallotSink()

    try {
        runBlocking {
            val outputChannel = Channel<EncryptedBallot>()
            val encryptorJobs = mutableListOf<Job>()
            val ballotProducer = produceBallots(ballots.filter { validate(it) })
            repeat(useThreads) {
                encryptorJobs.add(
                    launchEncryptor(
                        it,
                        ballotProducer,
                        outputChannel
                    ) { ballot -> runEncryption.encrypt(ballot) }
                )
            }
            launchSink(outputChannel, sink)

            // wait for all encryptions to be done, then close everything
            joinAll(*encryptorJobs.toTypedArray())
            outputChannel.close()
        }
    } finally {
        sink.close()
    }
    val took = getSystemTimeInMillis() - starting

    publisher.writeElectionInitialized(
        electionInit.addMetadataToCopy(
            Pair("Used", createdBy ?: "RunBatchEncryption"),
            Pair("UsedOn", getSystemDate().toString()),
            Pair("CreatedFromDir", inputDir)
        )
    )
    if (invalidDir != null && !invalidBallots.isEmpty()) {
        publisher.writePlaintextBallot(invalidDir, invalidBallots)
        println(" wrote ${invalidBallots.size} invalid ballots to $invalidDir")
    }

    val msecsPerBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
    println("Encryption with nthreads = $nthreads took $took millisecs for $count ballots = $msecsPerBallot msecs/ballot")
    val msecPerEncryption = (took.toDouble() / countEncryptions)
    val encryptionPerBallot = if (count == 0) 0 else (countEncryptions / count)
    println("    $countEncryptions total encryptions = $encryptionPerBallot per ballot = $msecPerEncryption millisecs/encryption")
}

private var codeBaux = ByteArray(0)

// orchestrates the encryption
private class EncryptionRunner(
    val group: GroupContext,
    val encryptor: Encryptor,
    val ballotNonce: UInt256?,
    val manifest: ManifestIF,
    val jointPublicKey: ElementModP,
    val extendedBaseHash: UInt256,
    val check: CheckType,
    val chainCodes : Boolean,
) {
    val publicKeyEG = ElGamalPublicKey(jointPublicKey)
    val extendedBaseHashQ = extendedBaseHash.toElementModQ(group)

    val verifier: VerifyEncryptedBallots?
    init {
        verifier = if (check == CheckType.Verify) VerifyEncryptedBallots(group, manifest,
            publicKeyEG, extendedBaseHashQ, 1)
        else null
    }

    fun encrypt(ballot: PlaintextBallot): EncryptedBallot {
        val ciphertextBallot = if (chainCodes) {
            encryptor.encrypt(ballot, ballotNonce, null, codeBaux)
        } else {
            encryptor.encrypt(ballot, ballotNonce)
        }

        // experiments in testing the encryption
        if (check == CheckType.EncryptTwice && ballotNonce != null) {
            val encrypted2 = encryptor.encrypt(ballot, ballotNonce)
            if (encrypted2.confirmationCode != ciphertextBallot.confirmationCode) {
                logger.warn { "encrypted.confirmationCode doesnt match" }
            }
            if (encrypted2 != ciphertextBallot) {
                logger.warn { "encrypted doesnt match" }
            }
        } else if (check == CheckType.Verify && verifier != null) {
            // VerifyEncryptedBallots may be doing more work than actually needed
            val submitted = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)
            val verifyResults = verifier.verifyEncryptedBallot(submitted, Stats())
            if (verifyResults is Err) {
                logger.warn { "encrypted doesnt verify = ${verifyResults}" }
            }
        } else if (check == CheckType.DecryptNonce) {
            // Decrypt with Nonce to ensure encryption worked
            val primaryNonce = ciphertextBallot.ballotNonce
            val encryptedBallot = ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)

            val decryptionWithPrimaryNonce = DecryptWithNonce(group, manifest, publicKeyEG, extendedBaseHash)
            val decryptResult = with (decryptionWithPrimaryNonce) { encryptedBallot.decrypt(primaryNonce) }
            if (decryptResult is Err) {
                logger.warn { "encrypted ballot fails decryption = $decryptResult" }
            }
        }

        codeBaux = ciphertextBallot.confirmationCode.bytes
        return ciphertextBallot.submit(EncryptedBallot.BallotState.CAST)
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