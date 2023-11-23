package electionguard.publish

data class ElectionRecordJsonPaths(val topDir : String) {
    private val electionRecordDir = topDir

    companion object {
        const val JSON_SUFFIX = ".json"
        const val DECRYPTING_TRUSTEE_PREFIX = "decryptingTrustee"
        const val MANIFEST_FILE = "manifest$JSON_SUFFIX"
        const val ELECTION_CONSTANTS_FILE = "constants$JSON_SUFFIX"
        const val ELECTION_CONFIG_FILE = "election_config$JSON_SUFFIX"
        const val ELECTION_INITIALIZED_FILE = "election_initialized$JSON_SUFFIX"
        const val ENCRYPTED_TALLY_FILE = "encrypted_tally$JSON_SUFFIX"
        const val DECRYPTED_TALLY_FILE = "tally$JSON_SUFFIX"

        const val ENCRYPTED_BALLOT_PREFIX = "eballot-"
        const val DECRYPTED_BALLOT_PREFIX = "dballot-"
        const val PLAINTEXT_BALLOT_PREFIX = "pballot-"

        const val ENCRYPTED_DIR = "encrypted_ballots"
        const val CHALLENGED_DIR = "challenged_ballots"
        const val ENCRYPTED_BALLOT_CHAIN = "ballot_chain"

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
        return "$ballotDir/$PLAINTEXT_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun encryptedBallotPath(ballotDir : String, ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "${ballotDir}/$ENCRYPTED_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun decryptedBallotPath(ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "${decryptedBallotDir()}/$DECRYPTED_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun decryptedBallotDir(): String {
        return "$electionRecordDir/$CHALLENGED_DIR/"
    }

    fun decryptingTrusteePath(trusteeDir: String, guardianId: String): String {
        val id = guardianId.replace(" ", "_")
        return "$trusteeDir/$DECRYPTING_TRUSTEE_PREFIX-$id$JSON_SUFFIX"
    }

    //////////////////////////////////////

    fun encryptedBallotDir(): String {
        return "$electionRecordDir/$ENCRYPTED_DIR/"
    }

    fun encryptedBallotDir(device: String): String {
        val useDevice = device.replace(" ", "_")
        return "${encryptedBallotDir()}/$useDevice/"
    }

    fun encryptedBallotDevicePath(device: String?, ballotId: String): String {
        val id = ballotId.replace(" ", "_")
        return if (device != null) {
            val useDevice = device.replace(" ", "_")
            "${encryptedBallotDir(useDevice)}/${ENCRYPTED_BALLOT_PREFIX}$id${JSON_SUFFIX}"
        } else {
            "${topDir}/${ENCRYPTED_BALLOT_PREFIX}$id${JSON_SUFFIX}"
        }
    }

    fun encryptedBallotChain(device: String): String {
        return "${encryptedBallotDir(device)}/${ENCRYPTED_BALLOT_CHAIN}${JSON_SUFFIX}"
    }
}