package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/testJava/encryptor",
                "-ballots",
                "src/commonTest/data/testJava/encryptor/election_private_data/plaintext_ballots/",
                "-out",
                "src/commonTest/data/testing/runBatchEncryption",
                "-invalidBallots",
                "src/commonTest/data/testing/runBatchEncryption/election_private_data/invalid_ballots",
                "-device",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}