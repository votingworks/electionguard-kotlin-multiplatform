package electionguard.publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext

actual class Consumer actual constructor(
    electionRecordDir: String,
    groupContext: GroupContext
) {
    actual fun readElectionRecordAllData(): ElectionRecordAllData {
        TODO("Not yet implemented")
    }

    actual fun readElectionRecordProto(): ElectionRecord? {
        TODO("Not yet implemented")
    }

    actual fun iterateSubmittedBallots(): Iterable<SubmittedBallot> {
        TODO("Not yet implemented")
    }

    actual fun iterateCastBallots(): Iterable<SubmittedBallot> {
        TODO("Not yet implemented")
    }

    actual fun iterateSpoiledBallots(): Iterable<SubmittedBallot> {
        TODO("Not yet implemented")
    }

    actual fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally> {
        TODO("Not yet implemented")
    }

}