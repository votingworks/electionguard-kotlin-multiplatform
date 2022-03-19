package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.noNullValuesOrNull
import electionguard.core.safeEnumValueOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ManifestFromProto")

fun electionguard.protogen.Manifest.importManifest(): Manifest? {

    val contests =
        this.contests.map { it.importContestDescription() }.noNullValuesOrNull()

    if (contests == null) {
        logger.error { "missing contests in Manifest" }
        return null
    }

    val electionType = this.electionType.importElectionType()
    if (electionType == null) {
        logger.error { "missing electionType in Manifest" }
        return null
    }

    val geopoliticalUnits =
        this.geopoliticalUnits.map { it.importGeopoliticalUnit() }.noNullValuesOrNull()
    if (geopoliticalUnits == null) {
        logger.error { "missing geopoliticalUnits in Manifest" }
        return null
    }

    return Manifest(
        this.electionScopeId,
        this.specVersion,
        this.electionType.importElectionType() ?: Manifest.ElectionType.unknown,
        this.startDate, // LocalDateTime.parse(this.startDate),
        this.endDate, // LocalDateTime.parse(this.endDate),
        this.geopoliticalUnits.map { it.importGeopoliticalUnit() },
        this.parties.map { it.importParty() },
        this.candidates.map { it.importCandidate() },
        this.contests.map { it.importContestDescription() },
        this.ballotStyles.map { it.importBallotStyle() },
        this.name?.let { this.name.importInternationalizedText() },
        this.contactInformation
            ?.let { this.contactInformation.importContactInformation() },
    )
}

private fun electionguard.protogen.AnnotatedString.importAnnotatedString(): Manifest.AnnotatedString {
    return Manifest.AnnotatedString(this.annotation, this.value)
}

private fun electionguard.protogen.BallotStyle.importBallotStyle(): Manifest.BallotStyle {
    return Manifest.BallotStyle(
        this.ballotStyleId,
        this.geopoliticalUnitIds,
        this.partyIds,
        this.imageUrl
    )
}

private fun electionguard.protogen.Candidate.importCandidate(): Manifest.Candidate {
    return Manifest.Candidate(
        this.candidateId,
        if (this.name != null) this.name.importInternationalizedText() else internationalizedTextUnknown(),
        this.partyId,
        this.imageUrl,
        this.isWriteIn
    )
}

private fun electionguard.protogen.ContactInformation.importContactInformation(): Manifest.ContactInformation {
    return Manifest.ContactInformation(
        this.addressLine,
        this.email.map { it.importAnnotatedString() },
        this.phone.map { it.importAnnotatedString() },
        this.name,
    )
}

private fun electionguard.protogen.ContestDescription.importContestDescription(): Manifest.ContestDescription {
    return Manifest.ContestDescription(
        this.contestId,
        this.sequenceOrder,
        this.geopoliticalUnitId,
        this.voteVariation.importVoteVariationType() ?: Manifest.VoteVariationType.other,
        // TODO ok?
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
        this.contactInformation
            ?.let { this.contactInformation.importContactInformation() },
    )
}

private fun electionguard.protogen.InternationalizedText.importInternationalizedText(): Manifest.InternationalizedText {
    return Manifest.InternationalizedText(this.text.map({ it.importLanguage() }))
}

private fun electionguard.protogen.Language.importLanguage(): Manifest.Language {
    return Manifest.Language(this.value, this.language)
}

private fun electionguard.protogen.Party.importParty(): Manifest.Party {
    return Manifest.Party(
        this.partyId,
        if (this.name != null) this.name.importInternationalizedText() else internationalizedTextUnknown(),
        this.abbreviation,
        this.color,
        this.logoUri,
    )
}

private fun electionguard.protogen.SelectionDescription.importSelectionDescription(): Manifest.SelectionDescription {
    return Manifest.SelectionDescription(
        this.selectionId,
        this.sequenceOrder,
        this.candidateId,
    )
}