package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.util.ErrorMessages

expect class ConsumerJsonR (topDir: String, group: GroupContext) : Consumer {
    override fun topdir() : String
    override fun isJson() : Boolean

    override fun readManifestBytes(filename : String): ByteArray
    override fun makeManifest(manifestBytes: ByteArray): Manifest

    override fun readElectionConfig(): Result<ElectionConfig, ErrorMessages>
    override fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages>
    override fun readTallyResult(): Result<TallyResult, ErrorMessages>
    override fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages>

    override fun hasEncryptedBallots() : Boolean
    override fun encryptingDevices(): List<String>
    override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages>
    override fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, ErrorMessages>
    override fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    override fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>

    override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot>

    override fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    override fun readTrustee(trusteeDir: String, guardianId: String): Result<DecryptingTrusteeIF, ErrorMessages>
}