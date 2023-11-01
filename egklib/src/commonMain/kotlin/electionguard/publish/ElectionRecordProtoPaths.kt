package electionguard.publish

data class ElectionRecordProtoPaths(val topDir : String) {
    private val electionRecordDir = topDir

    companion object {
        const val PROTO_SUFFIX = ".protobuf"
        const val DECRYPTING_TRUSTEE_PREFIX = "decryptingTrustee"
        const val MANIFEST_FILE = "manifest$PROTO_SUFFIX"
        const val ELECTION_CONFIG_FILE = "electionConfig$PROTO_SUFFIX"
        const val ELECTION_INITIALIZED_FILE = "electionInitialized$PROTO_SUFFIX"
        const val TALLY_RESULT_FILE = "tallyResult$PROTO_SUFFIX"
        const val DECRYPTION_RESULT_FILE = "decryptionResult$PROTO_SUFFIX"
        const val PLAINTEXT_BALLOT_FILE = "plaintextBallots$PROTO_SUFFIX"

        const val ENCRYPTED_DIR = "encrypted_ballots"
        const val ENCRYPTED_BALLOT_PREFIX = "eballot-"
        const val ENCRYPTED_CHAIN_FILE = "ballotChain$PROTO_SUFFIX"
        const val ENCRYPTED_BATCH_FILE = "encryptedBallots$PROTO_SUFFIX"

        const val DECRYPTED_DIR = "decrypted_ballots"
        const val DECRYPTED_BALLOT_PREFIX = "dballot-"
        const val DECRYPTED_BATCH_FILE = "challengedBallots$PROTO_SUFFIX"
    }

    fun manifestPath(): String {
        return "$electionRecordDir/$MANIFEST_FILE"
    }

    fun electionConfigPath(): String {
        return "$electionRecordDir/$ELECTION_CONFIG_FILE"
    }

    fun electionInitializedPath(): String {
        return "$electionRecordDir/$ELECTION_INITIALIZED_FILE"
    }

    fun tallyResultPath(): String {
        return "$electionRecordDir/$TALLY_RESULT_FILE"
    }

    fun decryptionResultPath(): String {
        return "$electionRecordDir/$DECRYPTION_RESULT_FILE"
    }

    // TODO resolve ??
    fun plaintextBallotPath(ballotDir: String): String {
        return "$ballotDir/$PLAINTEXT_BALLOT_FILE"
    }

    fun encryptedBallotPath(): String {
        return "$electionRecordDir/$ENCRYPTED_BATCH_FILE"
    }

    fun decryptedBatchPath(): String {
        return "$electionRecordDir/$DECRYPTED_BATCH_FILE"
    }

    fun decryptedBallotDir(): String {
        return "$electionRecordDir/$DECRYPTED_DIR"
    }

    // TODO resolve ??
    fun decryptingTrusteePath(trusteeDir: String, guardianId: String): String {
        return "$trusteeDir/$DECRYPTING_TRUSTEE_PREFIX-$guardianId$PROTO_SUFFIX"
    }

    fun encryptedBallotDir(): String {
        return "$electionRecordDir/$ENCRYPTED_DIR/"
    }

    fun encryptedBallotDir(device: String): String {
        val useDevice = device.replace(" ", "_")
        return "$electionRecordDir/$ENCRYPTED_DIR/$useDevice/"
    }

    fun encryptedBallotPath(device: String, ballotId: String): String {
        val useDevice = device.replace(" ", "_")
        val id = ballotId.replace(" ", "_")
        return "$electionRecordDir/$ENCRYPTED_DIR/$useDevice/${ENCRYPTED_BALLOT_PREFIX}$id${PROTO_SUFFIX}"
    }

    fun encryptedBallotBatched(device: String): String {
        return "${encryptedBallotDir(device)}/$ENCRYPTED_BATCH_FILE"
    }

    fun encryptedBallotChain(device: String): String {
        return "${encryptedBallotDir(device)}/$ENCRYPTED_CHAIN_FILE"
    }
}