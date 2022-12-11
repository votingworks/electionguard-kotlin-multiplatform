package electionguard.json

import electionguard.ballot.Manifest
import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Manifest")
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
    val name: Map<String, TextJson>, // LOOK why Map? whats the key? Language?
    val contact_information: ContactJson?,
)

fun Manifest.publish() : ManifestJson {
    val names = this.name.map { TextJson(it.value, it.language)}

    return ManifestJson(
        this.electionScopeId,
        this.specVersion,
        this.electionType.name,
        this.startDate,
        this.endDate,
        this.geopoliticalUnits.map { it.publish() },
        this.parties.map { it.publish() },
        this.candidates.map { it.publish() },
        this.contests.map { it.publish() },
        this.ballotStyles.map { it.publish() },
        names.associateBy { it.language }, // LOOK consider removing Map
        this.contactInformation?.publish(),
    )
}

fun ManifestJson.import(): Manifest {
    val type = safeEnumValueOf(this.type) ?: Manifest.ElectionType.unknown
    val names = this.name.map { entry -> Manifest.Language(entry.value.value, entry.value.language)}
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
        names,
        contact_information?.import(),
    )
}

/////////////////////////

@Serializable
@SerialName("BallotStyle")
data class BallotStyleJson(
    val object_id: String,
    val geopolitical_unit_ids: List<String>,
    val party_ids: List<String>,
    val image_url: String?,
)

fun Manifest.BallotStyle.publish() = BallotStyleJson(
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
@SerialName("Candidate")
data class CandidateJson(
    val object_id: String,
    val name: String?,
    val party_id: String?,
    val image_url: String?,
    val is_write_in: Boolean,
)

fun Manifest.Candidate.publish() = CandidateJson(
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
@SerialName("ContactInformation")
data class ContactJson(
    val address_line: List<String>,
    val email: String?,
    val phone: String?,
    val name: String?,
)

fun Manifest.ContactInformation.publish() =
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
@SerialName("GeopoliticalUnit")
data class GeopoliticalUnitJson(
    val object_id: String,
    val name: String,
    val type: String,
    val contact_information: String?,
)

fun Manifest.GeopoliticalUnit.publish() = GeopoliticalUnitJson(
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
@SerialName("Party")
data class PartyJson(
    val object_id: String,
    val name: String,
    val abbreviation: String?,
    val color: String?,
    val logo_uri: String?,
)

fun Manifest.Party.publish() = PartyJson(
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
@SerialName("Text")
data class TextJson(
    val value: String,
    val language: String,
)

fun Manifest.Language.publish() =
    TextJson(
        this.value,
        this.language,
    )

fun TextJson.import() =
    Manifest.Language(
        this.value,
        this.language,
    )

/////////////////////////

@Serializable
@SerialName("ManifestContest")
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

fun Manifest.ContestDescription.publish() = ContestJson(
    this.contestId,
    this.sequenceOrder,
    this.geopoliticalUnitId,
    this.voteVariation.name,
    this.numberElected,
    this.votesAllowed,
    this.name,
    this.selections.map { it.publish() },
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
@SerialName("ManifestSelection")
data class BallotSelectionJson(
    val object_id: String,
    val sequence_order: Int,
    val candidate_id: String,
)

fun Manifest.SelectionDescription.publish() = BallotSelectionJson(
    this.selectionId,
    this.sequenceOrder,
    this.candidateId,
)

fun BallotSelectionJson.import() = Manifest.SelectionDescription(
    this.object_id,
    this.sequence_order,
    this.candidate_id,
)
