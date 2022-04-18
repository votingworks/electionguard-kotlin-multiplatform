package electionguard.input

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
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
            println("Problems=%n$problems")
        }
        return !problems.hasErrors()
    }

    @Test
    fun testDefaultOkValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id").addSelection("selection_id", 1).done().build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, null)
    }

    @Test
    fun testStylingNotExistValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id").setDefaultStyle("badHairDay")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id").addSelection("selection_id", 1).done().build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.1")
    }

    fun testGpunitListed(listed: Boolean): ElectionAndBallot {
        val ebuilder = ManifestInputBuilder("ballot_id").addStyle("styling", "orphan", "annie")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .setGpunit(if (listed) "orphan" else "parented")
                .done()
                .build()
        assertEquals(validateManifest(election), listed)
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id").addSelection("selection_id", 1).done().build()
        return ElectionAndBallot(election, ballot)
    }

    @Test
    fun testGpunitListedValidate() {
        testValidate(testGpunitListed(true), null)
    }

    @Test
    fun testGpunitNotListedValidate() {
        testValidate(testGpunitListed(false), "Ballot.A.3")
    }

    @Test
    fun testInvalidContestValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_bad") // invalid contest id
                .addSelection("selection_id", 1)
                .addSelection("selection_id2", 0)
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.A.2")
    }

    @Test
    fun testInvalidSelectionValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id")
                .addSelection("selection_id", 0)
                .addSelection("selection_bad", 1) // invalid selection id
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.2.1")
    }

    @Test
    fun testZeroOrOneValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id")
                .addSelection("selection_id", 0)
                .addSelection("selection_id2", 2)
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.C.1")
    }

    @Test
    fun testOvervoteValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id")
                .addSelection("selection_id", 1)
                .addSelection("selection_id2", 1)
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.C.2")
    }

    @Test
    fun testContestDeclaredTwiceValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id")
                .addSelection("selection_id", 1)
                .done()
                .addContest("contest_id")
                .addSelection("selection_id", 1)
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.1")
    }

    @Test
    fun testSelectionDeclaredTwiceValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id")
                .addSelection("selection_id", 1)
                .addSelection("selection_id", 1) // voting for same candidate twice
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.2")
    }

    @Test
    fun testSelectionNoMatchValidate() {
        val ebuilder = ManifestInputBuilder("ballot_id")
        val election: Manifest =
            ebuilder.addContest("contest_id")
                .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
                .addSelection("selection_id", "candidate_1")
                .addSelection("selection_id2", "candidate_2")
                .done()
                .build()
        assertTrue(validateManifest(election))
        val builder = BallotInputBuilder("ballot_id")
        val ballot: PlaintextBallot =
            builder.addContest("contest_id")
                .addSelection("selection_id", 1)
                .addSelection("selection_id3", 0) // voting for same candidate twice
                .done()
                .build()
        val eandb = ElectionAndBallot(election, ballot)

        testValidate(eandb, "Ballot.B.2.1")
    }

    fun testValidate(eanb: ElectionAndBallot, expectMessage: String?) {
        val validator = BallotInputValidation(eanb.election)
        val problems = validator.validate(eanb.ballot)
        if (problems.hasErrors()) {
            println("Problems=%n$problems")
        }
        if (expectMessage != null) {
            assertTrue(problems.hasErrors())
            assertContains(problems.toString(), expectMessage)
        } else {
            assertFalse(problems.hasErrors())
        }
    }
}
