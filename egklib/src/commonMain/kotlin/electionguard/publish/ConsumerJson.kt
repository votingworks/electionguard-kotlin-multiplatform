package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

expect class ConsumerJson (topDir: String, group: GroupContext) : Consumer {
    override fun topdir() : String
    override fun isJson() : Boolean

    override fun readManifestBytes(filename : String): ByteArray
    override fun makeManifest(manifestBytes: ByteArray): Manifest

    override fun readElectionConfig(): Result<ElectionConfig, String>
    override fun readElectionInitialized(): Result<ElectionInitialized, String>
    override fun readTallyResult(): Result<TallyResult, String>
    override fun readDecryptionResult(): Result<DecryptionResult, String>

    override fun encryptingDevices(): List<String>
    override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, String>
    override fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    override fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>

    override fun hasEncryptedBallots() : Boolean
    override fun iterateEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot>
    override fun iterateCastBallots(): Iterable<EncryptedBallot>
    override fun iterateSpoiledBallots(): Iterable<EncryptedBallot>
    override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot>

    override fun iteratePlaintextBallots(ballotDir: String, filter : ((PlaintextBallot) -> Boolean)? ): Iterable<PlaintextBallot>
    override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF
}