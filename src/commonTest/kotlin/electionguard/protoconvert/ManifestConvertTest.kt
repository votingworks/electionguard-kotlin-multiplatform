package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ManifestConvertTest {

    @Test
    fun roundtripManifest() {
        val context = tinyGroup()
        val manifest = generateFakeManifest(context)
        val proto = manifest.publishManifest()
        val roundtrip = proto.importManifest(context)
        assertNotNull(roundtrip)
        assertEquals(roundtrip.electionScopeId, manifest.electionScopeId)
        assertEquals(roundtrip.specVersion, manifest.specVersion)
        assertEquals(roundtrip.electionType, manifest.electionType)
        assertEquals(roundtrip.startDate, manifest.startDate)
        assertEquals(roundtrip.endDate, manifest.endDate)
        for (idx in 0 until roundtrip.geopoliticalUnits.size) {
            val rgpu = roundtrip.geopoliticalUnits[idx]
            val gpu = manifest.geopoliticalUnits[idx]
            assertEquals(rgpu.name, gpu.name)
            assertEquals(rgpu.geopoliticalUnitId, gpu.geopoliticalUnitId)
            assertEquals(rgpu.contactInformation?.addressLine, gpu.contactInformation?.addressLine)
            compareAS(rgpu.contactInformation?.email, gpu.contactInformation?.email)
            compareAS(rgpu.contactInformation?.phone, gpu.contactInformation?.phone)
            assertEquals(rgpu.contactInformation?.name, gpu.contactInformation?.name)
            assertEquals(rgpu.contactInformation?.cryptoHashElement(), gpu.contactInformation?.cryptoHashElement())
            assertEquals(rgpu.contactInformation?.cryptoHash, gpu.contactInformation?.cryptoHash)
            assertEquals(rgpu.contactInformation, gpu.contactInformation)
            assertEquals(rgpu.type, gpu.type)
            assertEquals(rgpu.cryptoHashElement(), gpu.cryptoHashElement())
            assertEquals(rgpu, gpu)
        }
        assertEquals(roundtrip.geopoliticalUnits, manifest.geopoliticalUnits)
        assertEquals(roundtrip, manifest)
    }

    fun compareAS(list1 : List<Manifest.AnnotatedString>?, list2 : List<Manifest.AnnotatedString>?) {
        assertEquals((list1 == null), (list2 == null))
        if ((list1 == null) || (list2 == null)) {
            return
        }

        assertEquals(list1.size, list2.size)
        for (idx in 0 until list1.size) {
            val as1 = list1[idx]
            val as2 = list1[idx]
            assertEquals(as1, as2)
        }
    }

    companion object {
        fun generateFakeManifest(context: GroupContext): Manifest {
            //     electionScopeId: String,
            //    specVersion: String,
            //    electionType: Manifest.ElectionType,
            //    startDate: String, // LocalDateTime,
            //    endDate: String, // LocalDateTime,
            //    geopoliticalUnits: List<Manifest.GeopoliticalUnit>,
            //    parties: List<Manifest.Party>,
            //    candidates: List<Manifest.Candidate>,
            //    contests: List<Manifest.ContestDescription>,
            //    ballotStyles: List<Manifest.BallotStyle>,
            //    name: Manifest.InternationalizedText?,
            //    contactInformation: Manifest.ContactInformation?
            return context.manifestOf(
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
            return context.geopoliticalUnitOf(
                "geopoliticalUnitId$cseq",
                "name$cseq",
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
            return context.partyOf(
                "party$cseq",
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
            return context.candidateOf(
                "candidate$cseq",
                generateInternationalizedText(context),
                "party$cseq",
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
            return context.contestDescriptionOf(
                "contest$cseq",
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
            return context.selectionDescriptionOf(
                "selection$sseq",
                sseq,
                "candidate$sseq",
            )
        }

        //         val ballotStyleId: String,
        //        val geopoliticalUnitIds: List<String>,
        //        val partyIds: List<String>,
        //        val imageUri: String?,
        //        val cryptoHash: ElementModQ
        private fun generateBallotStyle(cseq: Int, context: GroupContext): Manifest.BallotStyle {
            return context.ballotStyleOf(
                "ballotStyle$cseq",
                List(3) { "geode$it" },
                List(11) { "paltry$it" },
                "Imagine",
            )
        }

        // val text: List<Language>
        private fun generateInternationalizedText(context: GroupContext): Manifest.InternationalizedText {
            return context.internationalizedTextOf(
                List(3) { generateLanguage(it, context) },
            )
        }

        //         val addressLine: List<String>,
        //        val email: List<AnnotatedString>,
        //        val phone: List<AnnotatedString>,
        //        val name: String?,
        //        val cryptoHash: ElementModQ
        private fun generateContactInformation(context: GroupContext): Manifest.ContactInformation {
            return context.contactInformationOf(
                List(3) { "addressLine$it" },
                List(3) { generateAnnotatedString(it, context) },
                List(3) { generateAnnotatedString(it, context) },
                "name",
            )
        }

        // val value: String, val language: String,
        private fun generateLanguage(seq: Int, context: GroupContext): Manifest.Language {
            return context.languageOf(
                "text$seq",
                "language:$seq:$seq",
            )
        }

        // val annotation: String, val value: String,
        private fun generateAnnotatedString(seq: Int, context: GroupContext): Manifest.AnnotatedString {
            return context.annotatedStringOf(
                "annotate$seq",
                "value:$seq:$seq",
            )
        }
    }
}