package electionguard.verifier

import electionguard.ballot.EncryptedBallot
import electionguard.util.Stats
import electionguard.core.productionGroup
import electionguard.publish.readElectionRecord
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class DuplicateTrackingCodesTest {
    private val inputDir = "src/commonTest/data/workflow/allAvailableJson"

    @Test
    fun duplicateTrackingCodes() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputDir)
        val mungedBallots = mutableListOf<EncryptedBallot>()

        var count = 0
        for (ballot in electionRecord.encryptedAllBallots { true }) {
            // println(" munged ballot ${ballot.ballotId}")
            mungedBallots.add(ballot)
            if (count % 3 == 0) {
                mungedBallots.add(ballot)
            }
            if (count > 10) {
                break
            }
            count++
        }

        // verify duplicate ballots fail
        val verifier = Verifier(electionRecord)
        val results = verifier.verifyEncryptedBallots(mungedBallots, Stats())
        println(results)
        assertTrue(results.hasErrors())
    }
}