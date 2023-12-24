package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedBallotChain
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.publish.ElectionRecord
import electionguard.util.ErrorMessages
import electionguard.util.Stats
import electionguard.util.sigfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

private const val debugBallots = false

/** Can be multithreaded. */
@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEncryptedBallots(
    val group: GroupContext,
    val manifest: ManifestIF,
    val jointPublicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256, // He
    val config: ElectionConfig,
    private val nthreads: Int,
) {
    val aggregator = SelectionAggregator() // for Verification 8 (Correctness of ballot aggregation)

    fun verifyBallots(
        ballots: Iterable<EncryptedBallot>,
        errs: ErrorMessages,
        stats: Stats = Stats(),
        showTime: Boolean = false
    ): Boolean {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceBallots(ballots)
            repeat(nthreads) {
                verifierJobs.add(
                    launchVerifier(
                        it,
                        ballotProducer,
                        aggregator
                    ) { ballot -> verifyEncryptedBallot(ballot, errs.nested("ballot ${ballot.ballotId}"), stats) })
            }

            // wait for all verifications to be done
            joinAll(*verifierJobs.toTypedArray())
        }

        // check duplicate confirmation codes (7.C): LOOK what if there are multiple records for the election?
        // LOOK what about checking for duplicate ballot ids?
        val checkDuplicates = mutableMapOf<UInt256, String>()
        confirmationCodes.forEach {
            if (checkDuplicates[it.code] != null) {
                errs.add("    7.C, 17.D. Duplicate confirmation code ${it.code} for ballot ids=${it.ballotId},${checkDuplicates[it.code]}")
            }
            checkDuplicates[it.code] = it.ballotId
        }

        if (showTime) {
            val took = getSystemTimeInMillis() - starting
            val perBallot = if (count == 0) 0 else (took.toDouble() / count).sigfig()
            println("   VerifyEncryptedBallots with $nthreads threads took $took millisecs wallclock for $count ballots = $perBallot msecs/ballot")
        }
        return !errs.hasErrors()
    }

    fun verifyEncryptedBallot(
        ballot: EncryptedBallot,
        errs: ErrorMessages,
        stats: Stats
    ) : Boolean {
        val starting = getSystemTimeInMillis()

        if (ballot.electionId != extendedBaseHash) {
            errs.add("Encrypted Ballot ${ballot.ballotId} has wrong electionId = ${ballot.electionId}; skipping")
            return false
        }

        var ncontests = 0
        var nselections = 0
        for (contest in ballot.contests) {
            ncontests++
            nselections += contest.selections.size
            verifyEncryptedContest(contest, ballot.isPreencrypt, errs.nested("Selection ${contest.contestId}"))
        }

        if (!ballot.isPreencrypt) {
            // The ballot confirmation code H(B) = H(HE ; 0x24, χ1 , χ2 , . . . , χmB , Baux ) ; 7.B
            val contestHashes = ballot.contests.map { it.contestHash }
            val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), contestHashes, ballot.codeBaux)
            if (confirmationCode != ballot.confirmationCode) {
                errs.add("    7.B. Incorrect ballot confirmation code for ballot ${ballot.ballotId} ")
            }
        } else {
            verifyPreencryptedCode(ballot, errs)
        }

        stats.of("verifyEncryptions", "selection").accum(getSystemTimeInMillis() - starting, nselections)
        if (debugBallots) println(" Ballot '${ballot.ballotId}' ncontests = $ncontests nselections = $nselections")

        return !errs.hasErrors()
    }

    fun verifyEncryptedContest(
        contest: EncryptedBallot.Contest,
        isPreencrypt: Boolean,
        errs: ErrorMessages
    ) {
        contest.selections.forEach {
            verifySelection( it, manifest.optionLimit(contest.contestId), errs.nested("Selection ${it.selectionId}"))
        }

        // Verification 6 (Adherence to vote limits)
        val texts: List<ElGamalCiphertext> = contest.selections.map { it.encryptedVote }
        val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()?: 0.encrypt(jointPublicKey)
        val cvalid = contest.proof.verify(
            ciphertextAccumulation,
            this.jointPublicKey,
            this.extendedBaseHash,
            manifest.contestLimit(contest.contestId)
        )
        if (cvalid is Err) {
            errs.add("    6. ChaumPedersenProof validation error = ${cvalid.error} ")
        }

        // χl = H(HE ; 0x23, l, K, α1 , β1 , α2 , β2 . . . , αm , βm ) 7.A
        val ciphers = mutableListOf<ElementModP>()
        texts.forEach {
            ciphers.add(it.pad)
            ciphers.add(it.data)
        }
        val contestHash =
            hashFunction(extendedBaseHash.bytes, 0x23.toByte(), contest.sequenceOrder, jointPublicKey, ciphers)
        if (contestHash != contest.contestHash) {
            errs.add("    7.A. Incorrect contest hash")
        }

        if (isPreencrypt) {
            verifyPreencryptionShortCodes(contest, errs)
        }
    }

    // Verification 5 (Well-formedness of selection encryptions)
    private fun verifySelection(
        selection: EncryptedBallot.Selection,
        optionLimit: Int,
        errs: ErrorMessages
    ) {
        val svalid = selection.proof.verify(
            selection.encryptedVote,
            this.jointPublicKey,
            this.extendedBaseHash,
            optionLimit,
        )
        if (svalid is Err) {
            errs.add("    5. ChaumPedersenProof validation error = ${svalid.error} ")
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // ballot chaining, section 7

    fun verifyConfirmationChain(consumer: ElectionRecord, errs: ErrorMessages): Boolean {

        consumer.encryptingDevices().forEach { device ->
            // println("verifyConfirmationChain device=$device")
            val ballotChainResult = consumer.readEncryptedBallotChain(device)
            if (ballotChainResult is Err) {
                errs.add(ballotChainResult.toString())
            } else {
                val ballotChain: EncryptedBallotChain = ballotChainResult.unwrap()
                val ballots = consumer.encryptedBallots(device) { true }

                // 7.D The initial hash code H0 satisfies H0 = H(HE ; 0x24, Baux,0 )
                // "and Baux,0 contains the unique voting device information". TODO ambiguous, change spec wording
                val H0 = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), config.configBaux0).bytes

                // (7.E) For all 1 ≤ j ≤ ℓ, the additional input byte array used to compute Hj = H(Bj) is equal to
                //       Baux,j = H(Bj−1) ∥ Baux,0 .
                var prevCC = H0
                var first = true
                ballots.forEach { ballot ->
                    val expectedBaux = if (first) H0 else prevCC + config.configBaux0  // eq 7.D and 7.E
                    first = false
                    if (!expectedBaux.contentEquals(ballot.codeBaux)) {
                        errs.add("    7.E. additional input byte array Baux != H(Bj−1 ) ∥ Baux,0 for ballot=${ballot.ballotId}")
                    }
                    prevCC = ballot.confirmationCode.bytes
                }
                // 7.F The final additional input byte array is equal to Baux = H(Bℓ ) ∥ Baux,0 ∥ b(“CLOSE”, 5) and
                //      H(Bℓ ) is the final confirmation code on this device.
                val bauxFinal = prevCC + config.configBaux0 + "CLOSE".encodeToByteArray()

                // 7.G The closing hash is correctly computed as H = H(HE ; 0x24, Baux )
                val expectedClosingHash = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
                if (expectedClosingHash != ballotChain.closingHash) {
                    errs.add("    7.G. The closing hash is not equal to H = H(HE ; 24, bauxFinal ) for encrypting device=$device")
                }
            }
        }
        return !errs.hasErrors()
    }

    //////////////////////////////////////////////////////////////
    // coroutines
    private var count = 0
    private fun CoroutineScope.produceBallots(producer: Iterable<EncryptedBallot>): ReceiveChannel<EncryptedBallot> =
        produce {
            for (ballot in producer) {
                send(ballot)
                yield()
                count++
            }
            channel.close()
        }

    private val confirmationCodes = mutableListOf<ConfirmationCode>()
    private val mutex = Mutex()

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<EncryptedBallot>,
        aggregator: SelectionAggregator,
        verify: (EncryptedBallot) -> Boolean,
    ) = launch(Dispatchers.Default) {
        for (ballot in input) {
            if (debugBallots) println("$id channel working on ${ballot.ballotId}")
            val result = verify(ballot)
            mutex.withLock {
                if (result) {
                    aggregator.add(ballot) // this slows down the ballot parallelism: nselections * (2 (modP multiplication))
                    confirmationCodes.add(ConfirmationCode(ballot.ballotId, ballot.confirmationCode))
                }
            }
            yield()
        }
    }
}

// check confirmation codes
data class ConfirmationCode(val ballotId: String, val code: UInt256)