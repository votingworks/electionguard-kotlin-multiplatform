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
        const val ENCRYPTED_BALLOT_FILE = "encryptedBallots$PROTO_SUFFIX"
        const val SPOILED_BALLOT_FILE = "spoiledBallotTallies$PROTO_SUFFIX"
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

    fun plaintextBallotPath(ballotDir: String): String {
        return "$ballotDir/$PLAINTEXT_BALLOT_FILE"
    }

    fun encryptedBallotPath(): String {
        return "$electionRecordDir/$ENCRYPTED_BALLOT_FILE"
    }

    fun spoiledBallotPath(): String {
        return "$electionRecordDir/$SPOILED_BALLOT_FILE"
    }

    fun decryptingTrusteePath(trusteeDir: String, guardianId: String): String {
        return "$trusteeDir/$DECRYPTING_TRUSTEE_PREFIX-$guardianId$PROTO_SUFFIX"
    }
}