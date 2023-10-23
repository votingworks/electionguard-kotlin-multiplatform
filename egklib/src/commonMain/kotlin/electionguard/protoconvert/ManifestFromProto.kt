package electionguard.protoconvert

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.safeEnumValueOf
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("ManifestFromProto")

fun electionguard.protogen.Manifest.import(): Result<Manifest, String> {
    return Ok(
        Manifest(
            this.electionScopeId,
            this.specVersion,
            this.electionType.import() ?: Manifest.ElectionType.unknown,
            this.startDate,
            this.endDate,
            this.geopoliticalUnits.map { it.import() },
            this.parties.map { it.import() },
            this.candidates.map { it.import() },
            this.contests.map { it.import() },
            this.ballotStyles.map { it.import() },
            this.name.map { it.import() },
            this.contactInformation?.import(),
        )
    )
}

private fun electionguard.protogen.BallotStyle.import() =
    Manifest.BallotStyle(
        this.ballotStyleId,
        this.geopoliticalUnitIds,
        this.partyIds,
        this.imageUrl.ifEmpty { null },
    )

private fun electionguard.protogen.Candidate.import() =
    Manifest.Candidate(
        this.candidateId,
        this.name.ifEmpty { null },
        this.partyId.ifEmpty { null },
        this.imageUrl.ifEmpty { null },
        this.isWriteIn
    )

private fun electionguard.protogen.ContactInformation.import() =
    Manifest.ContactInformation(
        this.name.ifEmpty { null },
        this.addressLine,
        this.email.ifEmpty { null },
        this.phone.ifEmpty { null },
    )

private fun electionguard.protogen.ContestDescription.import() =
    Manifest.ContestDescription(
        this.contestId,
        this.sequenceOrder,
        this.geopoliticalUnitId,
        this.voteVariation.import() ?: Manifest.VoteVariationType.other,
        this.numberElected,
        this.votesAllowed,
        this.name,
        this.selections.map { it.import() },
        this.ballotTitle.ifEmpty { null },
        this.ballotSubtitle.ifEmpty { null },
    )

private fun electionguard.protogen.GeopoliticalUnit.import() =
    Manifest.GeopoliticalUnit(
        this.geopoliticalUnitId,
        this.name,
        this.type.import() ?: Manifest.ReportingUnitType.unknown,
        this.contactInformation.ifEmpty { null },
    )

private fun electionguard.protogen.Language.import() =
    Manifest.Language(this.value, this.language)

private fun electionguard.protogen.Party.import() =
    Manifest.Party(
        this.partyId,
        this.name,
        this.abbreviation.ifEmpty { null },
        this.color.ifEmpty { null },
        this.logoUri.ifEmpty { null },
    )

private fun electionguard.protogen.SelectionDescription.import() =
    Manifest.SelectionDescription(
        this.selectionId,
        this.sequenceOrder,
        this.candidateId,
    )

//// enums

private fun electionguard.protogen.Manifest.ElectionType.import():
        Manifest.ElectionType? {
    val result = safeEnumValueOf<Manifest.ElectionType>(this.name)
    if (result == null) {
        logger.error { "Manifest.ElectionType $this has missing or unknown name" }
    }
    return result
}

private fun electionguard.protogen.GeopoliticalUnit.ReportingUnitType.import():
        Manifest.ReportingUnitType? {
    val result = safeEnumValueOf<Manifest.ReportingUnitType>(this.name)
    if (result == null) {
        logger.error { "Manifest.ReportingUnitType $this has missing or unknown name" }
    }
    return result
}

private fun electionguard.protogen.ContestDescription.VoteVariationType.import():
        Manifest.VoteVariationType? {
    val result = safeEnumValueOf<Manifest.VoteVariationType>(this.name)
    if (result == null) {
        logger.error { "Manifest.VoteVariationType $this has missing or unknown name" }
    }
    return result
}