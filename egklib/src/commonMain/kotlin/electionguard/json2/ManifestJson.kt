package electionguard.json2

import electionguard.ballot.Manifest
import electionguard.core.*
import kotlinx.serialization.Serializable

@Serializable
data class ManifestJson(
    val election_scope_id: String,
    val spec_version: String,
    val type: String,
    val start_date: String,
    val end_date: String,
    val geopolitical_units: List<GeopoliticalUnitJson>,
    val parties: List<PartyJson>,
    val candidates: List<CandidateJson>,
    val contests: List<ContestJson>,
    val ballot_styles: List<BallotStyleJson>,
    val name: List<LanguageJson>,
    val contact_information: ContactJson?,
)

fun Manifest.publishJson() : ManifestJson {
    return ManifestJson(
        this.electionScopeId,
        this.specVersion,
        this.electionType.name,
        this.startDate,
        this.endDate,
        this.geopoliticalUnits.map { it.publishJson() },
        this.parties.map { it.publishJson() },
        this.candidates.map { it.publishJson() },
        this.contests.map { it.publishJson() },
        this.ballotStyles.map { it.publishJson() },
        this.name.map { it.publishJson() },
        this.contactInformation?.publishJson(),
    )
}

fun ManifestJson.import(): Manifest {
    val type = safeEnumValueOf(this.type) ?: Manifest.ElectionType.unknown
    return Manifest(
        this.election_scope_id,
        this.spec_version,
        type,
        this.start_date,
        this.end_date,
        this.geopolitical_units.map { it.import() },
        this.parties.map { it.import() },
        this.candidates.map { it.import() },
        this.contests.map { it.import() },
        this.ballot_styles.map { it.import() },
        this.name.map { it.import() },
        contact_information?.import(),
    )
}

/////////////////////////

@Serializable
data class BallotStyleJson(
    val object_id: String,
    val geopolitical_unit_ids: List<String>,
    val party_ids: List<String>,
    val image_url: String?,
)

fun Manifest.BallotStyle.publishJson() = BallotStyleJson(
    this.ballotStyleId,
    this.geopoliticalUnitIds,
    this.partyIds,
    this.imageUri,
)

fun BallotStyleJson.import() : Manifest.BallotStyle {
    return Manifest.BallotStyle(
        this.object_id,
        this.geopolitical_unit_ids,
        this.party_ids,
        this.image_url,
    )
}

/////////////////////////

@Serializable
data class CandidateJson(
    val object_id: String,
    val name: String?,
    val party_id: String?,
    val image_url: String?,
    val is_write_in: Boolean,
)

fun Manifest.Candidate.publishJson() = CandidateJson(
    this.candidateId,
    this.name,
    this.partyId,
    this.imageUri,
    this.isWriteIn,
)

fun CandidateJson.import() : Manifest.Candidate {
    return Manifest.Candidate(
        this.object_id,
        this.name,
        this.party_id,
        this.image_url,
        this.is_write_in,
    )
}

/////////////////////////

@Serializable
data class ContactJson(
    val address_line: List<String>,
    val email: String?,
    val phone: String?,
    val name: String?,
)

fun Manifest.ContactInformation.publishJson() =
    ContactJson(
        this.addressLine,
        this.email,
        this.phone,
        this.name,
    )

fun ContactJson.import() =
    Manifest.ContactInformation(
        this.name,
        this.address_line,
        this.email,
        this.phone,
    )

/////////////////////////

@Serializable
data class GeopoliticalUnitJson(
    val object_id: String,
    val name: String,
    val type: String,
    val contact_information: String?,
)

fun Manifest.GeopoliticalUnit.publishJson() = GeopoliticalUnitJson(
    this.geopoliticalUnitId,
    this.name,
    this.type.name,
    this.contactInformation,
)

fun GeopoliticalUnitJson.import() : Manifest.GeopoliticalUnit {
    val type = safeEnumValueOf(this.type) ?: Manifest.ReportingUnitType.unknown
    return Manifest.GeopoliticalUnit(
        this.object_id,
        this.name,
        type,
        this.contact_information,
    )
}

/////////////////////////

@Serializable
data class PartyJson(
    val object_id: String,
    val name: String,
    val abbreviation: String?,
    val color: String?,
    val logo_uri: String?,
)

fun Manifest.Party.publishJson() = PartyJson(
    this.partyId,
    this.name,
    this.abbreviation,
    this.color,
    this.logoUri,
)

fun PartyJson.import() = Manifest.Party(
        this.object_id,
        this.name,
        this.abbreviation,
        this.color,
        this.logo_uri,
    )

/////////////////////////

@Serializable
data class LanguageJson(
    val value: String,
    val language: String,
)

fun Manifest.Language.publishJson() =
    LanguageJson(
        this.value,
        this.language,
    )

fun LanguageJson.import() =
    Manifest.Language(
        this.value,
        this.language,
    )

/////////////////////////

@Serializable
data class ContestJson(
    val object_id: String,
    val sequence_order: Int,
    val electoral_district_id: String,
    val vote_variation: String,
    val number_elected: Int,
    val votes_allowed: Int,
    val name: String,
    val ballot_selections: List<BallotSelectionJson>,
    val ballot_title: String?,
    val ballot_subtitle: String?,
)

fun Manifest.ContestDescription.publishJson() = ContestJson(
    this.contestId,
    this.sequenceOrder,
    this.geopoliticalUnitId,
    this.voteVariation.name,
    this.numberElected,
    this.votesAllowed,
    this.name,
    this.selections.map { it.publishJson() },
    this.ballotTitle,
    this.ballotSubtitle,
)

fun ContestJson.import() : Manifest.ContestDescription {
    val voteVariation = safeEnumValueOf(this.vote_variation) ?: Manifest.VoteVariationType.other

    return Manifest.ContestDescription(
        this.object_id,
        this.sequence_order,
        this.electoral_district_id,
        voteVariation,
        this.number_elected,
        this.votes_allowed,
        this.name,
        this.ballot_selections.map { it.import() },
        this.ballot_title,
        this.ballot_subtitle,
    )
}

/////////////////////////

@Serializable
data class BallotSelectionJson(
    val object_id: String,
    val sequence_order: Int,
    val candidate_id: String,
)

fun Manifest.SelectionDescription.publishJson() = BallotSelectionJson(
    this.selectionId,
    this.sequenceOrder,
    this.candidateId,
)

fun BallotSelectionJson.import() = Manifest.SelectionDescription(
    this.object_id,
    this.sequence_order,
    this.candidate_id,
)
