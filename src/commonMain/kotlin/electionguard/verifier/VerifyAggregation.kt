package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedTally
import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.plus
import kotlin.math.roundToInt

/**
 * Verification 7 (Correctness of ballot aggregation)
 * An election verifier must confirm for each (non-placeholder) option in each contest in the election
 * manifest that the aggregate encryption (A, B) satisfies
 * (7.A) A = Prod(αj) mod p,
 * (7.B) B = Prod(βj) mod p,
 * where the (αj, βj ) are the corresponding encryptions on all cast ballots in the election record.
 */
class VerifyAggregation(
    val group: GroupContext,
    val aggregator: SelectionAggregator,
) {

    fun verify(encryptedTally: EncryptedTally, showTime : Boolean = false): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()

        val errors = mutableListOf<String>()
        var ncontests = 0
        var nselections = 0
        for (contest in encryptedTally.contests) {
            ncontests++
            for (selection in contest.selections) {
                nselections++
                val key: String = contest.contestId + "." + selection.selectionId
                aggregator.get(key)

                // Already did the accumulation, just have to verify it.
                val accum = aggregator.get(key)
                if (accum != null) {
                    if (selection.ciphertext != accum) {
                        errors.add("    Ballot Aggregation does not match $key")
                    }
                } else {
                    if (selection.ciphertext.pad != group.ZERO_MOD_P || selection.ciphertext.data != group.ZERO_MOD_P) { // TODO test
                        errors.add("    Ballot Aggregation empty does not match $key")
                    }
                }
            }
        }
        val took = getSystemTimeInMillis() - starting
        if (showTime) println("   VerifyAggregation took $took millisecs")

        return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
    }
}

// while we are traversing the ballots, also accumulate ElGamalCiphertext in order to test the EncryptedTally
class SelectionAggregator {
    var selectionEncryptions = mutableMapOf<String, ElGamalCiphertext>()
    var nballotsCast = 0

    fun add(ballot: EncryptedBallot) {
        if (ballot.state == EncryptedBallot.BallotState.CAST) {
            nballotsCast++
            for (contest in ballot.contests) {
                for (selection in contest.selections) {
                    if (!selection.isPlaceholderSelection) { // only non placeholders
                        val key: String = contest.contestId + "." + selection.selectionId
                        val total = selectionEncryptions[key]
                        if (total != null) {
                            selectionEncryptions[key] = total + selection.ciphertext
                        } else {
                            selectionEncryptions[key] = selection.ciphertext
                        }
                    }
                }
            }
        }
    }

    fun get(key: String): ElGamalCiphertext? {
        return selectionEncryptions[key]
    }
}