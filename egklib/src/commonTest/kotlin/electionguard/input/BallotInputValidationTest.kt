package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.cli.ManifestBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestBallotInputValidation {
    class ElectionAndBallot(election: Manifest, ballot: PlaintextBallot) {
        val election: Manifest
        val ballot: PlaintextBallot

        init {
            this.election = election
            this.ballot = ballot
        }
    }

    private fun validateManifest(election: Manifest): Boolean {
        val validator = ManifestInputValidation(election)
        val problems = validator.validate()
        if (problems.hasErrors()) {
            println("$problems")
        }
        return !problems.hasErrors()
    }

    @Test
    fun testDefaultOkValidate() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 1, 0)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, null)
    }

    @Test
    fun testStylingNotExistValidate() {
        val ebuilder = ManifestBuilder()
            .setDefaultStyle("badHairDay")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id")
            .addSelection("selection_id", 1)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.1")
    }

    fun testGpunitListed(listed: Boolean): ElectionAndBallot {
        val ebuilder = ManifestBuilder()
            .addStyle("ballotStyle", "orphan", "annie")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .setGpunit(if (listed) "orphan" else "district9")
            .done()
            .build()
        assertEquals(validateManifest(election), listed)
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 1, 0)
            .done()
            .build()
        return ElectionAndBallot(election, ballot)
    }

    @Test
    fun testGpunitListedValidate() {
        testValidate(testGpunitListed(true), null)
    }

    @Test
    fun testGpunitNotListed() {
        testValidate(
            testGpunitListed(false),
            "Ballot.A.3 Contest's geopoliticalUnitId 'district9' not listed in BallotStyle 'ballotStyle' geopoliticalUnitIds"
        )
    }

    @Test
    fun testInvalidContestId() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_bad") // invalid contest id
            .addSelection("selection_id", 1)
            .addSelection("selection_id2", 0)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.2")
    }

    @Test
    fun testInvalidContestSeq() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id") // invalid contest id
            .addSelection("selection_id", 1)
            .addSelection("selection_id2", 0)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.2.1")
    }

    @Test
    fun testInvalidSelectionId() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 0, 0)
            .addSelection("selection_bad", 1, 1) // invalid selection id
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.4")
    }

    @Test
    fun testInvalidSelectionSeq() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 0, 0)
            .addSelection("selection_id2", 1, 2) // invalid selection id
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.4.1")
    }

    @Test
    fun testZeroOrOneValidate() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 0, 0)
            .addSelection("selection_id2", 2, 1)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, null)
    }

    @Test
    fun testOvervoteValidate() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 1, 0)
            .addSelection("selection_id2", 1, 1)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, null) // overvotes ok
    }

    @Test
    fun testContestDeclaredTwiceValidate() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id", 0)
            .addSelection("selection_id", 1, 0)
            .done()
            .addContest("contest_id", 1)
            .addSelection("selection_id", 1, 1)
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.1 Multiple Ballot contests have same contest id")
    }

    @Test
    fun testSelectionDeclaredTwiceValidate() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id")
            .addSelection("selection_id", 1)
            .addSelection("selection_id", 1) // voting for same candidate twice
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.2 Multiple Ballot selections have duplicate id 'selection_id'")
    }

    @Test
    fun testSelectionDuplicateSequence() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id")
            .addSelection("selection_id", 1, 1)
            .addSelection("selection_id2", 0, 1) // voting for same candidate twice
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.3 Multiple Ballot selections have duplicate sequenceOrder '1'")
    }

    @Test
    fun testSelectionNoMatchValidate() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder(election, "ballot_id")
        val ballot: PlaintextBallot = builder.addContest("contest_id")
            .addSelection("selection_id", 1)
            .addSelection("selection_id3", 0) // voting for same candidate twice
            .done()
            .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.4 Ballot Selection 'selection_id3' does not exist in contest manifest")
    }

    fun testValidate(eanb: ElectionAndBallot, expectMessage: String?) {
        val validator = BallotInputValidation(eanb.election)
        val problems = validator.validate(eanb.ballot)
        if (problems.hasErrors()) {
            println(problems)
        }
        if (expectMessage != null) {
            assertTrue(problems.hasErrors())
            assertContains(problems.toString(), expectMessage)
        } else {
            println(problems)
            assertFalse(problems.hasErrors())
        }
    }
}
