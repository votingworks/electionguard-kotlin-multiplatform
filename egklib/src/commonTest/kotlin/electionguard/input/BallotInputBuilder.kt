package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot

class BallotInputBuilder internal constructor(val manifest: Manifest, val id: String) {
    private val contests = ArrayList<ContestBuilder>()
    private var style = styleDef

    fun setStyle(style: String): BallotInputBuilder {
        this.style = style
        return this
    }

    fun addContest(contest_id: String, seqOrder : Int? = null): ContestBuilder {
        val c = ContestBuilder(contest_id, seqOrder)
        contests.add(c)
        return c
    }

    fun addContest(idx : Int): ContestBuilder {
        val contest = manifest.contests[idx]
        val c = ContestBuilder(contest.contestId, contest.sequenceOrder)
        contests.add(c)
        return c
    }

    fun build(): PlaintextBallot {
        return PlaintextBallot(
            id,
            style,
            contests.map {it.build() }
        )
    }

    inner class ContestBuilder internal constructor(val contestId: String, seqOrder : Int? = null) {
        private var seq = seqOrder?: 1
        private val selections = ArrayList<SelectionBuilder>()
        private val writeIns = ArrayList<String>()

        fun addSelection(id: String, vote: Int, seqOrder : Int? = null): ContestBuilder {
            val s = SelectionBuilder(id, vote, seqOrder)
            selections.add(s)
            return this
        }

        fun addSelection(idx : Int, vote: Int): ContestBuilder {
            val contest = manifest.contests.find { it.contestId == contestId }
                ?: throw IllegalArgumentException("Cant find contestId = $contestId")
            val selection = contest.selections[idx]
            val s = SelectionBuilder(selection.selectionId, vote, selection.sequenceOrder)
            selections.add(s)
            return this
        }

        fun addWriteIn(writeIn: String): ContestBuilder {
            writeIns.add(writeIn)
            return this
        }

        fun done(): BallotInputBuilder {
            return this@BallotInputBuilder
        }

        fun build(): PlaintextBallot.Contest {
            return PlaintextBallot.Contest(
                contestId,
                seq++,
                selections.map { it.build() },
                writeIns
            )
        }

        inner class SelectionBuilder internal constructor(private val selectionId: String, private val vote: Int, private val seqOrder : Int? = null) {
            fun build(): PlaintextBallot.Selection {
                return PlaintextBallot.Selection(selectionId, seqOrder?: seq++, vote)
            }
        }
    }

    companion object {
        const val styleDef = "ballotStyle"
    }
}