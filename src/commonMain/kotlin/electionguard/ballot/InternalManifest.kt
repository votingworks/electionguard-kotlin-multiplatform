package electionguard.ballot

/** An election with contests that have been filled out with selection placeholders.  */
class InternalManifest(val manifest: Manifest) {
    private val contests: Map<String, ContestWithPlaceholders>

    init {
        // For each contest, append the `number_elected` number of placeholder selections to the end of the contest collection.
        this.contests = mutableMapOf<String, ContestWithPlaceholders>()
        for (contest in manifest.contests) {
            val placeholders: List<Manifest.SelectionDescription> =
                generate_placeholder_selections_from(contest, contest.number_elected)
            contests.put(contest.contest_id, contest_description_with_placeholders_from(contest, placeholders))
        }
    }

    fun getContestById(contest_id: String?): ContestWithPlaceholders? {
        return contests.get(contest_id)
    }

    /** Find the ballot style for a specified style_id  */
    fun get_ballot_style(style_id: String?): Manifest.BallotStyle? {
        return manifest.ballot_styles.find { bs -> bs.style_id.equals(style_id) }
    }

    /** Get contests whose electoral_district_id is in the given ballot style's geopolitical_unit_ids.  */
    fun get_contests_for_style(ballot_style_id: String?): List<ContestWithPlaceholders> {
        val style = get_ballot_style(ballot_style_id)
        if (style?.geopolitical_unit_ids == null) {
            return emptyList()
        }
        val gp_unit_ids = style.geopolitical_unit_ids.toList()
        return contests.values.filter { c -> gp_unit_ids.contains(c.electoral_district_id) }
    }

    /**
     * A contest that's been filled with placeholder_selections.
     * The ElectionGuard spec requires that n-of-m elections have *exactly* n counters that are one,
     * with the rest zero, so if a voter deliberately undervotes, one or more of the placeholder counters will
     * become one. This allows the `ConstantChaumPedersenProof` to verify correctly for undervoted contests.
     */
    data class ContestWithPlaceholders(
        val object_id: String,
        val electoral_district_id: String,
        val sequence_order: Int,
        val vote_variation: Manifest.VoteVariationType,
        val number_elected: Int,
        val votes_allowed: Int,
        val name: String?,
        val ballot_selections: List<Manifest.SelectionDescription>,
        val ballot_title: Manifest.InternationalizedText?,
        val ballot_subtitle: Manifest.InternationalizedText?,
        val placeholder_selections: List<Manifest.SelectionDescription>
    ) {

        fun is_valid(): Boolean {
            val contest_description_validates: Boolean = super.is_valid()
            return contest_description_validates && placeholder_selections.size === this.number_elected
        }

        /** Gets the SelectionDescription from a selection id  */
        fun getSelectionById(selection_id: String?): Manifest.SelectionDescription? {
            val first_match = this.ballot_selections.find { s -> s.selection_id.equals(selection_id) }
            if (first_match != null) {
                return first_match
            } else {
                return placeholder_selections.find { ps -> ps.selection_id.equals(selection_id) }
            }
        }
    }

    companion object {
        fun contest_description_with_placeholders_from(
            contest: Manifest.ContestDescription, placeholders: List<Manifest.SelectionDescription>
        ): ContestWithPlaceholders {
            return ContestWithPlaceholders(
                contest.contest_id,
                contest.electoral_district_id,
                contest.sequence_order,
                contest.vote_variation,
                contest.number_elected,
                contest.votes_allowed,
                contest.name,
                contest.ballot_selections,
                contest.ballot_title,
                contest.ballot_subtitle,
                placeholders
            )
        }

        /**
         * Generates the specified number of placeholder selections in ascending sequence order from the max selection sequence order
         *
         * @param contest: ContestDescription for input
         * @param count: optionally specify a number of placeholders to generate
         * @return a collection of `SelectionDescription` objects, which may be empty
         */
        fun generate_placeholder_selections_from(contest: Manifest.ContestDescription, count: Int): List<Manifest.SelectionDescription> {
            //  max_sequence_order = max([selection.sequence_order for selection in contest.ballot_selections]);
            val max_sequence_order: Int = contest.ballot_selections.map { s -> s.sequence_order }.maxOrNull() ?: 0
            val selections = mutableListOf<Manifest.SelectionDescription>()
            for (i in 0 until count) {
                val sequence_order = max_sequence_order + 1 + i
                val sd = generate_placeholder_selection_from(contest, sequence_order)
                selections.add(sd.orElseThrow<IllegalStateException>(Supplier<IllegalStateException> { IllegalStateException() }))
            }
            return selections
        }

        /**
         * Generates a placeholder selection description that is unique so it can be hashed.
         *
         * @param use_sequence_idO: an optional integer unique to the contest identifying this selection's place in the contest
         * @return a SelectionDescription or None
         */
        fun generate_placeholder_selection_from(contest: Manifest.ContestDescription, use_sequence_idO: Int?
        ) : Manifest.SelectionDescription? {

            // sequence_ids = [selection.sequence_order for selection in contest.ballot_selections]
            val sequence_ids: List<Int> = contest.ballot_selections.stream().map { s -> s.sequence_order }
                .collect(Collectors.toList())
            val use_sequence_id: Int
            if (use_sequence_idO.isEmpty()) {
                // if no sequence order is specified, take the max
                use_sequence_id = sequence_ids.stream().max(java.util.Comparator<Int> { x: Int, y: Int ->
                    java.lang.Integer.compare(
                        x,
                        y
                    )
                }).orElse(0) + 1
            } else {
                use_sequence_id = use_sequence_idO.get()
                if (sequence_ids.contains(use_sequence_id)) {
                    logger.atWarning().log("mismatched placeholder selection %s already exists", use_sequence_id)
                    return Optional.empty<Manifest.SelectionDescription>()
                }
            }
            val placeholder_object_id: String = java.lang.String.format("%s-%s", contest.object_id, use_sequence_id)
            return Optional.of<Manifest.SelectionDescription>(
                Manifest.SelectionDescription(
                    String.format("%s-placeholder", placeholder_object_id),
                    String.format("%s-candidate", placeholder_object_id),
                    use_sequence_id
                )
            )
        }

        private fun <T> toImmutableListEmpty(from: List<T>?): ImmutableList<T> {
            return if (from == null || from.isEmpty()) {
                ImmutableList.of()
            } else ImmutableList.copyOf(from)
        }
    }
}