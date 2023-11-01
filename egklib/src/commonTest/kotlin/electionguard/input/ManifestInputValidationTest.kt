package electionguard.input

import electionguard.ballot.Manifest
import electionguard.cli.ManifestBuilder
import electionguard.util.ErrorMessages
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tester for [ManifestInputValidation]  */
class TestManifestInputValidation {

    @Test
    fun testDefaults() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertFalse(problems.hasErrors())
    }

    @Test
    fun testBallotStyleBadGpUnit() {
        val bs: Manifest.BallotStyle = Manifest.BallotStyle("bad", listOf("badGP"), emptyList(), null)
        val ebuilder = ManifestBuilder("election_scope_id")
            .addBallotStyle(bs)
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.A.1")
        assertContains(problems.toString(), "Manifest.A.5")
    }

    @Test
    fun testBadParty() {
        val ebuilder = ManifestBuilder("election_scope_id")
            .addCandidateAndParty("candide", "wayne")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.A.2")
    }

    @Test
    fun testContestGpunit() {
        val ebuilder = ManifestBuilder("election_scope_id")
            .addGpunit("district9")
        val election: Manifest = ebuilder.addContest("contest_id")
            .setGpunit("district1")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.A.3")
        assertContains(problems.toString(), "Manifest.A.5")
    }

    @Test
    fun testBadCandidateId() {
        val ebuilder = ManifestBuilder("election_scope_id")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "manchurian")
            .done()
            .removeCandidate("manchurian")
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.A.4")
    }

    @Test
    fun testDuplicateContestId() {
        val ebuilder = ManifestBuilder("election_scope_id")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.B.1")
        assertContains(problems.toString(), "Manifest.B.6")
    }

    @Test
    fun testBadSequence() {
        val ebuilder = ManifestBuilder("election_scope_id")
        val election: Manifest = ebuilder
            .addContest("contest_id", 42)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "manchurian")
            .done()
            .addContest("contest_id2", 42)
            .addSelection("selection_id3", "candidate_3", 6)
            .addSelection("selection_id4", "mongolian", 6)
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.B.2")
        assertContains(problems.toString(), "Manifest.B.4")
    }

    @Test
    fun testDuplicateSelectionId() {
        val ebuilder = ManifestBuilder()
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.B.3")
    }

    @Test
    fun testDuplicateCandidateIdGlobal() {
        val ebuilder = ManifestBuilder("election_scope_id")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id1", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .addContest("contest_id2")
            .addSelection("selection_id1", "candidate_1")
            .addSelection("selection_id3", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.B.6")
    }

    @Test
    fun testDuplicateCandidateId() {
        val ebuilder = ManifestBuilder("election_scope_id")
        val election: Manifest = ebuilder.addContest("contest_id")
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_1")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.B.5")
    }

    @Test
    fun testUnusedVoteType() {
        val ebuilder = ManifestBuilder("test_manifest")
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.proportional, 1)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.C.1")
    }

    @Test
    fun testOneVoteAllowed() {
        val ebuilder = ManifestBuilder("test_manifest")
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.one_of_m, 3)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.C.2")
    }

    @Test
    fun testVotesAllowed() {
        val ebuilder = ManifestBuilder("test_manifest")
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 3)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.C.3")
    }

    @Test
    fun testVotesApproval() {
        val ebuilder = ManifestBuilder("test_manifest")
        val election: Manifest = ebuilder.addContest("contest_id")
            .setVoteVariationType(Manifest.VoteVariationType.approval, 7)
            .addSelection("selection_id", "candidate_1")
            .addSelection("selection_id2", "candidate_2")
            .done()
            .build()
        val validator = ManifestInputValidation(election)
        val problems : ErrorMessages = validator.validate()
        println(problems)
        assertTrue(problems.hasErrors())
        assertContains(problems.toString(), "Manifest.C.4")
    }

}