package electionguard.protoconvert

import electionguard.ballot.Manifest
import electionguard.core.GroupContext

data class ManifestToProto(val groupContext: GroupContext) {

    fun translateToProto(manifest: Manifest): electionguard.protogen.Manifest {
        return electionguard.protogen
            .Manifest(
                manifest.electionScopeId,
                manifest.specVersion,
                convertElectionType(manifest.electionType),
                manifest.startDate.toString(),
                manifest.endDate.toString(),
                manifest.geopoliticalUnits.map { convertGeopoliticalUnit(it) },
                manifest.parties.map { convertParty(it) },
                manifest.candidates.map { convertCandidate(it) },
                manifest.contests.map { convertContestDescription(it) },
                manifest.ballotStyles.map { convertBallotStyle(it) },
                if (manifest.name == null) { null } else {
                    convertInternationalizedText(manifest.name)
                },
                convertContactInformation(manifest.contactInformation),
            )
    }

    private fun convertAnnotatedString(
        annotated: Manifest.AnnotatedString
    ): electionguard.protogen.AnnotatedString {
        return electionguard.protogen.AnnotatedString(annotated.annotation, annotated.value)
    }

    private fun convertBallotStyle(
        style: Manifest.BallotStyle
    ): electionguard.protogen.BallotStyle {
        return electionguard.protogen
            .BallotStyle(
                style.ballotStyleId,
                style.geopoliticalUnitIds,
                style.partyIds,
                style.imageUri ?: ""
            )
    }

    private fun convertCandidate(candidate: Manifest.Candidate): electionguard.protogen.Candidate {
        return electionguard.protogen
            .Candidate(
                candidate.candidateId,
                convertInternationalizedText(candidate.name),
                candidate.partyId ?: "",
                candidate.imageUri ?: "",
                candidate.isWriteIn
            )
    }

    private fun convertContactInformation(
        contact: Manifest.ContactInformation?
    ): electionguard.protogen.ContactInformation? {
        if (contact == null) {
            return null
        }
        return electionguard.protogen
            .ContactInformation(
                contact.name ?: "",
                contact.addressLine,
                contact.email.map { convertAnnotatedString(it) },
                contact.phone.map { convertAnnotatedString(it) },
            )
    }

    private fun convertContestDescription(
        contest: Manifest.ContestDescription
    ): electionguard.protogen.ContestDescription {
        return electionguard.protogen
            .ContestDescription(
                contest.contestId,
                contest.sequenceOrder,
                contest.geopoliticalUnitId,
                convertVoteVariationType(contest.voteVariation),
                contest.numberElected,
                contest.votesAllowed,
                contest.name,
                contest.selections.map { convertSelectionDescription(it) },
                convertInternationalizedText(contest.ballotTitle),
                convertInternationalizedText(contest.ballotSubtitle),
                contest.primaryPartyIds
            )
    }

    private fun convertVoteVariationType(
        type: Manifest.VoteVariationType
    ): electionguard.protogen.ContestDescription.VoteVariationType {
        return electionguard.protogen.ContestDescription.VoteVariationType.fromName(type.name)
    }

    private fun convertElectionType(
        type: Manifest.ElectionType
    ): electionguard.protogen.Manifest.ElectionType {
        return electionguard.protogen.Manifest.ElectionType.fromName(type.name)
    }

    private fun convertReportingUnitType(
        type: Manifest.ReportingUnitType
    ): electionguard.protogen.GeopoliticalUnit.ReportingUnitType {
        return electionguard.protogen.GeopoliticalUnit.ReportingUnitType.fromName(type.name)
    }

    private fun convertGeopoliticalUnit(
        geounit: Manifest.GeopoliticalUnit
    ): electionguard.protogen.GeopoliticalUnit {
        return electionguard.protogen
            .GeopoliticalUnit(
                geounit.geopoliticalUnitId,
                geounit.name,
                convertReportingUnitType(geounit.type),
                if (geounit.contactInformation == null) { null } else {
                    convertContactInformation(geounit.contactInformation)
                }
            )
    }

    private fun convertInternationalizedText(
        itext: Manifest.InternationalizedText?
    ): electionguard.protogen.InternationalizedText? {
        if (itext == null) {
            return null
        }
        return electionguard.protogen.InternationalizedText(itext.text.map({ convertLanguage(it) }))
    }

    private fun convertLanguage(language: Manifest.Language): electionguard.protogen.Language {
        return electionguard.protogen.Language(language.value, language.language)
    }

    private fun convertParty(party: Manifest.Party): electionguard.protogen.Party {
        return electionguard.protogen
            .Party(
                party.partyId,
                convertInternationalizedText(party.name),
                party.abbreviation ?: "",
                party.color ?: "",
                party.logoUri ?: ""
            )
    }

    private fun convertSelectionDescription(
        selection: Manifest.SelectionDescription
    ): electionguard.protogen.SelectionDescription {
        return electionguard.protogen
            .SelectionDescription(
                selection.selectionId,
                selection.sequenceOrder,
                selection.candidateId,
            )
    }
}