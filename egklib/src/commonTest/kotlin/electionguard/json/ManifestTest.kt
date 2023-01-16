package electionguard.json

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.input.ManifestBuilder
import electionguard.input.buildStandardManifest
import electionguard.protoconvert.ManifestConvertTest.Companion.generateFakeManifest
import electionguard.protoconvert.import
import electionguard.protoconvert.publishProto
import electionguard.publish.makePublisher
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlin.test.*

class ManifestTest {

    private val writeout = true
    private val ncontests = 20
    private val nselections = 5

    @Test
    fun writeManifest() {
        val manifest = buildStandardManifest(ncontests, nselections)
        val json = manifest.publish()
        val roundtrip = json.import()
        assertNotNull(roundtrip)
        assertTrue(roundtrip.equals(manifest))
        assertEquals(roundtrip, manifest)

        if (writeout) {
            val output = "testOut/ManifestConvertTest"
            val publisher = makePublisher(output, true, true)
            publisher.writeManifest(manifest)
            println("Wrote to $output")
        }
    }

    @Test
    fun testManifestInputBuilderRoundtrip() {
        runTest {
            val ebuilder = ManifestBuilder("ManifestTest")
            val manifest: Manifest = ebuilder.addContest("onlyContest")
                .addSelection("selection1", "candidate1")
                .addSelection("selection2", "candidate2")
                .done()
                .build()
            assertEquals(manifest, manifest.publish().import())
            assertEquals(manifest, jsonRoundTrip(manifest.publish()).import())
        }
    }

    @Test
    fun testGenerateFakeManifestRoundtrip() {
        runTest {
            val manifest = generateFakeManifest()
            assertEquals(manifest, manifest.publish().import())
            assertEquals(manifest, jsonRoundTrip(manifest.publish()).import())
        }
    }

