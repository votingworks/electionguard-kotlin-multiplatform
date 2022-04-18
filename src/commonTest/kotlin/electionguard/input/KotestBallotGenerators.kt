package electionguard.input

import electionguard.ballot.*
import electionguard.core.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.arbs.cars
import io.kotest.property.kotlinx.datetime.date
import kotlin.random.Random
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

private val firstNames =
    arrayListOf(
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

private val lastNames =
    arrayListOf(
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

private val colors =
    arrayListOf(
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

/**
 * Kotest generators for common ElectionGuard ballot-related data types. All are scoped inside the
 * [KotestBallotGenerators] object, purely to make them easier to import and manage.
 */
object KotestBallotGenerators {
    fun electionType(): Arb<Manifest.ElectionType> = Arb.enum()
    fun reportingUnitType(): Arb<Manifest.ReportingUnitType> = Arb.enum()

    // ElectionGuard only supports one-of-m and n-of-m elections, so that's
    // all we're going to support here, for now.
    fun voteVariationType(): Arb<Manifest.VoteVariationType> =
        Arb.element(Manifest.VoteVariationType.one_of_m, Manifest.VoteVariationType.one_of_m)

    fun color(): Arb<String> = Arb.of(colors)

    fun humanName(): Arb<String> =
        Arb.pair(Arb.of(firstNames), Arb.of(lastNames)).map { "${it.first} ${it.second}" }

    fun internationalHumanName(): Arb<Manifest.InternationalizedText> =
        // same name with multiple languages attached to it
        Arb.bind(humanName(), Arb.list(twoLetterCodes(), 1..4)) { name, codes ->
            Manifest.InternationalizedText(
                codes.map { Manifest.Language(value = name, language = it) }
            )
        }

    fun email(humanName: String): Arb<Manifest.AnnotatedString> {
        val emailGen = Arb.string(minSize = 1, maxSize = 10, Codepoint.alphanumeric())
        return Arb.bind(emailGen, emailGen, emailGen) { e1, e2, e3 ->
            Manifest.AnnotatedString(annotation = humanName, value = "$e1@$e2@$e3")
        }
    }

    private fun digits(): Arb<Codepoint> = Arb.of(('0'..'9').map { Codepoint(it.code) })

    fun phoneNumber(humanName: String): Arb<Manifest.AnnotatedString> =
        Arb.string(size = 10, codepoints = digits())
            .map { phNumber -> Manifest.AnnotatedString(annotation = humanName, value = phNumber) }

    fun address(): Arb<String> = Arb.bind(Arb.int(1..2000), Arb.cars()) { n, s -> "$n $s Street" }

    fun contactInformation(): Arb<Manifest.ContactInformation> =
        arbitrary {
            val name = humanName().bind()
            val em = listOf(email(name).bind())
            val phNumber = listOf(phoneNumber(name).bind())
            val addr = listOf(address().bind())

            Manifest.ContactInformation(
                addressLine = addr,
                email = em,
                phone = phNumber,
                name = name
            )
        }

    private fun alphaChars(): Arb<Codepoint> = Arb.of(('a'..'z').map { Codepoint(it.code) })

    private fun twoLetterCodes(): Arb<String> = Arb.string(size = 2, codepoints = alphaChars())

    fun uuid(prefix: String = ""): Arb<String> =
        // 8-4-4-4-12 hexidecimal pattern, but without any of the "version" bits
        Arb.bind(
            Arb.string(size = 8, codepoints = Codepoint.hex()),
            Arb.string(size = 4, codepoints = Codepoint.hex()),
            Arb.string(size = 4, codepoints = Codepoint.hex()),
            Arb.string(size = 4, codepoints = Codepoint.hex()),
            Arb.string(size = 12, codepoints = Codepoint.hex()),
        ) { a, b, c, d, e -> "$prefix$a-$b-$c-$d-$e" }

    fun geopoliticalUnit(): Arb<Manifest.GeopoliticalUnit> =
        Arb.bind(uuid("gpunit:"), humanName(), reportingUnitType(), contactInformation())
            { u, n, r, c -> Manifest.GeopoliticalUnit(u, n, r, c) }

    fun language(prefix: String = ""): Arb<Manifest.Language> =
        // we're just dumping alpha text here for lack of anything interesting
        Arb.bind(Arb.string(1..10), twoLetterCodes()) { n, l -> Manifest.Language("${prefix}n", l) }

    fun internationalizedText(prefix: String = ""): Arb<Manifest.InternationalizedText> =
        Arb.list(language(), 1..3).map { Manifest.InternationalizedText(it) }

    fun url(): Arb<String> =
        Arb.string(minSize = 1, maxSize = 10, codepoints = Codepoint.alphanumeric())
            .map { "https://www.$it.com" }

    fun partyList(numParties: Int): Arb<List<Manifest.Party>> {
        assert(numParties >= 1)

        val partyNames = (1..numParties).map { "Party$it" }
        val partyAbbrvs = (1..numParties).map { "P$it" }

        return Arb.list(Arb.triple(uuid("party:"), url(), color()), numParties..numParties)
            .map {
                it.mapIndexed { i, (uuid, url, color) ->
                    Manifest.Party(
                        partyId = uuid,
                        name = Manifest.simpleInternationalText(partyNames[i], "en"),
                        abbreviation = partyAbbrvs[i],
                        color = color,
                        logoUri = url
                    )
                }
            }
    }

    /**
     * Generates a [Manifest.BallotStyle] object, which rolls up a list of parties and geopolitical
     * units (passed as arguments), with some additional information added on as well.
     */
    fun ballotStyle(
        parties: List<Manifest.Party>,
        geoUnits: List<Manifest.GeopoliticalUnit>
    ): Arb<Manifest.BallotStyle> =
        arbitrary {
            assert(!parties.isEmpty())
            assert(!geoUnits.isEmpty())

            val gpUnitIds = geoUnits.map { it.geopoliticalUnitId }
            val partyIds = parties.map { it.partyId }

            val imageUri = Arb.choice(url(), Arb.constant(null)).bind()

            Manifest.BallotStyle(uuid("bs:").bind(), gpUnitIds, partyIds, imageUri)
        }

    /**
     * Generates a [Manifest.Candidate] object, assigning it one of the parties from the optional
     * [partyList] at random, with a chance that there will be no party assigned at all. Note that
     * we may end up with multiple candidates sharing the same party.
     */
    fun candidate(partyList: List<Manifest.Party>?): Arb<Manifest.Candidate> =
        arbitrary {
            val party =
                (if (partyList != null)
                    Arb.choice(Arb.of(partyList), Arb.constant(null))
                else
                    Arb.constant(null)).bind()

            val u = uuid("candidate:").bind()
            val name = internationalHumanName().bind()
            val uri = Arb.choice(url(), Arb.constant(null)).bind()

            Manifest.Candidate(u, name, party?.partyId, uri, false)
        }

    /**
     * Given a [Manifest.Candidate] and its position in a list of candidates, returns an equivalent
     * [Manifest.SelectionDescription]. The selection's `object_id` will contain the candidate's
     * `object_id` within, but will have a "c-" prefix attached, so you'll be able to tell that
     * they're related.
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
     * Generates a tuple: a list of [Manifest.Candidate] and a corresponding
     * [Manifest.ContestDescription] for an n-of-m contest.
     *
     * @param sequenceOrder integer describing the order of this contest; make these sequential when
     *     generating many contests.
     * @param partyList A list of `Party` objects; each candidate's party is drawn at random from
     *     this list.
     * @param geoUnits A list of `GeopoliticalUnit`; one of these goes into the
     *     `electoral_district_id`
     * @param n optional integer, specifying a particular value for n in this n-of-m contest,
     *     otherwise it's varied by the generator.
     * @param m optional integer, specifying a particular value for m in this n-of-m contest,
     *     otherwise it's varied by the generator.
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
                Arb.int(1..m)
                    .flatMap { candidateContestHelper(sequenceOrder, partyList, geoUnits, it, m) }
            }
            n != null && m == null -> {
                Arb.int(n..(n + 3))
                    .flatMap { candidateContestHelper(sequenceOrder, partyList, geoUnits, n, it) }
            }
            else -> {
                Arb.int(1..3)
                    .flatMap { nFinal ->
                        Arb.int(nFinal..(nFinal + 3))
                            .flatMap { mFinal ->
                                candidateContestHelper(
                                    sequenceOrder,
                                    partyList,
                                    geoUnits,
                                    nFinal,
                                    mFinal
                                )
                            }
                    }
            }
        }

    private fun candidateContestHelper(
        sequenceOrder: Int,
        partyList: List<Manifest.Party>,
        geoUnits: List<Manifest.GeopoliticalUnit>,
        n: Int,
        m: Int,
    ): Arb<Pair<List<Manifest.Candidate>, Manifest.ContestDescription>> =
        arbitrary {
            assert (n > 0)
            assert (n <= m)

            val partyIds = partyList.map { it.partyId }
            val candidates = Arb.list(candidate(partyList), m..m).bind()
            val u = uuid("candidate:").bind()
            val geoUnit = Arb.of(geoUnits).bind()

            val selectionDescriptions =
                candidates.mapIndexed { i, c -> c.toSelectionDescription(sequenceOrder, i) }
            val voteVariation =
                if (n == 1)
                    Manifest.VoteVariationType.one_of_m
                else
                    Manifest.VoteVariationType.n_of_m

            Pair(
                candidates,
                Manifest.ContestDescription(
                    contestId = "cc-${sequenceOrder}-$u",
                    sequenceOrder = sequenceOrder,
                    geopoliticalUnitId = geoUnit.geopoliticalUnitId,
                    voteVariation = voteVariation,
                    // TODO: figure out where/how/if we're supposed to encode m in here
                    numberElected = n,
                    votesAllowed = n,
                    name = "Contest $sequenceOrder",
                    selections = selectionDescriptions,
                    ballotTitle = Manifest.simpleInternationalText("Title $sequenceOrder", "en"),
                    ballotSubtitle =
                        Manifest.simpleInternationalText("Subtitle $sequenceOrder", "en"),
                    primaryPartyIds = partyIds,
                )
            )
        }

    fun manifest(maxParties: Int = 3, maxContests: Int = 3): Arb<Manifest> =
        arbitrary { rs ->
            val geoUnits = Arb.list(geopoliticalUnit(), 1..10).bind()
            val numParties = Arb.int(1..maxParties).bind()
            val parties = partyList(numParties).bind()
            val numContest = Arb.int(1..maxContests).bind()
            val contestsAndCandidates =
                (0 until numContest)
                    .map { candidateContest(it, parties, geoUnits, null, null).bind() }
            assert (!contestsAndCandidates.isEmpty())

            val candidates = contestsAndCandidates.flatMap { it.first }
            val contests = contestsAndCandidates.map { it.second }

            val styles = ballotStyle(parties, geoUnits).bind()
            val startDate = Arb.date(yearRange = 2020..2024).bind()
            val endDate = startDate + DatePeriod(days = 1)

            Manifest(
                electionScopeId = uuid("scopeId:").bind(),
                specVersion = "1.0", // does this mean anything?
                electionType = Manifest.ElectionType.general, // good enough for now
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                geopoliticalUnits = geoUnits,
                parties = parties,
                candidates = candidates,
                contests = contests,
                ballotStyles = listOf(styles),
                name = internationalizedText("Manifest: ").bind(),
                contactInformation = contactInformation().bind()
            )
        }

    /**
     * Given a manifest, generates a randomly filled plaintext ballot. If overvotes are desired, the
     * [overVotesAllowed] flag may be set to `true`.
     */
    fun plaintextVotedBallot(
        manifest: Manifest,
        overVotesAllowed: Boolean = false
    ): Arb<PlaintextBallot> =
        arbitrary {
            val numBallotStyles = manifest.ballotStyles.size
            assert (numBallotStyles > 0)

            val ballotStyle = manifest.ballotStyles[Arb.int(0 until numBallotStyles).bind()]

            val contests = manifest.getContests(ballotStyle.ballotStyleId)
            val random = Random(Arb.int().bind())
            assert (!contests.isEmpty())

            val votedContests =
                contests.map { contest ->
                    assert (contest.isValid())
                    val maxNumVoterPicks =
                        if (overVotesAllowed) contest.selections.size else contest.numberElected
                    val numVoterPicks = Arb.int(0..maxNumVoterPicks).bind()
                    val voterPicks =
                        (0..contest.numberElected).map { if (it < numVoterPicks) 1 else 0 }
                            .shuffled(random)
                    val pickedSelections =
                        contest.selections
                            .mapIndexed { index, selection ->
                                PlaintextBallot.Selection(
                                    selectionId = selection.selectionId,
                                    sequenceOrder = selection.sequenceOrder,
                                    vote = voterPicks[index],
                                    isPlaceholderSelection = false,
                                    extendedData = null
                                )
                            }

                    // TODO: do we need to include placeholders?
                    PlaintextBallot.Contest(
                        contest.contestId,
                        contest.sequenceOrder,
                        pickedSelections
                    )
                }

            PlaintextBallot(uuid("bid:").bind(), ballotStyle.ballotStyleId, votedContests, null)
        }

    /**
     * Produces a list of voted ballots from [plaintextVotedBallot] of the requested [numBallots].
     */
    fun plaintextVotedBallots(
        manifest: Manifest,
        overVotesAllowed: Boolean = false,
        numBallots: Int = 5
    ): Arb<List<PlaintextBallot>> =
        arbitrary {
            (1..numBallots).map { plaintextVotedBallot(manifest, overVotesAllowed).bind() }
        }

    /**
     * Everything necessary to exercise encryption and decryption of an election (at least, for an
     * election without guardians).
     */
    data class ElectionContextAndBallots(
        val electionContext: ElectionContext,
        val manifest: Manifest,
        val keypair: ElGamalKeypair,
        val ballots: List<PlaintextBallot>
    )

    /**
     * Top-level function useful for property-based unit tests of ElectionGuard: this function
     * takes only a handful of inputs, such as a [GroupContext], and produces a data structure
     * having all the data necessary to exercise things like encryption, tallying, and decryption.
     */
    fun electionContextAndBallots(
        groupContext: GroupContext,
        overVotesAllowed: Boolean = false,
        numBallots: Int = 5
    ): Arb<ElectionContextAndBallots> =
        arbitrary {
            val manifest = manifest().bind()
            val keypair = elGamalKeypairs(groupContext).bind()
            val elecCtx =
                ElectionContext(
                    numberOfGuardians = 1,
                    quorum = 1,
                    jointPublicKey = keypair.publicKey.key,
                    manifestHash = manifest.cryptoHash,
                    cryptoBaseHash = uint256s().bind(),
                    cryptoExtendedBaseHash = uint256s().bind(),
                    commitmentHash = uint256s().bind(),
                    extendedData = null
                )

            val ballots = plaintextVotedBallots(manifest, overVotesAllowed, numBallots).bind()

            ElectionContextAndBallots(elecCtx, manifest, keypair, ballots)
        }
}

// TODO: add support for referendum contests, in addition to the current candidate contests
