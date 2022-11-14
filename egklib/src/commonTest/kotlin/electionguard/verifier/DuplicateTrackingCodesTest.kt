package electionguard.verifier

import com.github.michaelbull.result.Err
import electionguard.ballot.EncryptedBallot
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.electionRecordFromConsumer
import kotlin.test.Test
import kotlin.test.assertTrue

class DuplicateTrackingCodesTest {
    private val inputDir = "src/commonTest/data/runWorkflowAllAvailable"

    @Test
    fun duplicateTrackingCodes() {
        val context = productionGroup()
        val electionRecord = electionRecordFromConsumer(Consumer(inputDir, context))
        val mungedBallots = mutableListOf<EncryptedBallot>()

        var count = 0
        for (ballot in electionRecord.encryptedBallots { true }) {
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

        println("verify duplicate ballots fail")
        val verifier = Verifier(electionRecord)
        val results = verifier.verifyEncryptedBallots(mungedBallots, Stats())
        assertTrue(results is Err)
    }
}