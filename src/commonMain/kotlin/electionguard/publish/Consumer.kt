package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

/** public API to read from the election record */
expect class Consumer(
    topDir: String,
    groupContext: GroupContext,
) {
    fun topdir() : String
    fun hasEncryptedBallots() : Boolean

    fun readElectionConfig(): Result<ElectionConfig, String>
    fun readElectionInitialized(): Result<ElectionInitialized, String>
    fun readTallyResult(): Result<TallyResult, String>
    fun readDecryptionResult(): Result<DecryptionResult, String>

    //// Use iterators, so that we never have to read in all objects at once.
    // all ballots in the ENCRYPTED_BALLOT_FILE, with filter
    fun iterateEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    fun iterateCastBallots(): Iterable<EncryptedBallot>  // state = CAST
    fun iterateSpoiledBallots(): Iterable<EncryptedBallot> // state = Spoiled
    // all tallies in the SPOILED_BALLOT_FILE
    fun iterateSpoiledBallotTallies(): Iterable<DecryptedTallyOrBallot>

    //// not part of the election record, private data
    // plaintext ballots in given directory, with filter
    fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    // trustee in given directory for given guardianId
    fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF
}