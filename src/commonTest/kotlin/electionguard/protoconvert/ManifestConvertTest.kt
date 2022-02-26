package electionguard.protoconvert

import electionguard.ballot.Manifest
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals

class ManifestConvertTest {

    @Test
    fun roundtripManifest() {
        val context = tinyGroup()
        val manifest = generateFakeManifest(context)
        val convertTo = ManifestToProto(context)
        val proto = convertTo.translateToProto(manifest)
        val convertFrom = ManifestFromProto(context)
        val roundtrip = convertFrom.translateFromProto(proto)
        assertEquals(roundtrip, manifest)
    }

    companion object {
        fun generateFakeManifest(context: GroupContext): Manifest {
            //     val groupContext: GroupContext,
            //    val electionScopeId: String,
            //    val specVersion: String,
            //    val electionType: ElectionType,
            //    val startDate: UtcOffset,
            //    val endDate: UtcOffset,
            //    val geopoliticalUnits: List<GeopoliticalUnit>,
            //    val parties: List<Party>,
            //    val candidates: List<Candidate>,
            //    val contests: List<ContestDescription>,
            //    val ballotStyles: List<BallotStyle>,
            //    val name: InternationalizedText?,
            //    val contactInformation: ContactInformation?
            return Manifest(
                context,
                "electionScopeId",
                "specVersion",
                Manifest.ElectionType.primary,
                "2022-02-22T00:00:00",
                "2022-02-23T23:59:57",
                List(11) { generateGeopoliticalUnit(it, context) },
                List(12) { generateParty(it, context) },
                List(13) { generateCandidate(it, context) },
                List(9) { generateContest(it, context) },
                List(8) { generateBallotStyle(it, context) },
                generateInternationalizedText(context),
                generateContactInformation(context),
            )
        }

        //         val geopoliticalUnitId: String,
        //        val name: String,
        //        val type: ReportingUnitType,
        //        val contactInformation: ContactInformation?,
        //        val cryptoHash: ElementModQ
        private fun generateGeopoliticalUnit(cseq: Int, context: GroupContext): Manifest.GeopoliticalUnit {
            return Manifest.makeGeopoliticalUnit(
                context,
                "geopoliticalUnitId" + cseq,
                "name" + cseq,
                Manifest.ReportingUnitType.city,
                generateContactInformation(context),
            )
        }

        //         val partyId: String,
        //        val name: InternationalizedText?,
        //        val abbreviation: String?,
        //        val color: String?,
        //        val logoUri: String?,
        //        val cryptoHash: ElementModQ
        private fun generateParty(cseq: Int, context: GroupContext): Manifest.Party {
            return Manifest.makeParty(
                context,
                "party" + cseq,
                generateInternationalizedText(context),
                "aggrieved",
                "color",
                "red",
            )
        }

        //         val candidateId: String,
        //        val name: InternationalizedText?,
        //        val partyId: String?,
        //        val imageUri: String?,
        //        val isWriteIn: Boolean,
        //        val cryptoHash: ElementModQ
        private fun generateCandidate(cseq: Int, context: GroupContext): Manifest.Candidate {
            return Manifest.makeCandidate(
                context,
                "candidate" + cseq,
                generateInternationalizedText(context),
                "party" + cseq,
                "imageUri",
                false,
            )
        }

        //         val contestId: String,
        //        val sequenceOrder: Int,
        //        val geopoliticalUnitId: String,
        //        val voteVariation: VoteVariationType,
        //        val numberElected: Int,
        //        val votesAllowed: Int,
        //        val name: String,
        //        val selections: List<SelectionDescription>,
        //        val ballotTitle: InternationalizedText?,
        //        val ballotSubtitle: InternationalizedText?,
        //        val primaryPartyIds: List<String>,
        //        val cryptoHash: ElementModQ
        private fun generateContest(cseq: Int, context: GroupContext): Manifest.ContestDescription {
            return Manifest.makeContestDescription(
                context,
                "contest" + cseq,
                cseq,
                "geounit",
                Manifest.VoteVariationType.n_of_m,
                1,
                1,
                "contest name",
                List(11) { generateSelection(it, context) },
                generateInternationalizedText(context),
                generateInternationalizedText(context),
                List(11) { "paltry$it" },
            )
        }

        //         val selectionId: String,
        //        val sequenceOrder: Int,
        //        val candidateId: String,
        //        val cryptoHash: ElementModQ
        private fun generateSelection(sseq: Int, context: GroupContext): Manifest.SelectionDescription {
            return Manifest.makeSelectionDescription(
                context,
                "selection" + sseq,
                sseq,
                "candidate" + sseq,
            )
        }

        //         val ballotStyleId: String,
        //        val geopoliticalUnitIds: List<String>,
        //        val partyIds: List<String>,
        //        val imageUri: String?,
        //        val cryptoHash: ElementModQ
        private fun generateBallotStyle(cseq: Int, context: GroupContext): Manifest.BallotStyle {
            return Manifest.makeBallotStyle(
                context,
                "ballotStyle" + cseq,
                List(3) { "geode$it" },
                List(11) { "paltry$it" },
                "Imagine",
            )
        }

        // val text: List<Language>
        private fun generateInternationalizedText(context: GroupContext): Manifest.InternationalizedText {
            return Manifest.makeInternationalizedText(
                context,
                List(3) { generateLanguage(it, context) },
            )
        }

        //         val addressLine: List<String>,
        //        val email: List<AnnotatedString>,
        //        val phone: List<AnnotatedString>,
        //        val name: String?,
        //        val cryptoHash: ElementModQ
        private fun generateContactInformation(context: GroupContext): Manifest.ContactInformation {
            return Manifest.makeContactInformation(
                context,
                List(3) { "addressLine$it" },
                List(3) { generateAnnotatedString(it, context) },
                List(3) { generateAnnotatedString(it, context) },
                "name",
            )
        }

        // val value: String, val language: String,
        private fun generateLanguage(seq: Int, context: GroupContext): Manifest.Language {
            return Manifest.makeLanguage(
                context,
                "text" + seq,
                "language:$seq:$seq",
            )
        }

        // val annotation: String, val value: String,
        private fun generateAnnotatedString(seq: Int, context: GroupContext): Manifest.AnnotatedString {
            return Manifest.makeAnnotatedString(
                context,
                "snnotste" + seq,
                "value:$seq:$seq",
            )
        }
    }
}