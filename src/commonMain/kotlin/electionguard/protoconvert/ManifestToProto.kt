package electionguard.protoconvert

import electionguard.ballot.Manifest
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ManifestToProto")

fun Manifest.publishManifest(): electionguard.protogen.Manifest {
    return electionguard.protogen
        .Manifest(
            this.electionScopeId,
            this.specVersion,
            this.electionType.publishElectionType(),
            this.startDate,
            this.endDate,
            this.geopoliticalUnits.map { it.publishGeopoliticalUnit() },
            this.parties.map { it.publishParty() },
            this.candidates.map { it.publishCandidate() },
            this.contests.map { it.publishContestDescription() },
            this.ballotStyles.map { it.publishBallotStyle() },
            this.name?.let { this.name.publishInternationalizedText() },
            this.contactInformation?.let { this.contactInformation.publishContactInformation() },
            this.cryptoHash.publishUInt256(),
        )
}

private fun Manifest.AnnotatedString.publishAnnotatedString():
    electionguard.protogen.AnnotatedString {
        return electionguard.protogen.AnnotatedString(this.annotation, this.value)
    }

private fun Manifest.BallotStyle.publishBallotStyle(): electionguard.protogen.BallotStyle {
    return electionguard.protogen
        .BallotStyle(
            this.ballotStyleId,
            this.geopoliticalUnitIds,
            this.partyIds,
            this.imageUri ?: ""
        )
}

private fun Manifest.Candidate.publishCandidate(): electionguard.protogen.Candidate {
    return electionguard.protogen
        .Candidate(
            this.candidateId,
            this.name.publishInternationalizedText(),
            this.partyId ?: "",
            this.imageUri ?: "",
            this.isWriteIn
        )
}

private fun Manifest.ContactInformation.publishContactInformation():
    electionguard.protogen.ContactInformation {
        return electionguard.protogen
            .ContactInformation(
                this.name ?: "",
                this.addressLine,
                this.email.map { it.publishAnnotatedString() },
                this.phone.map { it.publishAnnotatedString() },
            )
    }

private fun Manifest.ContestDescription.publishContestDescription():
    electionguard.protogen.ContestDescription {
        return electionguard.protogen
            .ContestDescription(
                this.contestId,
                this.sequenceOrder,
                this.geopoliticalUnitId,
                this.voteVariation.publishVoteVariationType(),
                this.numberElected,
                this.votesAllowed,
                this.name,
                this.selections.map { it.publishSelectionDescription() },
                this.ballotTitle?.let { this.ballotTitle.publishInternationalizedText() },
                this.ballotSubtitle?.let { this.ballotSubtitle.publishInternationalizedText() },
                this.primaryPartyIds,
                this.cryptoHash.publishUInt256(),
            )
    }

private fun Manifest.VoteVariationType.publishVoteVariationType():
    electionguard.protogen.ContestDescription.VoteVariationType {
        return try {
            electionguard.protogen.ContestDescription.VoteVariationType.fromName(this.name)
        } catch (e: IllegalArgumentException) {
            logger.error { "Manifest.VoteVariationType $this has missing or unknown name" }
            electionguard.protogen.ContestDescription.VoteVariationType.UNKNOWN
        }
    }

private fun Manifest.ElectionType.publishElectionType():
    electionguard.protogen.Manifest.ElectionType {
        return try {
            electionguard.protogen.Manifest.ElectionType.fromName(this.name)
        } catch (e: IllegalArgumentException) {
            logger.error { "Manifest.ElectionType $this has missing or unknown name" }
            electionguard.protogen.Manifest.ElectionType.UNKNOWN
        }
    }

private fun Manifest.ReportingUnitType.publishReportingUnitType():
    electionguard.protogen.GeopoliticalUnit.ReportingUnitType {
        return try {
            electionguard.protogen.GeopoliticalUnit.ReportingUnitType.fromName(this.name)
        } catch (e: IllegalArgumentException) {
            logger.error { "Manifest.GeopoliticalUnit $this has missing or unknown name" }
            electionguard.protogen.GeopoliticalUnit.ReportingUnitType.UNKNOWN
        }
    }

private fun Manifest.GeopoliticalUnit.publishGeopoliticalUnit():
    electionguard.protogen.GeopoliticalUnit {
        return electionguard.protogen
            .GeopoliticalUnit(
                this.geopoliticalUnitId,
                this.name,
                this.type.publishReportingUnitType(),
                this.contactInformation?.let { this.contactInformation.publishContactInformation() }
            )
    }

private fun Manifest.InternationalizedText.publishInternationalizedText():
    electionguard.protogen.InternationalizedText {
        return electionguard.protogen.InternationalizedText(this.text.map { it.publishLanguage() })
    }

private fun Manifest.Language.publishLanguage(): electionguard.protogen.Language {
    return electionguard.protogen.Language(this.value, this.language)
}

private fun Manifest.Party.publishParty(): electionguard.protogen.Party {
    return electionguard.protogen
        .Party(
            this.partyId,
            this.name.publishInternationalizedText(),
            this.abbreviation ?: "",
            this.color ?: "",
            this.logoUri ?: ""
        )
}

private fun Manifest.SelectionDescription.publishSelectionDescription():
    electionguard.protogen.SelectionDescription {
        return electionguard.protogen
            .SelectionDescription(
                this.selectionId,
                this.sequenceOrder,
                this.candidateId,
                this.cryptoHash.publishUInt256(),
            )
    }
