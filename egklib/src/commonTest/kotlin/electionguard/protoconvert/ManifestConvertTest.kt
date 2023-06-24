package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.input.buildTestManifest
import electionguard.publish.makePublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManifestConvertTest {

    private val writeout = true
    private val ncontests = 20
    private val nselections = 5

    @Test
    fun writeManifest() {
        val manifest = buildTestManifest(ncontests, nselections)
        val proto = manifest.publishProto()
        val roundtrip = proto.import().getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        assertTrue(roundtrip.equals(manifest))
        assertEquals(roundtrip, manifest)

        if (writeout) {
            val output = "testOut/ManifestConvertTest"
            val publisher = makePublisher(output, true)
            publisher.writeManifest(manifest)
            println("Wrote to $output")
        }
    }

    @Test
    fun roundtripManifest() {
        val manifest = generateFakeManifest()
        val proto = manifest.publishProto()
        val roundtrip = proto.import().getOrThrow { IllegalStateException(it) }
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
            assertEquals(rgpu.contactInformation, gpu.contactInformation)
            assertEquals(rgpu.type, gpu.type)
            assertEquals(rgpu, gpu)
        }
        assertEquals(roundtrip.geopoliticalUnits, manifest.geopoliticalUnits)
        assertEquals(roundtrip, manifest)
    }

    companion object {
        private const val ncontests = 20
        private const val nselections = 5

        fun generateFakeManifest(): Manifest {
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
            return Manifest(
                "electionScopeId",
                "specVersion",
                Manifest.ElectionType.primary,
                "2022-02-22T00:00:00",
                "2022-02-23T23:59:57",
                List(11) { generateGeopoliticalUnit(it) },
                List(3) { generateParty(it) },
                List(ncontests * nselections) { generateCandidate(it) },
                List(ncontests) { generateContest(it) },
                List(8) { generateBallotStyle(it) },
                generateInternationalizedText(),
                generateContactInformation(),
            )
        }

        //        val geopoliticalUnitId: String,
        //        val name: String,
        //        val type: ReportingUnitType,
        //        val contactInformation: ContactInformation?,
        //        val cryptoHash: ElementModQ
        private fun generateGeopoliticalUnit(cseq: Int): Manifest.GeopoliticalUnit {
            return Manifest.GeopoliticalUnit(
                "geode$cseq",
                "name$cseq",
                Manifest.ReportingUnitType.city,
                "contact",
            )
        }

        //         val partyId: String,
        //        val name: InternationalizedText?,
        //        val abbreviation: String?,
        //        val color: String?,
        //        val logoUri: String?,
        //        val cryptoHash: ElementModQ
        private fun generateParty(cseq: Int): Manifest.Party {
            return Manifest.Party(
                "party$cseq",
                "name",
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
        private fun generateCandidate(cseq: Int): Manifest.Candidate {
            val partyId = cseq % 3
            return Manifest.Candidate(
                "candidate$cseq",
                "name",
                "party$partyId",
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
        private fun generateContest(cseq: Int): Manifest.ContestDescription {
            return Manifest.ContestDescription(
                "contest$cseq",
                cseq,
                "geode0",
                Manifest.VoteVariationType.n_of_m,
                1,
                1,
                "contest name",
                List(nselections) { generateSelection(nselections * cseq + it) },
                "ballot",
                "subballot",
            )
        }

        //         val selectionId: String,
        //        val sequenceOrder: Int,
        //        val candidateId: String,
        //        val cryptoHash: ElementModQ
        private fun generateSelection(sseq: Int): Manifest.SelectionDescription {
            return Manifest.SelectionDescription("selection$sseq", sseq, "candidate$sseq")
        }

        //        val ballotStyleId: String,
        //        val geopoliticalUnitIds: List<String>,
        //        val partyIds: List<String>,
        //        val imageUri: String?,
        //        val cryptoHash: ElementModQ
        private fun generateBallotStyle(cseq: Int): Manifest.BallotStyle {
            return Manifest.BallotStyle(
                "ballotStyle$cseq",
                List(3) { "geode$it" },
                List(11) { "paltry$it" },
                "Imagine",
            )
        }

        // val text: List<Language>
        private fun generateInternationalizedText(): List<Manifest.Language> {
            return List(3) { generateLanguage(it) }
        }

        //         val addressLine: List<String>,
        //        val email: List<AnnotatedString>,
        //        val phone: List<AnnotatedString>,
        //        val name: String?,
        //        val cryptoHash: ElementModQ
        private fun generateContactInformation(): Manifest.ContactInformation {
            return Manifest.ContactInformation(
                "name",
                List(3) { "addressLine$it" },
                "email",
                "phone",
            )
        }

        // val value: String, val language: String,
        private fun generateLanguage(seq: Int): Manifest.Language {
            return Manifest.Language("text$seq", "language:$seq:$seq")
        }
    }
}