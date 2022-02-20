package electionguard.ballot

import electionguard.core.CryptoHashableElement
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.hashElements
import kotlinx.datetime.UtcOffset

/**
 * The Election Manifest: defines the candidates, contests, and associated information for a specific election.
 * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/election)
 */
data class Manifest(
    val groupContext : GroupContext,
    val election_scope_id: String,
    val type: ElectionType,
    val start_date: UtcOffset,
    val end_date: UtcOffset,
    val geopolitical_units: List<GeopoliticalUnit>,
    val parties: List<Party>,
    val candidates: List<Candidate>,
    val contests: List<ContestDescription>,
    val ballot_styles: List<BallotStyle>,
    val name: InternationalizedText?,
    val contact_information: ContactInformation?
) : CryptoHashableElement {

    override fun cryptoHashElement(): ElementModQ {
        return groupContext.hashElements(
            election_scope_id,
            type.name,
            start_date.toString(), // to_iso_date_string
            end_date.toString(),
            name,
            contact_information,
            geopolitical_units,
            parties,
            candidates,
            contests,
            ballot_styles,
        )
    }

    /**
     * The type of election.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/election-type)
     */
    enum class ElectionType {
        unknown,

        /**
         * For an election held typically on the national day for elections.
         */
        general,

        /**
         * For a primary election that is for a specific party where voter eligibility is based on registration.
         */
        partisan_primary_closed,

        /**
         * For a primary election that is for a specific party where voter declares desired party or chooses in private.
         */
        partisan_primary_open,

        /**
         * For a primary election without a specified type, such as a nonpartisan primary.
         */
        primary,

        /**
         * For an election to decide a prior contest that ended with no candidate receiving a majority of the votes.
         */
        runoff,

        /**
         * For an election held out of sequence for special circumstances, for example, to fill a vacated office.
         */
        special,

        /**
         * Used when the election type is not listed in this enumeration. If used, include a specific value of the OtherType element.
         */
        other
    }

    /**
     * The type of geopolitical unit.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/reporting-unit-type)
     */
    enum class ReportingUnitType {
        unknown,

        /**
         * Used to report batches of ballots that might cross precinct boundaries.
         */
        ballot_batch,

        /**
         * Used for a ballot-style area that's generally composed of precincts.
         */
        ballot_style_area,

        /**
         * Used as a synonym for a county.
         */
        borough,

        /**
         * Used for a city that reports results or for the district that encompasses it.
         */
        city,

        /**
         * Used for city council districts.
         */
        city_council,

        /**
         * Used for one or more precincts that have been combined for the purposes of reporting. If the term ward is
         * used interchangeably with combined precinct, use combined-precinct for the ReportingUnitType.
         */
        combined_precinct,

        /**
         * Used for national legislative body districts.
         */
        congressional,

        /**
         * Used for a country.
         */
        country,

        /**
         * Used for a county or for the district that encompasses it. Synonymous with borough and parish in some localities.
         */
        county,

        /**
         * Used for county council districts.
         */
        county_council,

        /**
         * Used for a dropbox for absentee ballots.
         */
        drop_box,

        /**
         * Used for judicial districts.
         */
        judicial,

        /**
         * Used as applicable for various units such as towns, townships, villages that report votes, or for the
         * district that encompasses them.
         */
        municipality,

        /**
         * Used for a polling place.
         */
        polling_place,

        /**
         * Used if the terms for ward or district are used interchangeably with precinct.
         */
        precinct,

        /**
         * Used for a school district.
         */
        school,

        /**
         * Used for a special district.
         */
        special,

        /**
         * Used for splits of precincts.
         */
        split_precinct,

        /**
         * Used for a state or for the district that encompasses it.
         */
        state,

        /**
         * Used for a state house or assembly district.
         */
        state_house,

        /**
         * Used for a state senate district.
         */
        state_senate,

        /**
         * Used for type of municipality that reports votes or for the district that encompasses it.
         */
        town,

        /**
         * Used for type of municipality that reports votes or for the district that encompasses it.
         */
        township,

        /**
         * Used for a utility district.
         */
        utility,

        /**
         * Used for a type of municipality that reports votes or for the district that encompasses it.
         */
        village,

        /**
         * Used for a vote center.
         */
        vote_center,

        /**
         * Used for combinations or groupings of precincts or other units.
         */
        ward,

        /**
         * Used for a water district.
         */
        water,

        /**
         * Used for other types of reporting units that aren't included in this enumeration.
         * If used, provide the item's custom type in an OtherType element.
         */
        other
    }

    /**
     * Enumeration for contest algorithm or rules in the contest.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/vote-variation)
     */
    enum class VoteVariationType {
        /**
         * Each voter can select up to one option.
         */
        one_of_m,

        /**
         * Approval voting, where each voter can select as many options as desired.
         */
        approval,

        /**
         * Borda count, where each voter can rank the options, and the rankings are assigned point values.
         */
        borda,

        /**
         * Cumulative voting, where each voter can distribute their vote to up to N options.
         */
        cumulative,

        /**
         * A 1-of-m method where the winner needs more than 50% of the vote to be elected.
         */
        majority,

        /**
         * A method where each voter can select up to N options.
         */
        n_of_m,

        /**
         * A 1-of-m method where the option with the most votes is elected, regardless of whether the option has
         * more than 50% of the vote.
         */
        plurality,

        /**
         * A proportional representation method, which is any system that elects winners in proportion to the total vote.
         * For the single transferable vote (STV) method, use rcv instead.
         */
        proportional,

        /**
         * Range voting, where each voter can select a score for each option.
         */
        range,

        /**
         * Ranked choice voting (RCV), where each voter can rank the options, and the ballots are counted in rounds.
         * Also known as instant-runoff voting (IRV) and the single transferable vote (STV).
         */
        rcv,

        /**
         * A 1-of-m method where the winner needs more than some predetermined fraction of the vote to be elected,
         * and where the fraction is more than 50%. For example, the winner might need three-fifths or two-thirds of the vote.
         */
        super_majority,

        /**
         * The vote variation is a type that isn't included in this enumeration. If used, provide the item's custom type
         * in an OtherType element.
         */
        other
    }

    /**
     * An annotated character string.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/annotated-string)
     */
    inner class AnnotatedString(val annotation: String, val value: String) : CryptoHashableElement {
        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(annotation, value)
        }
    }

    /// Note: data class cannot be inner class

    /**
     * The ISO-639 language code.
     * @see [ISO 639](https://en.wikipedia.org/wiki/ISO_639)
     */
    inner class Language(val value: String?, val language: String?) : CryptoHashableElement {
        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(value, language)
        }
    }

    /**
     * Text that may have translations in multiple languages.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/internationalized-text)
     */
    inner class InternationalizedText(val text: List<Language>?) : CryptoHashableElement {
        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(text)
        }
    }

    /**
     * Contact information about persons, boards of authorities, organizations, etc.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/contact-information)
     */
    inner class ContactInformation(
        val address_line: List<String>?,
        val email: List<AnnotatedString>?,
        val phone: List<AnnotatedString>?,
        val name: String?
    ) : CryptoHashableElement {
        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(name, address_line, email, phone)
        }
    }

    /**
     * A physical or virtual unit of representation or vote/seat aggregation.
     * Use this entity to define geopolitical units such as cities, districts, jurisdictions, or precincts
     * to associate contests, offices, vote counts, or other information with those geographies.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/gp-unit)
     */
    inner class GeopoliticalUnit(
        val unit_id: String,
        val name: String,
        val type: ReportingUnitType,
        val contact_information: ContactInformation?
    ) : CryptoHashableElement {

        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(this.unit_id, name, type.name, contact_information)
        }

    }

    /** Classifies a set of contests by their set of parties and geopolitical units  */
    inner class BallotStyle(
        val style_id: String,
        val geopolitical_unit_ids: List<String>?,
        val party_ids: List<String>?,
        val image_uri: String?
    ) : CryptoHashableElement {

        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(
                this.style_id, geopolitical_unit_ids, party_ids, image_uri
            )
        }
    }

    /**
     * A political party.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/party)
     */
    inner class Party(
        val party_id: String,
        val name: InternationalizedText,
        val abbreviation: String?,
        val color: String?,
        val logo_uri: String?,
    ) : CryptoHashableElement {
        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(
                this.party_id,
                name,
                abbreviation,
                color,
                logo_uri
            )
        }
    }

    /**
     * A candidate in a contest.
     * Note: The ElectionGuard Data Spec deviates from the NIST model in that selections for any contest type
     * are considered a "candidate". for instance, on a yes-no referendum contest, two `candidate` objects
     * would be included in the model to represent the `affirmative` and `negative` selections for the contest.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/candidate)
     */
    inner class Candidate(
        val candidate_id: String,
        val name: InternationalizedText,
        val party_id: String?,
        val image_uri: String?,
        val is_write_in: Boolean?
    ) : CryptoHashableElement {

        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(
                this.candidate_id, name, party_id, image_uri
            )
        }
    }

    /**
     * The metadata that describes the structure and type of one contest in the election.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/contest)
     */
    inner class ContestDescription(
        val contest_id: String,
        val electoral_district_id: String,
        val sequence_order: Int,
        val vote_variation: VoteVariationType,
        val number_elected: Int,
        val votes_allowed: Int,
        val name: String,
        val ballot_selections: List<SelectionDescription>,
        val ballot_title: InternationalizedText?,
        val ballot_subtitle: InternationalizedText?
    ) : CryptoHashableElement {

        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(
                contest_id,
                electoral_district_id,
                sequence_order,
                vote_variation,
                number_elected,
                votes_allowed,
                name,
                ballot_selections,
                ballot_title,
                ballot_subtitle,
            )
        }
    }


    /**
     * A ballot selection for a specific candidate in a contest.
     * @see [Civics Common Standard Data Specification](https://developers.google.com/elections-data/reference/ballot-selection)
     */
    inner class SelectionDescription(
        val selection_id: String,
        val candidate_id: String,
        val sequence_order: Int
    ) : CryptoHashableElement {
        override fun cryptoHashElement(): ElementModQ {
            return groupContext.hashElements(selection_id, candidate_id, sequence_order)
        }
    }

}
