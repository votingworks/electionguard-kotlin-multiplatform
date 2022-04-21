package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/testJava/kickstart/encryptor",
                "-ballots",
                "src/commonTest/data/testJava/kickstart/encryptor/election_private_data/plaintext_ballots/",
                "-out",
                "testOut/native/runBatchEncryption",
                "-invalidBallots",
                "testOut/native/runBatchEncryption/election_private_data/invalid_ballots",
                "-fixedNonces",
                "-nthreads",
                "6"
            )
        )
    }
}