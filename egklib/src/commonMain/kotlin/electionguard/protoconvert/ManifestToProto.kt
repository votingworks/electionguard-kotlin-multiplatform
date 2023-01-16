package electionguard.protoconvert

import electionguard.ballot.Manifest
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ManifestToProto")

fun Manifest.publishProto() =
    electionguard.protogen.Manifest(
            this.electionScopeId,
            this.specVersion,
            this.electionType.publishProto(),
            this.startDate,
            this.endDate,
            this.geopoliticalUnits.map { it.publishProto() },
            this.parties.map { it.publishProto() },
            this.candidates.map { it.publishProto() },
            this.contests.map { it.publishProto() },
            this.ballotStyles.map { it.publishProto() },
            this.name.map { it.publishProto() },
            this.contactInformation?.let { this.contactInformation.publishProto() },
            this.manifestHash.publishProto(),
        )

private fun Manifest.BallotStyle.publishProto() =
    electionguard.protogen.BallotStyle(
            this.ballotStyleId,
            this.geopoliticalUnitIds,
            this.partyIds,
            this.imageUri ?: ""
        )

private fun Manifest.Candidate.publishProto() =
    electionguard.protogen.Candidate(
            this.candidateId,
            this.name ?: "",
            this.partyId ?: "",
            this.imageUri ?: "",
            this.isWriteIn
        )

private fun Manifest.ContactInformation.publishProto() =
    electionguard.protogen.ContactInformation(
            this.name ?: "",
            this.addressLine,
            this.email ?: "",
            this.phone ?: "",
        )

private fun Manifest.ContestDescription.publishProto() =
    electionguard.protogen.ContestDescription(
            this.contestId,
            this.sequenceOrder,
            this.geopoliticalUnitId,
            this.voteVariation.publishProto(),
            this.numberElected,
            this.votesAllowed,
            this.name,
            this.selections.map { it.publishProto() },
            this.ballotTitle?: "",
            this.ballotSubtitle?: "",
            this.contestHash.publishProto(),
        )

private fun Manifest.GeopoliticalUnit.publishProto() =
    electionguard.protogen.GeopoliticalUnit(
            this.geopoliticalUnitId,
            this.name,
            this.type.publishProto(),
            this.contactInformation ?: "",
        )

private fun Manifest.Language.publishProto() =
    electionguard.protogen.Language(this.value, this.language)

private fun Manifest.Party.publishProto() =
    electionguard.protogen.Party(
        this.partyId,
        this.name,
        this.abbreviation ?: "",
        this.color ?: "",
        this.logoUri ?: ""
    )

private fun Manifest.SelectionDescription.publishProto() =
    electionguard.protogen.SelectionDescription(
            this.selectionId,
            this.sequenceOrder,
            this.candidateId,
            this.selectionHash.publishProto(),
        )

//// enums

private fun Manifest.ElectionType.publishProto() =
    try {
        electionguard.protogen.Manifest.ElectionType.fromName(this.name)
    } catch (e: IllegalArgumentException) {
        logger.error { "Manifest.ElectionType $this has missing or unknown name" }
        electionguard.protogen.Manifest.ElectionType.UNKNOWN
    }

private fun Manifest.ReportingUnitType.publishProto() =
    try {
        electionguard.protogen.GeopoliticalUnit.ReportingUnitType.fromName(this.name)
    } catch (e: IllegalArgumentException) {
        logger.error { "Manifest.GeopoliticalUnit $this has missing or unknown name" }
        electionguard.protogen.GeopoliticalUnit.ReportingUnitType.UNKNOWN
    }

private fun Manifest.VoteVariationType.publishProto() =
    try {
        electionguard.protogen.ContestDescription.VoteVariationType.fromName(this.name)
    } catch (e: IllegalArgumentException) {
        logger.error { "Manifest.VoteVariationType $this has missing or unknown name" }
        electionguard.protogen.ContestDescription.VoteVariationType.UNKNOWN
    }