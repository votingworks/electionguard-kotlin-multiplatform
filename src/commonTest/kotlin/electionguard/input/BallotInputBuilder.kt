package electionguard.input

import electionguard.ballot.PlaintextBallot

class BallotInputBuilder internal constructor(val id: String) {
    private val contests = ArrayList<ContestBuilder>()
    private var style = styleDef

    fun setStyle(style: String): BallotInputBuilder {
        this.style = style
        return this
    }

    fun addContest(contest_id: String): ContestBuilder {
        val c = ContestBuilder(contest_id)
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

    inner class ContestBuilder internal constructor(val id: String) {
        private var seq = 1
        private val selections = ArrayList<SelectionBuilder>()

        fun addSelection(id: String, vote: Int): ContestBuilder {
            val s = SelectionBuilder(id, vote)
            selections.add(s)
            return this
        }

        fun done(): BallotInputBuilder {
            return this@BallotInputBuilder
        }

        fun build(): PlaintextBallot.Contest {
            return PlaintextBallot.Contest(
                id,
                seq++,
                selections.map { it.build() }
            )
        }

        inner class SelectionBuilder internal constructor(private val id: String, private val vote: Int) {
            fun build(): PlaintextBallot.Selection {
                return PlaintextBallot.Selection(id, seq++, vote, false, null)
            }
        }
    }

    companion object {
        private const val styleDef = "styling"
    }
}