package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.Guardian
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextTally
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.isValid
import electionguard.core.toElementModQ
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
import kotlin.math.roundToInt

private const val debug = false

/**
 * 8. Correctness of partial decryptions.
 * Confirm for each (non-placeholder) option for each decrypting guardian Ti:
 *  (A) The given value vi is in the set Zq .
 *  (B) The given values ai and bi are both in the set Zrp .
 *  (C) The challenge value ci satisfies ci = H(Q, (A, B), (ai , bi), Mi).
 *  (D) The equation g^vi mod p = (ai * Ki^ci ) mod p is satisfied.
 *  (E) The equation A^vi mod p = (bi * Mi^ci ) mod p is satisfied.
 *
 * where
 *  Ki = public key of guardian Ti
 *  (A, B) = encrypted aggregate total of votes in contest LOOK WRONG encrypted selection vote
 *  Mi = partial decryption of (A, B) by guardian Ti
 *  (ai , bi) = commitment by guardian Ti to partial decryption of (A, B)
 *  ci = challenge to partial decryption of guardian Ti
 *  vi = response to challenge of guardian T
 *
 * 9. Correctness of substitute data for missing guardians.
 * Confirm for each (non-placeholder) option for each missing guardian Ti and for each surrogate guardian Tℓ:
 * (A) The given value v_i,ℓ is in the set Zq .
 * (B) The given values a_i,ℓ and b_i,ℓ are both in the set Zrp .
 * (C) The challenge value c_i,ℓ satisfies c_i,ℓ = H(Q, (A, B), (a_i,ℓ, b_i,ℓ), M_i,ℓ).
 * (D) The equation (g ^ v_i,ℓ) mod p = (a_i,ℓ * PKi ^ c_i,ℓ  ) mod p is satisfied.
 * (E) The equation (A ^ v_i,ℓ) mod p = (b_i,ℓ * M_i,ℓ ^ c_i,ℓ ) mod p is satisfied.
 * where
 *  (A, B) = encrypted aggregate total of votes in contest LOOK WRONG encrypted selection vote
 *  M_i,ℓ = share of guardian Tℓ of missing partial decryption of (A, B) by guardian Ti
 *  (a_i,ℓ, b_i,ℓ) = commitment by guardian Tℓ to share of partial decryption for missing guardian Ti
 *  c_i,ℓ = challenge to guardian Tℓ ’s share of missing partial decryption of guardian Ti
 *  v_i,ℓ = response to challenge of guardian Tℓ ’s share of partial decryption of guardian Ti
 *  PKi = ∏ (𝐾_𝑖,𝑗 ^ (l^j)) for 𝑗=0..𝑘−1 (k is quorum)
 *  𝐾_𝑖,𝑗 = g ^ coeff_i,j
 *  coeff_i,j = random polynomial coefficients 𝑗=0..𝑘−1 (k is quorum) for guardian Ti
 *  l = the x coordinate of guardian l, corresponding to the (1 based) guardian index  LOOK not defined in the spec
 *
 * 11. An election verifier should confirm the following equations for each (non-placeholder) option in
 * each contest in the ballot coding file.
 * (A) B = (M ⋅ (∏ Mi )) mod p.
 * (B) M = K^t mod p.
 * where:
 *  B = component of encrypted aggregate total of votes in contest
 *  Mi = partial decryption of (A, B) by guardian Ti
 *  M = full decryption of (A, B)
 *  t = tally value
 *
 * (C) An election verifier should also confirm that the text labels listed in the election record match
 * the corresponding text labels in the ballot coding file.
 * </pre>
*/
@OptIn(ExperimentalCoroutinesApi::class)
class VerifyDecryptedTally(
    val group: GroupContext,
    val manifest: Manifest,
    val jointPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: ElementModQ,
    guardians: List<Guardian>,
) {
    val guardianMap: Map<String, Guardian> = guardians.associateBy { it.guardianId }

    fun verifyDecryptedTally(tally: PlaintextTally): Stats {
        var ncontests = 0
        var nselections = 0
        var nshares = 0
        val errors = mutableListOf<String>()

        for (contest in tally.contests.values) {
            ncontests++

            for (selection in contest.selections.values) {
                nselections++
                val where2 = "${contest.contestId}/${selection.selectionId}"
                if (!manifest.contestAndSelectionSet.contains(where2)) {
                    errors.add("   11.C Fail manifest does not contain ${where2} ")
                }
                val selResult = verifySelectionDecryption(where2, selection)
                if (selResult is Err) {
                    errors.add(selResult.error)
                }

                for (partialDecryption in selection.partialDecryptions) {
                    nshares++
                    if (partialDecryption.proof != null) { // directly computed
                        val guardian = this.guardianMap[partialDecryption.guardianId]
                        val guardianKey = guardian?.publicKey()
                        if (guardianKey != null) {
                            // test that the proof is correct covers 8.A, 8.B, 8.C
                            val svalid = partialDecryption.proof.isValid(
                                group.G_MOD_P,
                                guardianKey,
                                selection.message.pad,
                                partialDecryption.share(),
                                arrayOf(
                                    cryptoExtendedBaseHash,
                                    guardianKey,
                                    selection.message.pad,
                                    selection.message.data
                                ), // section 7
                                arrayOf(partialDecryption.share())
                            )
                            if (svalid is Err) {
                                errors.add("   8. Fail guardian $guardian share proof for $where2 = ${svalid.error} ")
                            }
                            // TODO I think 8.D and 8.E are not needed because we have simplified proofs.
                            //   review when 2.0 verification spec is out
                        } else {
                            errors.add("Cant find guardian ${partialDecryption.guardianId} in $where2")
                        }

                    } else if (partialDecryption.recoveredDecryptions.isNotEmpty()) { // indirectly computed

                        for (recoveredDecryption in partialDecryption.recoveredDecryptions) {
                            val gx = recoveredDecryption.recoveryKey
                            val hx = recoveredDecryption.share
                            // test that the proof is correct covers 9.A, 9.B, 9.C
                            val recovered = recoveredDecryption.proof.isValid(
                                group.G_MOD_P,
                                gx,
                                selection.message.pad,
                                hx,
                                arrayOf(
                                    cryptoExtendedBaseHash,
                                    gx,
                                    selection.message.pad,
                                    selection.message.data
                                ), // section 7
                                arrayOf(hx)
                            )
                            if (recovered is Err) {
                                errors.add("    CompensatedDecryption proof failure $where2 = ${recovered.error}")
                            }
                            // TODO 9.D and 9.E are not needed because we have simplified proofs ??
                            //   review now against Java or when 2.0 verification spec is out
                        }

                    } else {
                        errors.add("Must have partialDecryption.proof or missingDecryptions $where2")
                    }
                }
            }
        }
        val allValid = errors.isEmpty()
        return Stats(tally.tallyId, allValid, ncontests, nselections, errors, nshares)
    }

    /**
     * 11. An election verifier should confirm the following equations for each (non-placeholder) option in
     * each contest in the ballot coding file.
     * <pre>
     * (A) B = (M ⋅ (∏ Mi )) mod p.
     * (B) M = g^t mod p.
     *
     * (C) An election verifier should also confirm that the text labels listed in the election record match
     * the corresponding text labels in the ballot coding file.
     * </pre>
     */
    fun verifySelectionDecryption(where: String, selection: PlaintextTally.Selection): Result<Boolean, String> {
        val errors = mutableListOf<String>()
        for (share in selection.partialDecryptions) {
            val guardian = guardianMap[share.guardianId]
            val guardianPublicKey = guardian?.publicKey() ?: throw IllegalStateException("Cant find guardian ${share.guardianId}")
            val partialDecryptions: Iterable<ElementModP> = selection.partialDecryptions
                .map { s -> s.share() }
            val productMi: ElementModP = with(group) { partialDecryptions.multP() }
            val M: ElementModP = selection.value
            val B: ElementModP = selection.message.data
            if (!B.equals(M * productMi)) {
                errors.add(" 11.A Tally Decryption failed for $where")
            }

            // (B) 𝑀 = 𝑔^𝑡 mod 𝑝.
            val tallyQ = selection.tally.toElementModQ(group)
            if (!selection.value.equals(jointPublicKey powP tallyQ)) {
                errors.add(" 11.B Tally Decryption failed for $where}");
            }
        }
        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
    }

    fun verifySpoiledBallotTallies(
        ballots: Iterable<PlaintextTally>,
        nthreads: Int,
        showTime: Boolean = false
    ): StatsAccum {
        val starting = getSystemTimeInMillis()

        runBlocking {
            val verifierJobs = mutableListOf<Job>()
            val ballotProducer = produceTallies(ballots)
            repeat(nthreads) {
                verifierJobs.add(launchVerifier(it, ballotProducer) { ballot -> verifyDecryptedTally(ballot) })
            }

            // wait for all encryptions to be done, then close everything
            joinAll(*verifierJobs.toTypedArray())
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
        if (showTime) println(" verifySpoiledBallotTallies ok=${globalStat.allOk} took $took millisecs for $count ballots = $perBallot msecs/ballot")
        return globalStat
    }

    private val globalStat = StatsAccum()
    private var count = 0
    private fun CoroutineScope.produceTallies(producer: Iterable<PlaintextTally>): ReceiveChannel<PlaintextTally> =
        produce {
            for (tally in producer) {
                send(tally)
                yield()
                count++
            }
            channel.close()
        }

    private val mutex = Mutex()

    private fun CoroutineScope.launchVerifier(
        id: Int,
        input: ReceiveChannel<PlaintextTally>,
        verify: (PlaintextTally) -> Stats,
    ) = launch(Dispatchers.Default) {
        for (tally in input) {
            if (debug) println("$id channel working on ${tally.tallyId}")
            val stat = verify(tally)
            mutex.withLock {
                globalStat.add(stat)
            }
            yield()
        }
    }

}