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
            manifest.name.map { importLanguage(it) },
            manifest.contactInformation?.let { importContactInformation(manifest.contactInformation) },
        )
    )
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
        proto.name.ifEmpty { null },
        proto.partyId.ifEmpty { null },
        proto.imageUrl.ifEmpty { null },
        proto.isWriteIn
    )

private fun importContactInformation(proto: electionguard.protogen.ContactInformation) =
    Manifest.ContactInformation(
        proto.name.ifEmpty { null },
        proto.addressLine,
        proto.email.ifEmpty { null },
        proto.phone.ifEmpty { null },
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
        proto.ballotTitle.ifEmpty { null },
        proto.ballotSubtitle.ifEmpty { null },
    )

private fun importGeopoliticalUnit(proto: electionguard.protogen.GeopoliticalUnit) =
    Manifest.GeopoliticalUnit(
        proto.geopoliticalUnitId,
        proto.name,
        importReportingUnitType(proto.type) ?: Manifest.ReportingUnitType.unknown,
        proto.contactInformation.ifEmpty { null },
    )

private fun importLanguage(proto: electionguard.protogen.Language) =
    Manifest.Language(proto.value, proto.language)

private fun importParty(proto: electionguard.protogen.Party) =
    Manifest.Party(
        proto.partyId,
        proto.name,
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

//// enums

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

private fun importVoteVariationType(proto: electionguard.protogen.ContestDescription.VoteVariationType):
        Manifest.VoteVariationType? {
    val result = safeEnumValueOf<Manifest.VoteVariationType>(proto.name)
    if (result == null) {
        logger.error { "Manifest.VoteVariationType $proto has missing or unknown name" }
    }
    return result
}