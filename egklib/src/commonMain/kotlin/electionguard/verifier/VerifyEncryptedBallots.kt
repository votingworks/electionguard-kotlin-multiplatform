package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedBallotChain
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.publish.ElectionRecord
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

    fun verifyBallots(ballots: Iterable<EncryptedBallot>, stats: Stats = Stats(), showTime: Boolean = false): Result<Boolean, String> {
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
                    ) { ballot -> verifyEncryptedBallot(ballot, stats) })
            }

            // wait for all verifications to be done
            joinAll(*verifierJobs.toTypedArray())
        }

        // check duplicate confirmation codes (7.C): LOOK what if there are multiple records for the election?
        // LOOK what about checking for duplicate ballot ids?
        val checkDuplicates = mutableMapOf<UInt256, String>()
        confirmationCodes.forEach {
            if (checkDuplicates[it.code] != null) {
                allResults.add(Err("    7.C, 17.D. Duplicate confirmation code for ballot ${it.ballotId} and ${checkDuplicates[it.code]}"))
            }
            checkDuplicates[it.code] = it.ballotId
        }

        if (showTime) {
            val took = getSystemTimeInMillis() - starting
            val perBallot = if (count == 0) 0 else (took.toDouble() / count).sigfig()
            println("   VerifyEncryptedBallots with $nthreads threads took $took millisecs wallclock for $count ballots = $perBallot msecs/ballot")
        }
        return allResults.merge()
    }

    fun verifyEncryptedBallot(ballot: EncryptedBallot, stats: Stats): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()
        val results = mutableListOf<Result<Boolean, String>>()

        var ncontests = 0
        var nselections = 0
        for (contest in ballot.contests) {
            val where = "${ballot.ballotId}/${contest.contestId}"
            ncontests++
            nselections += contest.selections.size

            contest.selections.forEach {
                results.add(verifySelection(where, it, manifest.optionLimit(contest.contestId)))
            }

            // Verification 6 (Adherence to vote limits)
            val texts: List<ElGamalCiphertext> = contest.selections.map { it.encryptedVote }
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
            val cvalid = contest.proof.verify(
                ciphertextAccumulation,
                this.jointPublicKey,
                this.extendedBaseHash,
                manifest.contestLimit(contest.contestId)
            )
            if (cvalid is Err) {
                results.add(Err("    6. ChaumPedersenProof validation error for $where = ${cvalid.error} "))
            }

            // χl = H(HE ; 0x23, l, K, α1 , β1 , α2 , β2 . . . , αm , βm ) 7.A
            val ciphers = mutableListOf<ElementModP>()
            texts.forEach {
                ciphers.add(it.pad)
                ciphers.add(it.data)
            }
            val contestHash = hashFunction(extendedBaseHash.bytes, 0x23.toByte(), contest.sequenceOrder, jointPublicKey.key, ciphers)
            if (contestHash != contest.contestHash) {
                results.add(Err("    7.A. Incorrect contest hash for contest ${contest.contestId} "))
            }

            if (ballot.isPreencrypt) {
                results.add(verifyPreencryptionShortCodes(ballot.ballotId, contest))
            }
        }

        if (!ballot.isPreencrypt) {
            // The ballot confirmation code H(B) = H(HE ; 0x24, χ1 , χ2 , . . . , χmB , Baux ) ; 7.B
            val contestHashes = ballot.contests.map { it.contestHash }
            val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), contestHashes, ballot.codeBaux)
            if (confirmationCode != ballot.confirmationCode) {
                results.add(Err("    7.B. Incorrect ballot confirmation code for ballot ${ballot.ballotId} "))
            }
        } else {
            results.add(verifyPreencryptedCode(ballot))
        }
        // TODO ballot chaining 7.D-G

        stats.of("verifyEncryptions", "selection").accum(getSystemTimeInMillis() - starting, nselections)
        if (debugBallots) println(" Ballot '${ballot.ballotId}' ncontests = $ncontests nselections = $nselections")
        return results.merge()
    }

    // Verification 5 (Well-formedness of selection encryptions)
    private fun verifySelection(where: String, selection: EncryptedBallot.Selection, optionLimit : Int): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        val here = "${where}/${selection.selectionId}"

        val svalid = selection.proof.verify(
            selection.encryptedVote,
            this.jointPublicKey,
            this.extendedBaseHash,
            optionLimit,
        )
        if (svalid is Err) {
            errors.add(Err("    5. ChaumPedersenProof validation error for ${here}} = ${svalid.error} "))
        }
        return errors.merge()
    }

    //////////////////////////////////////////////////////////////////////////////
    // ballot chaining, section 7

    fun verifyConfirmationChain(consumer: ElectionRecord): Result<Boolean, String> {
        val results = mutableListOf<Result<Boolean, String>>()

        consumer.encryptingDevices().forEach { device ->
            // println("verifyConfirmationChain device=$device")
            val ballotChainResult = consumer.readEncryptedBallotChain(device)
            if (ballotChainResult is Err) {
                results.add(ballotChainResult)
            } else {
                val ballotChain: EncryptedBallotChain = ballotChainResult.unwrap()
                val ballots = consumer.encryptedBallots(device) { true }

                // 7.D The initial hash code H0 satisfies H0 = H(HE ; 0x24, Baux,0 ) TODO must store?
                // and Baux,0 contains the unique voting device information. TODO how?
                val H0 = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), config.configBaux0).bytes

                // (7.E) For all 1 ≤ j ≤ ℓ, the additional input byte array used to compute Hj = H(Bj ) is equal to
                //       Baux,j = H(Bj−1 ) ∥ Baux,0 .
                var prevCC = H0
                var first = true
                ballots.forEach { ballot ->
                    val expectedBaux = if (first) H0 else hashFunction(prevCC, config.configBaux0).bytes // eq 7.E
                    first = false
                    if (!expectedBaux.contentEquals(ballot.codeBaux)) {
                        results.add(Err("    7.E. additional input byte array Baux != H(Bj−1 ) ∥ Baux,0 for ballot=${ballot.ballotId}"))
                    }
                    prevCC = ballot.confirmationCode.bytes
                }
                // 7.F The final additional input byte array is equal to Baux = H(Bℓ ) ∥ Baux,0 ∥ b(“CLOSE”, 5) and
                //      H(Bℓ ) is the final confirmation code on this device. TODO store?
                val bauxFinal = hashFunction(prevCC, config.configBaux0, "CLOSE")
                // 7.G The closing hash is correctly computed as H = H(HE ; 0x24, Baux )
                val expectedClosingHash = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), bauxFinal)
                if (expectedClosingHash != ballotChain.closingHash) {
                    results.add(Err("    7.G. The closing hash is not equal to H = H(HE ; 24, bauxFinal ) for encrypting device=$device"))
                }
            }
        }
        return results.merge()
    }

    //////////////////////////////////////////////////////////////
    // coroutines
    private val allResults = mutableListOf<Result<Boolean, String>>()
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
        agg: SelectionAggregator,
        verify: (EncryptedBallot) -> Result<Boolean, String>,
    ) = launch(Dispatchers.Default) {
        for (ballot in input) {
            if (debugBallots) println("$id channel working on ${ballot.ballotId}")
            val result = verify(ballot)
            mutex.withLock {
                agg.add(ballot) // this slows down the ballot parallelism: nselections * (2 (modP multiplication))
                confirmationCodes.add(ConfirmationCode(ballot.ballotId, ballot.confirmationCode))
                allResults.add(result)
            }
            yield()
        }
    }
}

// check confirmation codes
data class ConfirmationCode(val ballotId: String, val code: UInt256)