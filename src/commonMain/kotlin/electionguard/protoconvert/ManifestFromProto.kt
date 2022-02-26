package electionguard.protoconvert

import electionguard.ballot.Manifest
import electionguard.core.GroupContext
import kotlinx.datetime.UtcOffset

data class ManifestFromProto(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.Manifest): Manifest {
        return Manifest(
            groupContext,
            proto.electionScopeId,
            proto.specVersion,
            convert(proto.electionType),
            UtcOffset.parse(proto.startDate),
            UtcOffset.parse(proto.endDate),
            proto.geopoliticalUnits.map {convertGeopoliticalUnit(it)},
            proto.parties.map{convertParty(it)},
            proto.candidates.map{convertCandidate(it)},
            proto.contests.map{convertContestDescription(it)},
            proto.ballotStyles.map{convertBallotStyle(it)},
            if (proto.name == null) { null } else { convertInternationalizedText(proto.name) },
            convertContactInformation(proto.contactInformation),
        )
    }

    private fun convertAnnotatedString(annotated: electionguard.protogen.AnnotatedString): Manifest.AnnotatedString {
        return Manifest.makeAnnotatedString(groupContext, annotated.annotation, annotated.value)
    }

    private fun convertBallotStyle(proto: electionguard.protogen.BallotStyle): Manifest.BallotStyle {
        return Manifest.makeBallotStyle(
            groupContext,
            proto.ballotStyleId,
            proto.geopoliticalUnitIds,
            proto.partyIds,
            proto.imageUrl
        )
    }

    private fun convertCandidate(proto: electionguard.protogen.Candidate): Manifest.Candidate {
        return Manifest.makeCandidate(
            groupContext,
            proto.candidateId,
            convertInternationalizedText(proto.name),
            proto.partyId,
            proto.imageUrl,
            proto.isWriteIn
        )
    }

    private fun convertContactInformation(proto: electionguard.protogen.ContactInformation?): Manifest.ContactInformation? {
        if (proto == null) {
            return null
        }
        return Manifest.makeContactInformation(
            groupContext,
            proto.addressLine,
            proto.email.map { convertAnnotatedString(it) },
            proto.phone.map { convertAnnotatedString(it) },
            proto.name,
        )
    }

    private fun convertContestDescription(proto: electionguard.protogen.ContestDescription): Manifest.ContestDescription {
        return Manifest.makeContestDescription(
            groupContext,
            proto.contestId,
            proto.geopoliticalUnitId,
            proto.sequenceOrder,
            convertVoteVariationType(proto.voteVariation),
            proto.numberElected,
            proto.votesAllowed,
            proto.name,
            proto.selections.map { convertSelectionDescription(it) },
            convertInternationalizedText(proto.ballotTitle),
            convertInternationalizedText(proto.ballotSubtitle),
            proto.primaryPartyIds
        )
    }

    private fun convertVoteVariationType(type: electionguard.protogen.ContestDescription.VoteVariationType): Manifest.VoteVariationType {
        return Manifest.VoteVariationType.valueOf(type.name?: throw IllegalStateException(type.toString()))
    }

    private fun convert(type: electionguard.protogen.Manifest.ElectionType): Manifest.ElectionType {
        return Manifest.ElectionType.valueOf(type.name?: throw IllegalStateException(type.toString()))
    }

    private fun convertReportingUnitType(type: electionguard.protogen.GeopoliticalUnit.ReportingUnitType): Manifest.ReportingUnitType {
        return Manifest.ReportingUnitType.valueOf(type.name?: throw IllegalStateException(type.toString()))
    }

    private fun convertGeopoliticalUnit(proto : electionguard.protogen.GeopoliticalUnit): Manifest.GeopoliticalUnit {
        return Manifest.makeGeopoliticalUnit(
            groupContext,
            proto.geopoliticalUnitId,
            proto.name,
            convertReportingUnitType(proto.type),
            if (proto.contactInformation == null) { null } else {convertContactInformation(proto.contactInformation)}
        )
    }

    private fun convertInternationalizedText(proto: electionguard.protogen.InternationalizedText?): Manifest.InternationalizedText? {
        if (proto == null) {
            return null
        }
        return Manifest.makeInternationalizedText(
            groupContext,
            proto.text.map( { convertLanguage(it) }
            ))
    }

    private fun convertLanguage(proto: electionguard.protogen.Language): Manifest.Language {
        return Manifest.makeLanguage(
            groupContext,
            proto.value,
            proto.language)
    }

    private fun convertParty(proto: electionguard.protogen.Party): Manifest.Party {
        return Manifest.makeParty(
            groupContext,
            proto.partyId,
            convertInternationalizedText(proto.name),
            proto.abbreviation,
            proto.color,
            proto.logoUri
        )
    }

    private fun convertSelectionDescription(proto: electionguard.protogen.SelectionDescription): Manifest.SelectionDescription {
        return Manifest.makeSelectionDescription(
            groupContext,
            proto.selectionId,
            proto.candidateId,
            proto.sequenceOrder
        )
    }
}