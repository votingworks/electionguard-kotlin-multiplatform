package electionguard.encrypt

import electionguard.ballot.ContestData
import electionguard.ballot.Manifest
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.get
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.toElementModQ

/**
 * Experimental.
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The zero encryptions of all Selections are precomputed.
 * A vote triggers the computation of the one encryption.
 * So most of the work is already done when encrypt() is called, for low latency.
 * See SelectionPrecomputeTest to get timings.
 */
class SelectionPrecompute(
    val group: GroupContext,
    val manifest: Manifest,
    val elgamalPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: UInt256,
    val ballotId: String,
    val ballotStyleId: String,
    val codeSeed: ElementModQ,
    primaryNonce: UInt256?, // if null, use random
    private val timestampOverride: Long? = null, // if null, use time of encryption
) {
    val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)
    val ballotNonce: UInt256 = hashElements(UInt256.ONE, this.ballotId, primaryNonce) // TODO
    private val mcontests: List<Manifest.ContestDescription>
    val contests: List<Contest>

    init {
        mcontests = manifest.styleToContestsMap[ballotStyleId] ?: throw IllegalArgumentException("Unknown ballotStyleId $ballotStyleId")
        contests = mcontests.map { Contest(it) }
    }

    // for one-of-n, a non-zero vote will clear other selections
    // otherwise caller must clear selections to keep within vote limit
    fun vote(contestId: String, selectionId: String, vote: Int): Boolean {
        val contest = contests.find { it.mcontest.contestId == contestId} ?: return false
        return contest.vote(selectionId, vote)
    }

    fun encrypt(): CiphertextBallot {
        val encryptedContests = contests.map { it.encryptedContest() }

        // Ticks are defined here as number of seconds since the unix epoch (00:00:00 UTC on 1 January 1970)
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000)
        val cryptoHash = hashElements(ballotId, UInt256.ONE, encryptedContests) // TODO
        val ballotCode = hashElements(codeSeed, timestamp, cryptoHash)

        //     val ballotId: String,
        //    val ballotStyleId: String,
        //    val manifestHash: UInt256, // matches config.manifestHash
        //    val confirmationCode: UInt256, // tracking code, H(B), eq 59
        //    val contests: List<Contest>,
        //    val timestamp: Long,
        //    val ballotNonce: UInt256,
        //    val isPreEncrypt: Boolean = false,
        return CiphertextBallot(
            ballotId,
            ballotStyleId,
            ballotCode,
            ByteArray(0),
            encryptedContests,
            timestamp,
            ballotNonce,
        )
    }

    inner class Contest(
        val mcontest: Manifest.ContestDescription,
    ) {
        val selections = mutableListOf<Selection>()
        private val placeholders = mutableListOf<Selection>()
        private val contestNonce: ElementModQ
        private val chaumPedersenNonce: ElementModQ
        private val contestDataNonce: UInt256

        init {
            val contestDescriptionHash = mcontest.contestHash
            val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
            val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
            contestNonce = nonceSequence[0]
            chaumPedersenNonce = nonceSequence[1]
            contestDataNonce = UInt256.random()
            mcontest.selections.forEach { selections.add(Selection(it, contestNonce, false)) }

            // Add a placeholder selection for each possible vote in the contest
            val limit = mcontest.votesAllowed
            val selectionSequenceOrderMax = mcontest.selections.maxOf { it.sequenceOrder }
            for (placeholder in 1..limit) {
                val sequenceNo = selectionSequenceOrderMax + placeholder
                placeholders.add(placeHolderSelection("${mcontest.contestId}-$sequenceNo", sequenceNo, contestNonce))
            }
        }

        fun vote(selectionId: String, vote: Int): Boolean {
            val selection = selections.find { it.mselection.selectionId == selectionId} ?: return false
            if (mcontest.voteVariation == Manifest.VoteVariationType.one_of_m) {
                selections.forEach { it.vote(0)}
            }
            selection.vote(vote)
            return true
        }

        fun encryptedContest(): CiphertextBallot.Contest {
            val votes = selections.sumOf { it.vote() }
            // check for overvotes
            if (votes > mcontest.votesAllowed) {
                // could use Result
                throw IllegalStateException("votes $votes > limit ${mcontest.votesAllowed}")
            }
            // modify the placeholders for undervotes
            for (count in 0 until mcontest.votesAllowed - votes) {
                placeholders[count].vote(1)
            }
            val allSelections = selections + placeholders
            val encryptedSelections = allSelections.map { it.encryptedSelection() }.sortedBy { it.sequenceOrder }
            val contestData = ContestData(emptyList(), emptyList())
            return mcontest.encryptContest(
                group,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                votes,
                encryptedSelections,
                contestData.encrypt(elgamalPublicKey, mcontest.votesAllowed, contestDataNonce),
            )
        }
    }

    private fun placeHolderSelection(selectionId: String, sequenceOrder: Int, contestNonce: ElementModQ): Selection {
        val mselection = Manifest.SelectionDescription(selectionId, sequenceOrder, "placeholder")
        return Selection(
            mselection,
            contestNonce,
            true,
        )
    }

    inner class Selection(
        val mselection: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        private val isPlaceholder: Boolean = false
    ) {
        private var vote = 0
        private val disjunctiveChaumPedersenNonce: ElementModQ
        private val selectionNonce: ElementModQ
        val zero: CiphertextBallot.Selection

        init {
            val nonceSequence = Nonces(mselection.selectionHash.toElementModQ(group), contestNonce)
            disjunctiveChaumPedersenNonce = nonceSequence[0]
            selectionNonce = nonceSequence[mselection.sequenceOrder]
            zero = mselection.encryptSelection(
                0,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                selectionNonce,
                isPlaceholder,
            )
        }

        val one: CiphertextBallot.Selection by
        lazy {
            mselection.encryptSelection(
                1,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                selectionNonce,
                isPlaceholder,
            )
        }

        fun vote(vote: Int): CiphertextBallot.Selection {
            this.vote = vote
            return encryptedSelection()
        }

        fun vote(): Int = vote

        fun encryptedSelection(): CiphertextBallot.Selection {
            return if (vote == 0) zero else one
        }
    }
}