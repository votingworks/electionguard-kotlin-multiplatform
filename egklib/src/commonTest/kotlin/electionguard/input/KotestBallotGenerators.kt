package electionguard.input

import electionguard.ballot.Manifest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

private val firstNames = arrayListOf(
    "James",
    "Mary",
    "John",
    "Patricia",
    "Robert",
    "Jennifer",
    "Michael",
    "Linda",
    "William",
    "Elizabeth",
    "David",
    "Barbara",
    "Richard",
    "Susan",
    "Joseph",
    "Jessica",
    "Thomas",
    "Sarah",
    "Charles",
    "Karen",
    "Christopher",
    "Nancy",
    "Daniel",
    "Margaret",
    "Matthew",
    "Lisa",
    "Anthony",
    "Betty",
    "Donald",
    "Dorothy",
    "Sylvia",
    "Viktor",
    "Camille",
    "Mirai",
    "Anant",
    "Rohan",
    "François",
    "Altuğ",
    "Sigurður",
    "Böðmóður",
    "Quang Dũng"
)

private val lastNames = arrayListOf(
    "SMITH",
    "JOHNSON",
    "WILLIAMS",
    "JONES",
    "BROWN",
    "DAVIS",
    "MILLER",
    "WILSON",
    "MOORE",
    "TAYLOR",
    "ANDERSON",
    "THOMAS",
    "JACKSON",
    "WHITE",
    "HARRIS",
    "MARTIN",
    "THOMPSON",
    "GARCIA",
    "MARTINEZ",
    "ROBINSON",
    "CLARK",
    "RODRIGUEZ",
    "LEWIS",
    "LEE",
    "WALKER",
    "HALL",
    "ALLEN",
    "YOUNG",
    "HERNANDEZ",
    "KING",
    "WRIGHT",
    "LOPEZ",
    "HILL",
    "SCOTT",
    "GREEN",
    "ADAMS",
    "BAKER",
    "GONZALEZ",
    "STEELE-LOY",
    "O'CONNOR",
    "ANAND",
    "PATEL",
    "GUPTA",
    "ĐẶNG",
)

private val colors = arrayListOf(
    "pea green",
    "puke green",
    "kelly green",
    "seafoam green",
    "blue green",
    "khaki",
    "burgundy",
    "brick red",
    "bright blue",
    "rose",
    "mustard",
    "indigo",
    "lime",
    "sea green",
    "periwinkle",
    "olive green",
    "peach",
    "pale green",
    "hot pink",
    "black",
    "lilac",
    "navy blue",
    "royal blue",
    "beige",
    "salmon",
    "olive",
    "maroon",
    "bright green",
    "mauve",
    "forest green",
    "aqua",
    "cyan",
    "tan",
    "lavender",
    "turquoise",
    "violet",
    "lime green",
    "grey",
    "sky blue",
    "yellow",
    "magenta",
    "orange",
    "teal",
    "red",
    "brown",
    "pink",
    "blue",
    "green",
    "purple",
)


/** Kotest generators for common ElectionGuard ballot-related data types. */
object KotestBallotGenerators {
    fun electionType(): Arb<Manifest.ElectionType> = Arb.enum()
    fun reportingUnitType(): Arb<Manifest.ReportingUnitType> = Arb.enum()

    // ElectionGuard only supports one-of-m and n-of-m elections, so that's
    // all we're going to support here, for now.
    fun voteVariationType(): Arb<Manifest.VoteVariationType> =
        Arb.element(Manifest.VoteVariationType.one_of_m, Manifest.VoteVariationType.one_of_m)

    fun color(): Arb<String> = Arb.of(colors)

    fun humanName(): Arb<String> =
        Arb.pair(Arb.of(firstNames), Arb.of(lastNames))
            .map { it : Pair<Any, Any> ->  "${it.first} ${it.second}" }

    fun email(): Arb<String> {
        return Arb.string(minSize = 1, maxSize = 10, Codepoint.alphanumeric())
    }

    private fun digits(): Arb<Codepoint> =
        Arb.of(('0'..'9').map { Codepoint(it.code) })

    fun phoneNumber(): Arb<String> =
        Arb.string(size = 10, codepoints = digits())

    fun address(): Arb<String> =
        Arb.bind(Arb.int(1..2000), phoneNumber()) { n, s -> "$n $s Street" }

    fun contactInformation(): Arb<Manifest.ContactInformation> = arbitrary {
        val name = humanName().bind()
        val em = email().bind()
        val phNumber = phoneNumber().bind()
        val addr = listOf(address().bind())

        Manifest.ContactInformation(
            addressLine = addr,
            email = em,
            phone = phNumber,
            name = name
        )
    }

    private fun alphaChars(): Arb<Codepoint> =
        Arb.of(('a'..'z').map { Codepoint(it.code) })

