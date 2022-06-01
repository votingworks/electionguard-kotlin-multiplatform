package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.safeEnumValueOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ManifestFromProto")

fun importManifest(manifest: electionguard.protogen.Manifest?):
        Result<Manifest, String> {

    if (manifest == null) {
        return Err("Null Manifest")
    }

    return Ok(Manifest(
        manifest.electionScopeId,
        manifest.specVersion,
        manifest.electionType.importElectionType() ?: Manifest.ElectionType.unknown,
        manifest.startDate, // LocalDateTime.parse(this.startDate),
        manifest.endDate, // LocalDateTime.parse(this.endDate),
        manifest.geopoliticalUnits.map { it.importGeopoliticalUnit() },
        manifest.parties.map { it.importParty() },
        manifest.candidates.map { it.importCandidate() },
        manifest.contests.map { it.importContestDescription() },
        manifest.ballotStyles.map { it.importBallotStyle() },
        manifest.name?.let { manifest.name.importInternationalizedText() },
        manifest.contactInformation?.let { manifest.contactInformation.importContactInformation() },
    ))
}

private fun electionguard.protogen.AnnotatedString.importAnnotatedString():
        Manifest.AnnotatedString {
    return Manifest.AnnotatedString(this.annotation, this.value)
}

private fun electionguard.protogen.BallotStyle.importBallotStyle(): Manifest.BallotStyle {
    return Manifest.BallotStyle(
        this.ballotStyleId,
        this.geopoliticalUnitIds,
        this.partyIds,
        this.imageUrl.ifEmpty { null },
    )
}

private fun electionguard.protogen.Candidate.importCandidate(): Manifest.Candidate {
    return Manifest.Candidate(
        this.candidateId,
        if (this.name != null) this.name.importInternationalizedText() else internationalizedTextUnknown(),
        this.partyId.ifEmpty { null },
        this.imageUrl.ifEmpty { null },
        this.isWriteIn
    )
}

private fun electionguard.protogen.ContactInformation.importContactInformation():
        Manifest.ContactInformation {
    return Manifest.ContactInformation(
        this.addressLine,
        this.email.map { it.importAnnotatedString() },
        this.phone.map { it.importAnnotatedString() },
        this.name.ifEmpty { null },
    )
}

private fun electionguard.protogen.ContestDescription.importContestDescription():
        Manifest.ContestDescription {
    return Manifest.ContestDescription(
        this.contestId,
        this.sequenceOrder,
        this.geopoliticalUnitId,
        this.voteVariation.importVoteVariationType() ?: Manifest.VoteVariationType.other,
        this.numberElected,
        this.votesAllowed,
        this.name,
        this.selections.map { it.importSelectionDescription() },
        this.ballotTitle?.let { this.ballotTitle.importInternationalizedText() },
        this.ballotSubtitle?.let { this.ballotSubtitle.importInternationalizedText() },
        this.primaryPartyIds
    )
}

private fun electionguard.protogen.ContestDescription.VoteVariationType.importVoteVariationType():
        Manifest.VoteVariationType? {
    val result = safeEnumValueOf<Manifest.VoteVariationType>(this.name)
    if (result == null) {
        logger.error { "Vote variation type $this has missing or incorrect name" }
    }
    return result
}

private fun electionguard.protogen.Manifest.ElectionType.importElectionType():
        Manifest.ElectionType? {
    val result = safeEnumValueOf<Manifest.ElectionType>(this.name)
    if (result == null) {
        logger.error { "Vote election type $this has missing or incorrect name" }
    }
    return result
}

private fun electionguard.protogen.GeopoliticalUnit.ReportingUnitType.importReportingUnitType():
        Manifest.ReportingUnitType? {
    val result = safeEnumValueOf<Manifest.ReportingUnitType>(this.name)
    if (result == null) {
        logger.error { "Reporting unit type $this has missing or incorrect name" }
    }
    return result
}

private fun electionguard.protogen.GeopoliticalUnit.importGeopoliticalUnit(): Manifest.GeopoliticalUnit {
    return Manifest.GeopoliticalUnit(
        this.geopoliticalUnitId,
        this.name,
        this.type.importReportingUnitType() ?: Manifest.ReportingUnitType.unknown, // TODO ok?
        this.contactInformation?.let { this.contactInformation.importContactInformation() },
    )
}

private fun electionguard.protogen.InternationalizedText.importInternationalizedText():
        Manifest.InternationalizedText {
    return Manifest.InternationalizedText(this.text.map { it.importLanguage() })
}

private fun electionguard.protogen.Language.importLanguage(): Manifest.Language {
    return Manifest.Language(this.value, this.language)
}

private fun electionguard.protogen.Party.importParty(): Manifest.Party {
    return Manifest.Party(
        this.partyId,
        if (this.name != null) this.name.importInternationalizedText() else internationalizedTextUnknown(),
        this.abbreviation.ifEmpty { null },
        this.color.ifEmpty { null },
        this.logoUri.ifEmpty { null },
    )
}

private fun electionguard.protogen.SelectionDescription.importSelectionDescription():
        Manifest.SelectionDescription {
    return Manifest.SelectionDescription(
        this.selectionId,
        this.sequenceOrder,
        this.candidateId,
    )
}