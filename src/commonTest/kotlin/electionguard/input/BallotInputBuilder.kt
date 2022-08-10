package electionguard.input

import electionguard.ballot.PlaintextBallot

class BallotInputBuilder internal constructor(val id: String) {
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

    fun build(): PlaintextBallot {
        return PlaintextBallot(
            id,
            style,
            contests.map {it.build() }
        )
    }

    inner class ContestBuilder internal constructor(val id: String, seqOrder : Int? = null) {
        private var seq = seqOrder?: 1
        private val selections = ArrayList<SelectionBuilder>()

        fun addSelection(id: String, vote: Int, seqOrder : Int? = null): ContestBuilder {
            val s = SelectionBuilder(id, vote, seqOrder)
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

        inner class SelectionBuilder internal constructor(private val id: String, private val vote: Int, private val seqOrder : Int? = null) {
            fun build(): PlaintextBallot.Selection {
                return PlaintextBallot.Selection(id, seqOrder?: seq++, vote, null)
            }
        }
    }

    companion object {
        private const val styleDef = "styling"
    }
}