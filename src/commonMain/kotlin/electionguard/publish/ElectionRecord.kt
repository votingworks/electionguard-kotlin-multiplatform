package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

expect class ElectionRecord(
    topDir: String,
    groupContext: GroupContext,
) {
    fun topdir() : String

    fun readElectionConfig(): Result<ElectionConfig, String>
    fun readElectionInitialized(): Result<ElectionInitialized, String>
    fun readTallyResult(): Result<TallyResult, String>
    fun readDecryptionResult(): Result<DecryptionResult, String>

    // Use iterators, so that we never have to read in all objects at once.
    fun iterateSubmittedBallots(filter : (EncryptedBallot) -> Boolean): Iterable<EncryptedBallot>
    fun iterateCastBallots(): Iterable<EncryptedBallot>
    fun iterateSpoiledBallots(): Iterable<EncryptedBallot>
    fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally>

    // not part of the election record, private data
    fun iteratePlaintextBallots(ballotDir: String, filter : (PlaintextBallot) -> Boolean): Iterable<PlaintextBallot>
    fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF

}