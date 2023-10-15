package electionguard.publish

data class ElectionRecordJsonRPaths(val topDir : String) {
    private val electionPublicRecordDir = "$topDir/public"

    companion object {
        const val JSON_SUFFIX = ".json"
        const val MANIFEST_FILE = "election_manifest_pretty$JSON_SUFFIX"
        const val MANIFEST_CANONICAL_FILE = "election_manifest_canonical.bin"
        const val ELECTION_PARAMETERS_FILE = "election_parameters$JSON_SUFFIX"
        const val HASHES_FILE = "hashes$JSON_SUFFIX"
        const val HASHES_EXT_FILE = "hashes_ext$JSON_SUFFIX"
        const val JOINT_ELECTION_KEY_FILE = "joint_election_public_key$JSON_SUFFIX"

        const val ENCRYPTED_TALLY_FILE = "encrypted_tally$JSON_SUFFIX"
        const val DECRYPTED_TALLY_FILE = "tally$JSON_SUFFIX"

        const val ENCRYPTED_BALLOT_PREFIX = "eballot-"
        const val DECRYPTED_BALLOT_PREFIX = "dballot-"
        const val PLAINTEXT_BALLOT_PREFIX = "pballot-"
        const val PEP_BALLOT_PREFIX = "pepballot-"

        const val ENCRYPTED_DIR = "encrypted_ballots"
        const val CHALLENGED_DIR = "challenged_ballots"
        const val ENCRYPTED_BALLOT_CHAIN = "ballot_chain"
    }

    fun manifestPath(): String {
        return "$electionPublicRecordDir/$MANIFEST_FILE"
    }

    fun manifestCanonicalPath(): String {
        return "$electionPublicRecordDir/$MANIFEST_CANONICAL_FILE"
    }

    fun electionParametersPath(): String {
        return "$electionPublicRecordDir/$ELECTION_PARAMETERS_FILE"
    }

    fun electionHashesPath(): String {
        return "$electionPublicRecordDir/$HASHES_FILE"
    }

    fun electionHashesExtPath(): String {
        return "$electionPublicRecordDir/$HASHES_EXT_FILE"
    }

    fun jointElectionKeyPath(): String {
        return "$electionPublicRecordDir/$JOINT_ELECTION_KEY_FILE"
    }

    fun guardianPath(idx: Int): String {
        return "$electionPublicRecordDir/guardian_$idx.public_key$JSON_SUFFIX"
    }

    fun guardianPrivatePath(trusteeDir: String, idx: String): String {
        return "$trusteeDir/SECRET_for_guradian_$idx/guardian_$idx.SECRET_key$JSON_SUFFIX"
    }


/////////////////////////////////////////////////////////////////////
    fun encryptedTallyPath(): String {
        return "$electionPublicRecordDir/$ENCRYPTED_TALLY_FILE"
    }

    fun decryptedTallyPath(): String {
        return "$electionPublicRecordDir/$DECRYPTED_TALLY_FILE"
    }

    fun plaintextBallotPath(ballotDir: String, ballotId: String): String {
        val id = ballotId.replace(" ", "_")
        return "$ballotDir/$PLAINTEXT_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun encryptedBallotPath(outputDir : String, ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "${outputDir}/$ENCRYPTED_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun pepBallotPath(outputDir : String, ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "${outputDir}/$PEP_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun decryptedBallotPath(ballotId : String): String {
        val id = ballotId.replace(" ", "_")
        return "${decryptedBallotDir()}/$DECRYPTED_BALLOT_PREFIX$id$JSON_SUFFIX"
    }

    fun decryptedBallotDir(): String {
        return "$electionPublicRecordDir/$CHALLENGED_DIR/"
    }


    //////////////////////////////////////

    fun encryptedBallotDir(): String {
        return "$electionPublicRecordDir/$ENCRYPTED_DIR/"
    }

    fun encryptedBallotDir(device: String): String {
        val useDevice = device.replace(" ", "_")
        return "${encryptedBallotDir()}/$useDevice/"
    }

    fun encryptedBallotDevicePath(device: String, ballotId: String): String {
        val useDevice = device.replace(" ", "_")
        val id = ballotId.replace(" ", "_")
        return "${encryptedBallotDir(useDevice)}/${ENCRYPTED_BALLOT_PREFIX}$id${JSON_SUFFIX}"
    }

    fun encryptedBallotChain(device: String): String {
        return "${encryptedBallotDir(device)}/${ENCRYPTED_BALLOT_CHAIN}${JSON_SUFFIX}"
    }
}