    @Test
    fun testBallotStyle() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 10),
            ) { id, n,  ->
                val obj = makeBallotStyle(id, n)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testCandidate() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
            ) { id  ->
                val obj = makeCandidate(id)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testContact() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 10),
            ) { id, n,  ->
                val obj = makeContact(id, n)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testGpu() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
                Arb.string(minSize = 7),
                Arb.int(min = 0, max = Manifest.ReportingUnitType.values().size-1),
            ) { id, name, type,  ->
                val obj = makeGPU(id, name, type)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testLanguage() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
                Arb.string(minSize = 7),
            ) { value, language,  ->
                val obj = makeLanguage(value, language)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testParty() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
                Arb.string(minSize = 7),
            ) { id, name,  ->
                val obj = makeParty(id, name)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testSelection() {
        runTest {
            checkAll(
                iterations = 111,
                Arb.string(minSize = 3),
                Arb.int(),
                Arb.string(minSize = 7),
            ) { id, seq, name,  ->
                val obj = makeSelection(id, seq, name)
                assertEquals(obj, obj.publish().import())
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testContest() {
        runTest {
            checkAll(
                iterations = 3,
                Arb.string(minSize = 11),
                Arb.int(),
                Arb.int(min = 0, max = Manifest.VoteVariationType.values().size-1),
                Arb.int(min = 1, max = 11),
            ) { id, seq, type, nsel,  ->
                val obj = makeContest(id, seq, type, nsel)
                val rt = obj.publish().import()
                assertEquals(obj, rt)
                assertEquals(obj.cryptoHash, rt.cryptoHash)
                println("${obj.cryptoHash}, ${rt.cryptoHash}")
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())
            }
        }
    }

    @Test
    fun testManifest() {
        runTest {
            checkAll(
                iterations = 81,
                Arb.string(minSize = 3),
                Arb.int(min = 0, max = Manifest.ElectionType.values().size-1),
                Arb.int(min = 1, max = 3),
                Arb.int(min = 1, max = 4),
                Arb.int(min = 1, max = 5),
                Arb.int(min = 1, max = 6),
                Arb.int(min = 1, max = 5),
                Arb.int(min = 1, max = 3),
            ) { id, type, ngpu, np, ncand, nc, nbs, nlang,  ->
                val obj = makeManifest(id, type, ngpu, np, ncand, nc, nbs, nlang)
                val rt = obj.publish().import()
                compareManifestHash(obj, rt)
                assertEquals(obj, jsonRoundTrip(obj.publish()).import())

                val proto : electionguard.protogen.Manifest = obj.publishProto()
                val rtProto = proto.import().getOrThrow { IllegalStateException(it) }
                compareManifestHash(obj, rtProto)
                assertEquals(obj, rtProto)

                assertEquals(rt, rtProto)
            }
        }
    }
}

fun compareManifestHash(org: Manifest, roundtrip:Manifest) {
    assertNotNull(roundtrip)
    assertEquals(org.electionScopeId, roundtrip.electionScopeId)
    assertEquals(org.specVersion, roundtrip.specVersion)
    assertEquals(org.electionType, roundtrip.electionType)
    assertEquals(org.startDate, roundtrip.startDate)
    assertEquals(org.endDate, roundtrip.endDate)

    assertEquals(org.geopoliticalUnits, roundtrip.geopoliticalUnits)
    assertEquals(org.parties, roundtrip.parties)
    assertEquals(org.candidates, roundtrip.candidates)
    assertEquals(org.ballotStyles, roundtrip.ballotStyles)
    assertEquals(org.contests, roundtrip.contests)
    assertEquals(org.contactInformation, roundtrip.contactInformation)
    assertEquals(org.name, roundtrip.name)
    assertEquals(org.cryptoHash, roundtrip.cryptoHash)

    assertEquals(org, org)
}

fun makeBallotStyle(id: String, ngpu : Int) = Manifest.BallotStyle(
        id,
        if (Random.nextBoolean()) List(ngpu) {"gpu$it"} else emptyList(),
        if (Random.nextBoolean()) listOf("party1", "party2") else emptyList(),
        if (Random.nextBoolean()) "image" else null,
    )

fun makeCandidate(id: String) = Manifest.Candidate(
        id,
        if (Random.nextBoolean()) "name" else null,
        if (Random.nextBoolean()) "party" else null,
        if (Random.nextBoolean()) "image" else null,
        Random.nextBoolean(),
    )

fun makeContact(id: String, nlines: Int) = Manifest.ContactInformation(
        id,
        if (Random.nextBoolean()) List(nlines) {"line$it"} else emptyList(),
        if (Random.nextBoolean()) "email" else null,
        if (Random.nextBoolean()) "phone" else null,
    )

fun makeGPU(id: String, name: String, type : Int) = Manifest.GeopoliticalUnit(
        id,
        name,
        Manifest.ReportingUnitType.values()[type],
        if (Random.nextBoolean()) "contact" else null,
    )

fun makeLanguage(value: String, language: String) = Manifest.Language(value, language)

fun makeParty(id: String, name: String) = Manifest.Party(
        id,
        name,
        if (Random.nextBoolean()) "abbrev" else null,
        if (Random.nextBoolean()) "color" else null,
        if (Random.nextBoolean()) "logo" else null,
    )

fun makeSelection(id: String, seq: Int, candidate: String) = Manifest.SelectionDescription(
    id,
    seq,
    candidate,
)

fun makeContest(id: String, seq : Int, type: Int, nsel : Int) = Manifest.ContestDescription(
        id,
        seq,
        "gpu",
        Manifest.VoteVariationType.values()[type],
        99,
        42,
        "name",
        List(nsel) {makeSelection("id$it", it, "candidate$it")},
        if (Random.nextBoolean()) "title" else null,
        if (Random.nextBoolean()) "subtitle" else null,
    )

fun makeManifest(id: String, type: Int, ngpu : Int, np: Int, ncand : Int, nc : Int, nstyles: Int, nlang: Int) = Manifest(
    id,
    "version",
    Manifest.ElectionType.values()[type],
    "Start",
    "End",
    List(ngpu) { makeGPU("id$it","name$it", 1) },
    List(np) { makeParty("id$it", "name$it") },
    List(ncand) { makeCandidate("candidate$it") },
    List(nc) { makeContest("id$it", it,  1, 4) },
    List(nstyles) { makeBallotStyle("bs$it", it) },
    List(nlang) { makeLanguage("value$it", "lang$it") },
    if (Random.nextBoolean()) makeContact("contact", 3) else null,
)