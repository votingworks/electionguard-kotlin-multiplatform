package electionguard.verifier

import com.github.michaelbull.result.Err
import electionguard.ballot.EncryptedBallot
import electionguard.core.Stats
import electionguard.core.productionGroup
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertTrue

class DuplicateTrackingCodesTest {
    private val inputDir = "src/commonTest/data/workflow/allAvailableProto"

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

        println("verify duplicate ballots fail")
        val verifier = Verifier(electionRecord)
        val results = verifier.verifyEncryptedBallots(mungedBallots, Stats())
        assertTrue(results is Err)
    }
}