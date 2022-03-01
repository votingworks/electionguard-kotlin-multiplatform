package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import electionguard.core.safeEnumValueOf
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ManifestFromProto")

fun electionguard.protogen.Manifest.importManifest(groupContext: GroupContext): Manifest? {

    val contests =
        this.contests.map { it.importContestDescription(groupContext) }.noNullValuesOrNull()

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
        this.geopoliticalUnits.map { it.importGeopoliticalUnit(groupContext) }.noNullValuesOrNull()
    if (geopoliticalUnits == null) {
        logger.error { "missing geopoliticalUnits in Manifest" }
        return null
    }

    return groupContext.manifestOf(
        this.electionScopeId,
        this.specVersion,
        this.electionType.importElectionType() ?: Manifest.ElectionType.unknown,
        this.startDate, // LocalDateTime.parse(this.startDate),
        this.endDate, // LocalDateTime.parse(this.endDate),
        this.geopoliticalUnits.map { it.importGeopoliticalUnit(groupContext) },
        this.parties.map { it.importParty(groupContext) },
        this.candidates.map { it.importCandidate(groupContext) },
        this.contests.map { it.importContestDescription(groupContext) },
        this.ballotStyles.map { it.importBallotStyle(groupContext) },
        this.name?.let { this.name.importInternationalizedText(groupContext) },
        this.contactInformation
            ?.let { this.contactInformation.importContactInformation(groupContext) },
    )
}

private fun electionguard.protogen.AnnotatedString.importAnnotatedString(
    groupContext: GroupContext
): Manifest.AnnotatedString {
    return groupContext.annotatedStringOf(this.annotation, this.value)
}

private fun electionguard.protogen.BallotStyle.importBallotStyle(
    groupContext: GroupContext
): Manifest.BallotStyle {
    return groupContext.ballotStyleOf(
        this.ballotStyleId,
        this.geopoliticalUnitIds,
        this.partyIds,
        this.imageUrl
    )
}

private fun electionguard.protogen.Candidate.importCandidate(
    groupContext: GroupContext
): Manifest.Candidate {
    return groupContext.candidateOf(
        this.candidateId,
        this.name?.let { this.name.importInternationalizedText(groupContext) },
        this.partyId,
        this.imageUrl,
        this.isWriteIn
    )
}

private fun electionguard.protogen.ContactInformation.importContactInformation(
    groupContext: GroupContext
): Manifest.ContactInformation {
    return groupContext.contactInformationOf(
        this.addressLine,
        this.email.map { it.importAnnotatedString(groupContext) },
        this.phone.map { it.importAnnotatedString(groupContext) },
        this.name,
    )
}

private fun electionguard.protogen.ContestDescription.importContestDescription(
    groupContext: GroupContext
): Manifest.ContestDescription {
    return groupContext.contestDescriptionOf(
        this.contestId,
        this.sequenceOrder,
        this.geopoliticalUnitId,
        this.voteVariation.importVoteVariationType() ?: Manifest.VoteVariationType.other,
        // TODO ok?
        this.numberElected,
        this.votesAllowed,
        this.name,
        this.selections.map { it.importSelectionDescription(groupContext) },
        this.ballotTitle?.let { this.ballotTitle.importInternationalizedText(groupContext) },
        this.ballotSubtitle?.let { this.ballotSubtitle.importInternationalizedText(groupContext) },
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

private fun electionguard.protogen.GeopoliticalUnit.importGeopoliticalUnit(
    groupContext: GroupContext
): Manifest.GeopoliticalUnit {
    return groupContext.geopoliticalUnitOf(
        this.geopoliticalUnitId,
        this.name,
        this.type.importReportingUnitType() ?: Manifest.ReportingUnitType.unknown, // TODO ok?
        this.contactInformation
            ?.let { this.contactInformation.importContactInformation(groupContext) },
    )
}

private fun electionguard.protogen.InternationalizedText.importInternationalizedText(
    groupContext: GroupContext
): Manifest.InternationalizedText {
    return groupContext.internationalizedTextOf(this.text.map({ it.importLanguage(groupContext) }))
}

private fun electionguard.protogen.Language.importLanguage(
    groupContext: GroupContext
): Manifest.Language {
    return groupContext.languageOf(this.value, this.language)
}

private fun electionguard.protogen.Party.importParty(groupContext: GroupContext): Manifest.Party {
    return groupContext.partyOf(
        this.partyId,
        this.name?.let { this.name.importInternationalizedText(groupContext) },
        this.abbreviation,
        this.color,
        this.logoUri
    )
}

private fun electionguard.protogen.SelectionDescription.importSelectionDescription(
    groupContext: GroupContext
): Manifest.SelectionDescription {
    return groupContext.selectionDescriptionOf(
        this.selectionId,
        this.sequenceOrder,
        this.candidateId,
    )
}