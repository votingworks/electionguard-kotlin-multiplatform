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

    return Ok(
        Manifest(
            manifest.electionScopeId,
            manifest.specVersion,
            importElectionType(manifest.electionType) ?: Manifest.ElectionType.unknown,
            manifest.startDate,
            manifest.endDate,
            manifest.geopoliticalUnits.map { importGeopoliticalUnit(it) },
            manifest.parties.map { importParty(it) },
            manifest.candidates.map { importCandidate(it) },
            manifest.contests.map { importContestDescription(it) },
            manifest.ballotStyles.map { importBallotStyle(it) },
            manifest.name?.let { importInternationalizedText(manifest.name) },
            manifest.contactInformation?.let { importContactInformation(manifest.contactInformation) },
        )
    )
}

private fun importAnnotatedString(proto: electionguard.protogen.AnnotatedString):
        Manifest.AnnotatedString {
    return Manifest.AnnotatedString(proto.annotation, proto.value)
}

private fun importBallotStyle(proto: electionguard.protogen.BallotStyle) =
    Manifest.BallotStyle(
        proto.ballotStyleId,
        proto.geopoliticalUnitIds,
        proto.partyIds,
        proto.imageUrl.ifEmpty { null },
    )

private fun importCandidate(proto: electionguard.protogen.Candidate) =
    Manifest.Candidate(
        proto.candidateId,
        if (proto.name != null) importInternationalizedText(proto.name) else internationalizedTextUnknown(),
        proto.partyId.ifEmpty { null },
        proto.imageUrl.ifEmpty { null },
        proto.isWriteIn
    )

private fun importContactInformation(proto: electionguard.protogen.ContactInformation) =
    Manifest.ContactInformation(
        proto.addressLine,
        proto.email.map { importAnnotatedString(it) },
        proto.phone.map { importAnnotatedString(it) },
        proto.name.ifEmpty { null },
    )

private fun importContestDescription(proto: electionguard.protogen.ContestDescription) =
    Manifest.ContestDescription(
        proto.contestId,
        proto.sequenceOrder,
        proto.geopoliticalUnitId,
        importVoteVariationType(proto.voteVariation) ?: Manifest.VoteVariationType.other,
        proto.numberElected,
        proto.votesAllowed,
        proto.name,
        proto.selections.map { importSelectionDescription(it) },
        proto.ballotTitle?.let { importInternationalizedText(proto.ballotTitle) },
        proto.ballotSubtitle?.let { importInternationalizedText(proto.ballotSubtitle) },
        proto.primaryPartyIds
    )

private fun importVoteVariationType(proto: electionguard.protogen.ContestDescription.VoteVariationType):
        Manifest.VoteVariationType? {
    val result = safeEnumValueOf<Manifest.VoteVariationType>(proto.name)
    if (result == null) {
        logger.error { "Manifest.VoteVariationType $proto has missing or unknown name" }
    }
    return result
}

private fun importElectionType(proto: electionguard.protogen.Manifest.ElectionType):
        Manifest.ElectionType? {
    val result = safeEnumValueOf<Manifest.ElectionType>(proto.name)
    if (result == null) {
        logger.error { "Manifest.ElectionType $proto has missing or unknown name" }
    }
    return result
}

private fun importReportingUnitType(proto: electionguard.protogen.GeopoliticalUnit.ReportingUnitType):
        Manifest.ReportingUnitType? {
    val result = safeEnumValueOf<Manifest.ReportingUnitType>(proto.name)
    if (result == null) {
        logger.error { "Manifest.ReportingUnitType $proto has missing or unknown name" }
    }
    return result
}

private fun importGeopoliticalUnit(proto: electionguard.protogen.GeopoliticalUnit) =
    Manifest.GeopoliticalUnit(
        proto.geopoliticalUnitId,
        proto.name,
        importReportingUnitType(proto.type) ?: Manifest.ReportingUnitType.unknown,
        proto.contactInformation?.let { importContactInformation(proto.contactInformation) },
    )

private fun importInternationalizedText(proto: electionguard.protogen.InternationalizedText) =
    Manifest.InternationalizedText(
        proto.text.map { importLanguage(it) }
    )

private fun importLanguage(proto: electionguard.protogen.Language) =
    Manifest.Language(proto.value, proto.language)

private fun importParty(proto: electionguard.protogen.Party) =
    Manifest.Party(
        proto.partyId,
        if (proto.name != null) importInternationalizedText(proto.name) else internationalizedTextUnknown(),
        proto.abbreviation.ifEmpty { null },
        proto.color.ifEmpty { null },
        proto.logoUri.ifEmpty { null },
    )

private fun importSelectionDescription(proto: electionguard.protogen.SelectionDescription) =
    Manifest.SelectionDescription(
        proto.selectionId,
        proto.sequenceOrder,
        proto.candidateId,
    )