package electionguard.input

import electionguard.ballot.Manifest

class ManifestInputBuilder(val manifestName: String) {
    val districtDefault = "district"
    val styleDefault = "styling"

    private val contests = ArrayList<ContestBuilder>()
    private val candidates = HashMap<String, Manifest.Candidate>()
    private var style = styleDefault
    var districts = ArrayList<Manifest.GeopoliticalUnit>()
    var ballotStyles = ArrayList<Manifest.BallotStyle>()

    fun addGpunit(gpunitName: String): ManifestInputBuilder {
        districts.add(Manifest.GeopoliticalUnit(gpunitName, "name", Manifest.ReportingUnitType.congressional, null))
        return this
    }

    fun setDefaultStyle(style: String): ManifestInputBuilder {
        this.style = style
        return this
    }

    fun addStyle(style: String, vararg gpunits: String): ManifestInputBuilder {
        for (gpunit in gpunits) {
            districts.add(Manifest.GeopoliticalUnit(gpunit, "name", Manifest.ReportingUnitType.congressional, null))
        }
        ballotStyles.add(Manifest.BallotStyle(style, gpunits.asList(), emptyList(), null))
        return this
    }

    fun addCandidateAndParty(candidate_id: String, party: String?): ManifestInputBuilder {
        val c = Manifest.Candidate(candidate_id, Manifest.InternationalizedText(), party, null, false)
        candidates[candidate_id] = c
        return this
    }

    fun addBallotStyle(ballotStyle: Manifest.BallotStyle): ManifestInputBuilder {
        ballotStyles.add(ballotStyle)
        return this
    }

    fun addContest(contest_id: String): ContestBuilder {
        val c = ContestBuilder(contest_id)
        contests.add(c)
        return c
    }

    fun addContest(contest_id: String, seq: Int): ContestBuilder {
        val c = ContestBuilder(contest_id).setSequence(seq)
        contests.add(c)
        return c
    }


    fun addCandidate(candidate_id: String) {
        val c = Manifest.Candidate(candidate_id)
        candidates[candidate_id] = c
    }

    fun removeCandidate(candidate_id: String): ManifestInputBuilder {
        candidates.remove(candidate_id)
        return this
    }

    fun build(): Manifest {
        if (districts.isEmpty()) {
            districts.add(
                Manifest.GeopoliticalUnit(
                    districtDefault,
                    "name",
                    Manifest.ReportingUnitType.congressional,
                    null
                )
            )
        }
        if (ballotStyles.isEmpty()) {
            ballotStyles.add(Manifest.BallotStyle(style, listOf(districtDefault), emptyList(), null))
        }
        val parties: List<Manifest.Party> = listOf(Manifest.Party("dog"), Manifest.Party("cat"))
        return Manifest(
            manifestName, "2.0.0", Manifest.ElectionType.general,
            "start", "end",
            districts, parties, candidates.values.toList(),
            contests.map { it.build() },
            ballotStyles, null, null
        )
    }

    private var contest_seq = 0
    private var selection_seq = 0

    inner class ContestBuilder internal constructor(val id: String) {
        private var seq: Int = contest_seq++
        private val selections = ArrayList<SelectionBuilder>()
        private var type: Manifest.VoteVariationType = Manifest.VoteVariationType.one_of_m
        private var allowed = 1
        private var district: String = districtDefault
        private val name = "name"

        fun setVoteVariationType(type: Manifest.VoteVariationType, allowed: Int): ContestBuilder {
            require(allowed > 0)
            this.type = type
            this.allowed = if (type === Manifest.VoteVariationType.one_of_m) 1 else allowed
            return this
        }

        fun setGpunit(gpunit: String): ContestBuilder {
            district = gpunit
            return this
        }

        fun setSequence(seq: Int): ContestBuilder {
            this.seq = seq
            return this
        }

        fun addSelection(id: String, candidate_id: String): ContestBuilder {
            val s  = SelectionBuilder(id, candidate_id)
            selections.add(s)
            addCandidate(candidate_id)
            return this
        }

        fun addSelection(id: String, candidate_id: String, seq: Int): ContestBuilder {
            val s: SelectionBuilder = SelectionBuilder(id, candidate_id).setSequence(seq)
            selections.add(s)
            addCandidate(candidate_id)
            return this
        }

        fun done(): ManifestInputBuilder {
            return this@ManifestInputBuilder
        }

        fun build(): Manifest.ContestDescription {
            require(selections.size > 0)
            if (type === Manifest.VoteVariationType.approval) {
                allowed = selections.size
            }
            if (type === Manifest.VoteVariationType.n_of_m) {
                require(allowed <= selections.size)
            }

            // String contestId,
            //                              String electoral_district_id,
            //                              int sequence_order,
            //                              VoteVariationType vote_variation,
            //                              int number_elected,
            //                              int votes_allowed,
            //                              String name,
            //                              List<SelectionDescription> ballot_selections,
            //                              @Nullable InternationalizedText ballot_title,
            //                              @Nullable InternationalizedText ballot_subtitle
            return Manifest.ContestDescription(
                id, seq, district,
                type, allowed, allowed, name,
                selections.map { it.build() },
                null, null, emptyList(),
            )
        }

        inner class SelectionBuilder internal constructor(private val id: String, private val candidate_id: String) {
            private var seq: Int = selection_seq++
            fun setSequence(seq: Int): SelectionBuilder {
                this.seq = seq
                return this
            }

            fun build(): Manifest.SelectionDescription {
                return Manifest.SelectionDescription(id, seq, candidate_id)
            }
        }
    }
}