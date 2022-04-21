package electionguard.encrypt

import electionguard.ballot.CiphertextBallot
import electionguard.ballot.Manifest
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.get
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The zero encryptions of all Selections are precomputed.
 * A vote triggers the computation of the one encryption.
 * So most of the work is already done when encrypt() is called, for low latency.
 */
class BallotPrecompute(
    val group: GroupContext,
    val manifest: Manifest,
    val elgamalPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: UInt256,
    val ballotId: String,
    val ballotStyleId: String,
    val codeSeed: ElementModQ,
    masterNonce: ElementModQ?, // if null, use random
    val timestampOverride: Long? = null, // if null, use time of encryption
) {
    val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)
    val masterNonce: ElementModQ = masterNonce ?: group.randomElementModQ()
    val ballotNonce: UInt256 = hashElements(manifest.cryptoHashUInt256(), this.ballotId, masterNonce)
    val mcontests: List<Manifest.ContestDescription>
    val contests: List<Contest>

    init {
        mcontests = manifest.styleToContestsMap[ballotStyleId] ?: throw IllegalArgumentException("Unknown ballotStyleId $ballotStyleId")
        contests = mcontests.map { Contest(it) }
    }

    // for one-of-n, a non-zero vote will clear other selections
    // otherwise caller must clear selections to keep within vote limit
    fun vote(contestId: String, selectionId: String, vote: Int): Boolean {
        val contest = contests.find { it.mcontest.contestId == contestId}
        if (contest == null) {
            return false
        }
        return contest.vote(selectionId, vote)
    }

    fun encrypt(): CiphertextBallot {
        val encryptedContests = contests.map { it.encryptedContest() }

        // Ticks are defined here as number of seconds since the unix epoch (00:00:00 UTC on 1 January 1970)
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000)
        val cryptoHash = hashElements(ballotId, manifest.cryptoHashUInt256(), encryptedContests)
        val ballotCode = hashElements(codeSeed, timestamp, cryptoHash)

        val encryptedBallot = CiphertextBallot(
            this.ballotId,
            this.ballotStyleId,
            manifest.cryptoHashUInt256(),
            codeSeed.toUInt256(),
            ballotCode,
            encryptedContests,
            timestamp,
            cryptoHash,
            masterNonce,
        )
        return encryptedBallot
    }

    inner class Contest(
        val mcontest: Manifest.ContestDescription,
    ) {
        val selections = mutableListOf<Selection>()
        val placeholders = mutableListOf<Selection>()
        val contestNonce: ElementModQ
        val chaumPedersenNonce: ElementModQ

        init {
            val contestDescriptionHash = mcontest.cryptoHash
            val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
            val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
            contestNonce = nonceSequence[mcontest.sequenceOrder]
            chaumPedersenNonce = nonceSequence[0]
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
            val selection = selections.find { it.mselection.selectionId == selectionId}
            if (selection == null) {
                return false
            }
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
            return mcontest.encryptContest(
                group,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                contestNonce,
                chaumPedersenNonce,
                encryptedSelections,
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
        val isPlaceholder: Boolean = false
    ) {
        private var vote = 0
        val disjunctiveChaumPedersenNonce: ElementModQ
        val selectionNonce: ElementModQ
        val zero: CiphertextBallot.Selection

        init {
            val nonceSequence = Nonces(mselection.cryptoHash.toElementModQ(group), contestNonce)
            disjunctiveChaumPedersenNonce = nonceSequence.get(0)
            selectionNonce = nonceSequence.get(mselection.sequenceOrder)
            zero = mselection.encryptSelection(
                0,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                disjunctiveChaumPedersenNonce,
                selectionNonce,
                isPlaceholder,
                null // LOOK not handling write-ins
            )
        }

        val one: CiphertextBallot.Selection by
        lazy {
            mselection.encryptSelection(
                1,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                disjunctiveChaumPedersenNonce,
                selectionNonce,
                isPlaceholder,
                null // LOOK not handling write-ins
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