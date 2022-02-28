package electionguard.protoconvert

import electionguard.ballot.Manifest
import electionguard.core.GroupContext
import electionguard.core.noNullValuesOrNull
import electionguard.core.safeEnumValueOf
import kotlinx.datetime.UtcOffset
import mu.KotlinLogging
private val logger = KotlinLogging.logger("ManifestFromProto")

data class ManifestFromProto(val groupContext: GroupContext) {

    fun translateFromProto(proto: electionguard.protogen.Manifest?): Manifest? {
        if (proto == null) {
            return null
        }

        val contests = proto.contests.map { convertContestDescription(it) }.noNullValuesOrNull()

        if (contests == null) {
            logger.error { "missing contests in Manifest" }
            return null
        }

        val electionType = convert(proto.electionType)
        val geopoliticalUnits =
            proto.geopoliticalUnits.map { convertGeopoliticalUnit(it) }.noNullValuesOrNull()

        if (electionType == null || geopoliticalUnits == null) {
            logger.error { "missing electionType or geopoliticalUnits in Manifest" }
            return null
        }

        // TODO: presumably there are many more error conditions that we need to check for here,
        //   like having zero ballot styles, zero candidates, etc.

        return Manifest(
            groupContext,
            proto.electionScopeId,
            proto.specVersion,
            electionType,
            UtcOffset.parse(proto.startDate),
            UtcOffset.parse(proto.endDate),
            geopoliticalUnits,
            proto.parties.map { convertParty(it) },
            proto.candidates.map { convertCandidate(it) },
            contests,
            proto.ballotStyles.map { convertBallotStyle(it) },
            convertInternationalizedText(proto.name),
            convertContactInformation(proto.contactInformation),
        )
    }

    private fun convertAnnotatedString(
        annotated: electionguard.protogen.AnnotatedString
    ): Manifest.AnnotatedString {
        return Manifest.makeAnnotatedString(groupContext, annotated.annotation, annotated.value)
    }

    private fun convertBallotStyle(
        proto: electionguard.protogen.BallotStyle
    ): Manifest.BallotStyle {
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

    private fun convertContactInformation(
        proto: electionguard.protogen.ContactInformation?
    ): Manifest.ContactInformation? {
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

    private fun convertContestDescription(
        proto: electionguard.protogen.ContestDescription?
    ): Manifest.ContestDescription? {
        if (proto == null) {
            return null
        }

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

    private fun convertVoteVariationType(
        type: electionguard.protogen.ContestDescription.VoteVariationType?
    ): Manifest.VoteVariationType? {
        if (type == null) {
            return null
        }

        val result = safeEnumValueOf<Manifest.VoteVariationType>(type.name)
        if (result == null) {
            logger.error { "Vote variation type $type has missing or incorrect name" }
        }
        return result
    }

    private fun convert(
        type: electionguard.protogen.Manifest.ElectionType?
    ): Manifest.ElectionType? {
        if (type == null) {
            return null
        }

        val result = safeEnumValueOf<Manifest.ElectionType>(type.name)
        if (result == null) {
            logger.error { "Vote election type $type has missing or incorrect name" }
        }
        return result
    }

    private fun convertReportingUnitType(
        type: electionguard.protogen.GeopoliticalUnit.ReportingUnitType?
    ): Manifest.ReportingUnitType? {
        if (type == null) {
            return null
        }

        val result = safeEnumValueOf<Manifest.ReportingUnitType>(type.name)
        if (result == null) {
            logger.error { "Reporting unit type $type has missing or incorrect name" }
        }
        return result
    }

    private fun convertGeopoliticalUnit(
        proto: electionguard.protogen.GeopoliticalUnit?
    ): Manifest.GeopoliticalUnit? {
        if (proto == null) {
            return null
        }

        return Manifest.makeGeopoliticalUnit(
            groupContext,
            proto.geopoliticalUnitId,
            proto.name,
            convertReportingUnitType(proto.type),
            convertContactInformation(proto.contactInformation)
        )
    }

    private fun convertInternationalizedText(
        proto: electionguard.protogen.InternationalizedText?
    ): Manifest.InternationalizedText? {
        if (proto == null) {
            return null
        }
        return Manifest.makeInternationalizedText(
            groupContext,
            proto.text.map({ convertLanguage(it) })
        )
    }

    private fun convertLanguage(proto: electionguard.protogen.Language): Manifest.Language {
        return Manifest.makeLanguage(groupContext, proto.value, proto.language)
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

    private fun convertSelectionDescription(
        proto: electionguard.protogen.SelectionDescription
    ): Manifest.SelectionDescription {
        return Manifest.makeSelectionDescription(
            groupContext,
            proto.selectionId,
            proto.candidateId,
            proto.sequenceOrder
        )
    }
}