    private fun twoLetterCodes(): Arb<String> =
        Arb.string(size = 2, codepoints = alphaChars())

    fun uuid(): Arb<String> =
        // 8-4-4-4-12 hexidecimal pattern, but without any of the "version" bits
        Arb.bind(
            Arb.string(size = 8, codepoints = Codepoint.hex()),
            Arb.string(size = 4, codepoints = Codepoint.hex()),
            Arb.string(size = 4, codepoints = Codepoint.hex()),
            Arb.string(size = 4, codepoints = Codepoint.hex()),
            Arb.string(size = 12, codepoints = Codepoint.hex()),
        ) { a, b, c, d, e -> "$a-$b-$c-$d-$e" }

    fun geopoliticalUnit(): Arb<Manifest.GeopoliticalUnit> =
        Arb.bind(
            uuid(),
            humanName(),
            reportingUnitType(),
            humanName()
        ) { u, n, r, c -> Manifest.GeopoliticalUnit(u, n, r, c) }

    fun language(prefix: String = ""): Arb<Manifest.Language> =
        // we're just dumping alpha text here for lack of anything interesting
        Arb.bind(Arb.string(1..10), twoLetterCodes()) { _, l -> Manifest.Language("${prefix}n", l) }

    fun url(): Arb<String> =
        Arb.string(minSize = 1, maxSize = 10, codepoints = Codepoint.alphanumeric())
            .map { "https://www.$it.com" }

    fun partyList(numParties: Int): Arb<List<Manifest.Party>> {
        require(numParties >= 1)

        val partyNames = (1..numParties).map { "Party$it" }
        val partyAbbrvs = (1..numParties).map { "P$it" }

        return Arb.list(Arb.triple(uuid(), url(), color()), numParties..numParties).map {
            it.mapIndexed { i, trip : Triple<String, String, String> ->
                val (uuid, url, color) = trip
                Manifest.Party(
                    partyId = uuid,
                    name = partyNames[i],
                    abbreviation = partyAbbrvs[i],
                    color = color,
                    logoUri = url
                )
            }
        }
    }

    /**
     * Generates a [Manifest.BallotStyle] object, which rolls up a list of parties and
     * geopolitical units (passed as arguments), with some additional information
     * added on as well.
     */
    fun ballotStyle(
        parties: List<Manifest.Party>,
        geoUnits: List<Manifest.GeopoliticalUnit>
    ): Arb<Manifest.BallotStyle> = arbitrary {
        require(!parties.isEmpty())
        require(!geoUnits.isEmpty())

        val gpUnitIds = geoUnits.map { it.geopoliticalUnitId }
        val partyIds = parties.map { it.partyId }

        val imageUri = Arb.choice(url(), Arb.constant(null)).bind()

        Manifest.BallotStyle("bs-" + uuid().bind(), gpUnitIds, partyIds, imageUri)
    }

    /**
     * Generates a [Manifest.Candidate] object, assigning it one of the parties from the optional
     * [partyList] at random, with a chance that there will be no party assigned at all. Note that
     * we may end up with multiple candidates sharing the same party.
     */
    fun candidate(partyList: List<Manifest.Party>?): Arb<Manifest.Candidate> = arbitrary {
        val party =
            (if (partyList != null)
                Arb.choice(Arb.of(partyList), Arb.constant(null))
            else
                Arb.constant(null)).bind()

        val u = uuid().bind()
        val name = humanName().bind()
        val uri = Arb.choice(url(), Arb.constant(null)).bind()

        Manifest.Candidate(u, name, party?.partyId, uri, false)
    }

    /**
     * Given a `Candidate` and its position in a list of candidates, returns an equivalent
     * `SelectionDescription`. The selection's `object_id` will contain the candidate's
     * `object_id` within, but will have a "c-" prefix attached, so you'll be able to
     * tell that they're related.
     */
    private fun Manifest.Candidate.toSelectionDescription(
        contestSequence: Int,
        selectionSequence: Int
    ) =
        Manifest.SelectionDescription(
            selectionId = "c-${contestSequence}-${selectionSequence}-${this.candidateId}",
            sequenceOrder = selectionSequence,
            candidateId = candidateId
        )

