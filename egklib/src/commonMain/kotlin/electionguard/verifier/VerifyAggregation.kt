package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedTally
import electionguard.core.*

/**
 * Verification 7 (Correctness of ballot aggregation)
 * An election verifier must confirm for each (non-placeholder) option in each contest in the election
 * manifest that the aggregate encryption (A, B) satisfies
 * (7.A) A = Prod(αj) mod p,
 * (7.B) B = Prod(βj) mod p,
 * where the (αj, βj ) are the corresponding encryptions on all cast ballots in the election record.
 *
 * (9.F) For each contest text label that occurs in at least one submitted ballot, that contest text
 *   label occurs in the list of contests in the corresponding tally.
 */
class VerifyAggregation(
    val group: GroupContext,
    val aggregator: SelectionAggregator,
) {

    fun verify(encryptedTally: EncryptedTally, showTime: Boolean = false): Result<Boolean, String> {
        val starting = getSystemTimeInMillis()

        val errors = mutableListOf<Result<Boolean, String>>()
        var ncontests = 0
        var nselections = 0
        for (contest in encryptedTally.contests) {
            ncontests++
            for (selection in contest.selections) {
                nselections++
                val key: String = contest.contestId + "." + selection.selectionId

                // Already did the accumulation, just have to verify it.
                val accum = aggregator.get(key)
                if (accum != null) {
                    if (selection.ciphertext.pad != accum.pad) {
                        errors.add(Err("  7.A  Ballot Aggregation does not match: $key"))
                    }
                    if (selection.ciphertext.data != accum.data) {
                        errors.add(Err("  7.B  Ballot Aggregation does not match: $key"))
                    }
                } else {
                    // LOOK needed? placeholders ??
                    if (selection.ciphertext.pad != group.ZERO_MOD_P || selection.ciphertext.data != group.ZERO_MOD_P) { // TODO test
                        errors.add(Err("    Ballot Aggregation empty does not match $key"))
                    }
                }
            }
        }

        aggregator.contestSet.forEach { ballotContest ->
            if (null == encryptedTally.contests.find { it.contestId == ballotContest}) {
                errors.add(Err("   9.F Contest '$ballotContest' in ballots not in tally"))
            }
        }

        val took = getSystemTimeInMillis() - starting
        if (showTime) println("   VerifyAggregation took $took millisecs")

        return errors.merge()
    }
}

// while we are traversing the ballots, also accumulate ElGamalCiphertext in order to test the EncryptedTally
class SelectionAggregator {
    var selectionEncryptions = mutableMapOf<String, ElGamalCiphertext>()
    var contestSet = mutableSetOf<String>()
    var nballotsCast = 0

    fun add(ballot: EncryptedBallot) {
        if (ballot.state == EncryptedBallot.BallotState.CAST) {
            nballotsCast++
            for (contest in ballot.contests) {
                contestSet.add(contest.contestId)
                for (selection in contest.selections) {
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

    fun get(key: String): ElGamalCiphertext? {
        return selectionEncryptions[key]
    }
}