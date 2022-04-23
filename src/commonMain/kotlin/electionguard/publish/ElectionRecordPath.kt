package electionguard.publish

data class ElectionRecordPath(val topDir : String) {
    private val electionRecordDir = "$topDir/$ELECTION_RECORD_DIR"

    companion object {
        const val PROTO_VERSION = "2.0.0"
        const val ELECTION_RECORD_DIR = "election_record"

        const val PROTO_SUFFIX = ".protobuf"
        const val ELECTION_CONFIG_FILE_NAME = "electionConfig" + PROTO_SUFFIX
        const val ELECTION_INITIALIZED_FILE_NAME = "electionInitialized" + PROTO_SUFFIX
        const val TALLY_RESULT_NAME = "tallyResult" + PROTO_SUFFIX
        const val DECRYPTION_RESULT_NAME = "decryptionResult" + PROTO_SUFFIX
        const val INVALID_BALLOT_PROTO = "invalidBallots" + PROTO_SUFFIX
        const val PLAINTEXT_BALLOT_PROTO = "plaintextBallots" + PROTO_SUFFIX
        const val SUBMITTED_BALLOT_PROTO = "encryptedBallots" + PROTO_SUFFIX
        const val SPOILED_BALLOT_FILE = "spoiledBallotsTally" + PROTO_SUFFIX
    }

    fun electionConfigPath(): String {
        return "$electionRecordDir/$ELECTION_CONFIG_FILE_NAME"
    }

    fun electionInitializedPath(): String {
        return "$electionRecordDir/$ELECTION_INITIALIZED_FILE_NAME"
    }

    fun tallyResultPath(): String {
        return "$electionRecordDir/$TALLY_RESULT_NAME"
    }

    fun decryptionResultPath(): String {
        return "$electionRecordDir/$DECRYPTION_RESULT_NAME"
    }

    fun plaintextBallotProtoPath(ballotDir : String): String {
        return "$ballotDir/$PLAINTEXT_BALLOT_PROTO"
    }

    fun submittedBallotProtoPath(): String {
        return "$electionRecordDir/$SUBMITTED_BALLOT_PROTO"
    }

    fun spoiledBallotProtoPath(): String {
        return "$electionRecordDir/$SPOILED_BALLOT_FILE"
    }

    fun invalidBallotProtoPath(invalidDir : String): String {
        return "$invalidDir/$INVALID_BALLOT_PROTO"
    }
}