    /**
     * Generates a tuple: a `List<Candidate>` and a corresponding `CandidateContestDescription` for
     * an n-of-m contest.
     *
     * @param sequenceOrder integer describing the order of this contest; make these sequential
     *   when generating many contests.
     * @param partyList A list of `Party` objects; each candidate's party is drawn at random from this list.
     * @param geoUnits A list of `GeopoliticalUnit`; one of these goes into the `electoral_district_id`
     * @param n optional integer, specifying a particular value for n in this n-of-m contest, otherwise
     *   it's varied by the generator.
     * @param m optional integer, specifying a particular value for m in this n-of-m contest, otherwise
     *   it's varied by the generator.
     */
    fun candidateContest(
        sequenceOrder: Int,
        partyList: List<Manifest.Party>,
        geoUnits: List<Manifest.GeopoliticalUnit>,
        n: Int?,
        m: Int?
    ): Arb<Pair<List<Manifest.Candidate>, Manifest.ContestDescription>> =
    // we're trying to first get some concrete values for n and m, after which
    // we call the helper; note that a generated m value must be at least as
        // large as n for this to be well-formed.
        when {
            n != null && m != null -> {
                candidateContestHelper(sequenceOrder, partyList, geoUnits, n, m)
            }
            n == null && m != null -> {
                Arb.int(1..m).flatMap {
                    candidateContestHelper(sequenceOrder, partyList, geoUnits, it, m)
                }
            }
            n != null && m == null -> {
                Arb.int(n..(n + 3)).flatMap {
                    candidateContestHelper(sequenceOrder, partyList, geoUnits, n, it)
                }
            }
            else -> {
                Arb.int(1..3).flatMap { nFinal ->
                    Arb.int(nFinal..(nFinal + 3)).flatMap { mFinal ->
                        candidateContestHelper(sequenceOrder, partyList, geoUnits, nFinal, mFinal)
                    }
                }
            }
        }

    /**
     * Similar to [candidateContest] but guarantees that, for the n-of-m contest that n < m,
     * therefore it's possible to construct an "ovotervoted" plaintext, suitable for subsequent
     * testing of overvote-handling.
     */
    fun candidateContestRoomForOvervoting(
        sequenceOrder: Int,
        partyList: List<Manifest.Party>,
        geoUnits: List<Manifest.GeopoliticalUnit>
    ): Arb<Pair<List<Manifest.Candidate>, Manifest.ContestDescription>> =
        Arb.int(1..3).flatMap { nFinal ->
            Arb.int((nFinal + 1)..(nFinal + 3)).flatMap { mFinal ->
                candidateContestHelper(sequenceOrder, partyList, geoUnits, nFinal, mFinal)
            }
        }

    private fun candidateContestHelper(
        sequenceOrder: Int,
        partyList: List<Manifest.Party>,
        geoUnits: List<Manifest.GeopoliticalUnit>,
        n: Int,
        m: Int,
    ): Arb<Pair<List<Manifest.Candidate>, Manifest.ContestDescription>> = arbitrary {
        require (n > 0)
        require (n <= m)

        val candidates = Arb.list(candidate(partyList), m..m).bind()
        val u = uuid().bind()
        val geoUnit = Arb.of(geoUnits).bind()

        val selectionDescriptions = candidates.mapIndexed { i, c ->
            c.toSelectionDescription(sequenceOrder, i)
        }
        val voteVariation = if (n == 1)
            Manifest.VoteVariationType.one_of_m
        else
            Manifest.VoteVariationType.n_of_m

        Pair(candidates, Manifest.ContestDescription(
            contestId = "cc-${sequenceOrder}-$u",
            sequenceOrder = sequenceOrder,
            geopoliticalUnitId = geoUnit.geopoliticalUnitId,
            voteVariation = voteVariation,
            numberElected = n,
            votesAllowed = n,
            name = "Contest $sequenceOrder",
            selections = selectionDescriptions,
            ballotTitle =  "Title $sequenceOrder",
            ballotSubtitle = "Subtitle $sequenceOrder",
        ))
    }

    fun manifest(maxParties: Int = 3, maxContests: Int = 3): Arb<Manifest> = arbitrary { _ ->
        val geoUnits = Arb.list(geopoliticalUnit(), 1..10).bind()
        val numParties = Arb.int(1..maxParties).bind()
        val parties = partyList(numParties).bind()
        val numContest = Arb.int(1..maxContests).bind()
        val contestsAndCandidates = (0 until numContest).map {
            candidateContest(it, parties, geoUnits, null, null).bind()
        }
        require (!contestsAndCandidates.isEmpty())

        val candidates = contestsAndCandidates.flatMap { it.first }
        val contests = contestsAndCandidates.map { it.second }

        val styles = ballotStyle(parties, geoUnits).bind()
        val currentMoment : Instant = Clock.System.now()
        val startDate: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.UTC)
        DatePeriod.parse("1 day")
        val endDateInstant = currentMoment.plus(1.days)
        val endDate: LocalDateTime = endDateInstant.toLocalDateTime(TimeZone.UTC)

        Manifest(
            electionScopeId = "scopeId: " + uuid().bind(),
            specVersion = "1.0", // does this mean anything?
            electionType = Manifest.ElectionType.general, // good enough for now
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            geopoliticalUnits = geoUnits,
            parties = parties,
            candidates = candidates,
            contests = contests,
            ballotStyles = listOf(styles),
            name = listOf(language("Manifest: ").bind()),
            contactInformation = contactInformation().bind()
        )
    }
}

