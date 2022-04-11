package electionguard.publish

data class ElectionRecordPath(val topDir : String) {
    private val electionRecordDir = "$topDir/$ELECTION_RECORD_DIR"

    companion object {
        const val PROTO_VERSION = "2.0.0"
        const val ELECTION_RECORD_DIR = "election_record"

        const val PROTO_SUFFIX = ".protobuf"
        const val ELECTION_RECORD_FILE_NAME = "electionRecord" + PROTO_SUFFIX
        const val GUARDIANS_FILE = "guardians" + PROTO_SUFFIX
        const val INVALID_BALLOT_PROTO = "invalidBallots" + PROTO_SUFFIX
        const val PLAINTEXT_BALLOT_PROTO = "plaintextBallots" + PROTO_SUFFIX
        const val SUBMITTED_BALLOT_PROTO = "submittedBallots" + PROTO_SUFFIX
        const val SPOILED_BALLOT_FILE = "spoiledBallotsTally" + PROTO_SUFFIX
        const val TRUSTEES_FILE = "trustees" + PROTO_SUFFIX
    }

    fun electionRecordProtoPath(): String {
        return "$electionRecordDir/$ELECTION_RECORD_FILE_NAME"
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