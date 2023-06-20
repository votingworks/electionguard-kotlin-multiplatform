package electionguard.publish

data class ElectionRecordJsonPaths(val topDir : String) {
    private val electionRecordDir = topDir

    companion object {
        const val JSON_SUFFIX = ".json"
        const val DECRYPTING_TRUSTEE_PREFIX = "decryptingTrustee"
        const val MANIFEST_FILE = "manifest$JSON_SUFFIX"
        const val ELECTION_CONSTANTS_FILE = "constants$JSON_SUFFIX"
        const val ELECTION_CONFIG_FILE = "electionConfig$JSON_SUFFIX"
        const val ELECTION_INITIALIZED_FILE = "electionInitialized$JSON_SUFFIX"
        const val ENCRYPTED_TALLY_FILE = "encrypted_tally$JSON_SUFFIX"
        const val DECRYPTED_TALLY_FILE = "tally$JSON_SUFFIX"

        const val SUBMITTED_DIR = "submitted_ballots"
    }

    fun manifestPath(): String {
        return "$electionRecordDir/$MANIFEST_FILE"
    }

    fun electionConstantsPath(): String {
        return "$electionRecordDir/$ELECTION_CONSTANTS_FILE"
    }

    fun electionConfigPath(): String {
        return "$electionRecordDir/$ELECTION_CONFIG_FILE"
    }

    fun electionInitializedPath(): String {
        return "$electionRecordDir/$ELECTION_INITIALIZED_FILE"
    }

    fun encryptedTallyPath(): String {
        return "$electionRecordDir/$ENCRYPTED_TALLY_FILE"
    }

    fun decryptedTallyPath(): String {
        return "$electionRecordDir/$DECRYPTED_TALLY_FILE"
    }

    fun plaintextBallotPath(ballotDir: String, ballotId: String): String {
        val id = ballotId.replace(" ", "_")
        return "$ballotDir/$id$JSON_SUFFIX"
    }

    fun encryptedBallotPath(ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "$electionRecordDir/$SUBMITTED_DIR/$id$JSON_SUFFIX"
    }

    fun encryptedBallotDir(): String {
        return "$electionRecordDir/$SUBMITTED_DIR/"
    }

    fun decryptedBallotPath(ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "$electionRecordDir/spoiled_ballots/$id$JSON_SUFFIX"
    }

    fun decryptedBallotDir(): String {
        return "$electionRecordDir/spoiled_ballots/"
    }

    fun decryptingTrusteePath(trusteeDir: String, guardianId: String): String {
        val id = guardianId.replace(" ", "_")
        return "$trusteeDir/$DECRYPTING_TRUSTEE_PREFIX-$id$JSON_SUFFIX"
    }
}