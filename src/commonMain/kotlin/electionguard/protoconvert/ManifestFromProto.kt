package electionguard.protoconvert

import electionguard.ballot.Manifest
import electionguard.core.GroupContext

    fun electionguard.protogen.Manifest.importManifest(groupContext : GroupContext): Manifest {
        return Manifest(
            groupContext,
            this.electionScopeId,
            this.specVersion,
            this.electionType.importElectionType(),
            this.startDate, // LocalDateTime.parse(this.startDate),
            this.endDate, // LocalDateTime.parse(this.endDate),
            this.geopoliticalUnits.map { it.importGeopoliticalUnit(groupContext) },
            this.parties.map { it.importParty(groupContext) },
            this.candidates.map { it.importCandidate(groupContext) },
            this.contests.map { it.importContestDescription(groupContext) },
            this.ballotStyles.map { it.importBallotStyle(groupContext) },
            this.name?.let { this.name.importInternationalizedText(groupContext) },
            this.contactInformation?.let { this.contactInformation.importContactInformation(groupContext) },
        )
    }

    private fun electionguard.protogen.AnnotatedString.importAnnotatedString(groupContext : GroupContext): Manifest.AnnotatedString {
        return Manifest.annotatedStringOf(groupContext, this.annotation, this.value)
    }

    private fun electionguard.protogen.BallotStyle.importBallotStyle(groupContext : GroupContext): Manifest.BallotStyle {
        return Manifest.ballotStyleOf(
            groupContext,
            this.ballotStyleId,
            this.geopoliticalUnitIds,
            this.partyIds,
            this.imageUrl
        )
    }

    private fun electionguard.protogen.Candidate.importCandidate(groupContext : GroupContext): Manifest.Candidate {
        return Manifest.candidateOf(
            groupContext,
            this.candidateId,
            this.name?.let { this.name.importInternationalizedText(groupContext) },
            this.partyId,
            this.imageUrl,
            this.isWriteIn
        )
    }

    private fun electionguard.protogen.ContactInformation.importContactInformation(groupContext : GroupContext): Manifest.ContactInformation {
        return Manifest.contactInformationOf(
            groupContext,
            this.addressLine,
            this.email.map { it.importAnnotatedString(groupContext) },
            this.phone.map { it.importAnnotatedString(groupContext) },
            this.name,
        )
    }

    private fun electionguard.protogen.ContestDescription.importContestDescription(groupContext : GroupContext): Manifest.ContestDescription {
        return Manifest.contestDescriptionOf(
            groupContext,
            this.contestId,
            this.sequenceOrder,
            this.geopoliticalUnitId,
            this.voteVariation.importVoteVariationType(),
            this.numberElected,
            this.votesAllowed,
            this.name,
            this.selections.map { it.importSelectionDescription(groupContext) },
            this.ballotTitle?.let { this.ballotTitle.importInternationalizedText(groupContext) },
            this.ballotSubtitle?.let { this.ballotSubtitle.importInternationalizedText(groupContext) },
            this.primaryPartyIds
        )
    }

    private fun electionguard.protogen.ContestDescription.VoteVariationType.importVoteVariationType(): Manifest.VoteVariationType {
        return Manifest.VoteVariationType.valueOf(this.name ?: throw IllegalStateException(this.toString()))
    }

    private fun electionguard.protogen.Manifest.ElectionType.importElectionType(): Manifest.ElectionType {
        return Manifest.ElectionType.valueOf(this.name ?: throw IllegalStateException(this.toString()))
    }

    private fun importReportingUnitType(type: electionguard.protogen.GeopoliticalUnit.ReportingUnitType): Manifest.ReportingUnitType {
        return Manifest.ReportingUnitType.valueOf(type.name ?: throw IllegalStateException(type.toString()))
    }

    private fun electionguard.protogen.GeopoliticalUnit.importGeopoliticalUnit(groupContext : GroupContext): Manifest.GeopoliticalUnit {
        return Manifest.geopoliticalUnitOf(
            groupContext,
            this.geopoliticalUnitId,
            this.name,
            importReportingUnitType(this.type),
            this.contactInformation?.let { this.contactInformation.importContactInformation(groupContext) },
        )
    }

    private fun electionguard.protogen.InternationalizedText.importInternationalizedText(groupContext : GroupContext): Manifest.InternationalizedText {
        return Manifest.internationalizedTextOf(
            groupContext,
            this.text.map({ it.importLanguage(groupContext) }
            ))
    }

    private fun electionguard.protogen.Language.importLanguage(groupContext : GroupContext): Manifest.Language {
        return Manifest.makeLanguage(
            groupContext,
            this.value,
            this.language
        )
    }

    private fun electionguard.protogen.Party.importParty(groupContext : GroupContext): Manifest.Party {
        return Manifest.partyOf(
            groupContext,
            this.partyId,
            this.name?.let { this.name.importInternationalizedText(groupContext) },
            this.abbreviation,
            this.color,
            this.logoUri
        )
    }

    private fun electionguard.protogen.SelectionDescription.importSelectionDescription(groupContext : GroupContext): Manifest.SelectionDescription {
        return Manifest.selectionDescriptionOf(
            groupContext,
            this.selectionId,
            this.sequenceOrder,
            this.candidateId,
        )
    }