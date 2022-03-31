package electionguard.publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext

expect class Consumer(topDir: String, groupContext: GroupContext) {
    fun readElectionRecordAllData(): ElectionRecordAllData
    fun readElectionRecord(): ElectionRecord
    fun iteratePlaintextBallots(ballotDir : String): Iterable<PlaintextBallot>
    fun iterateSubmittedBallots(): Iterable<SubmittedBallot>
    fun iterateCastBallots(): Iterable<SubmittedBallot>
    fun iterateSpoiledBallots(): Iterable<SubmittedBallot>
    fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally>
}