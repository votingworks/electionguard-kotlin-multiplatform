package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "testOut/kotlin2",
                "-ballots",
                "src/commonTest/data/testJava/kickstart/encryptor/election_private_data/plaintext_ballots/",
                "-out",
                "testOut/kotlin2",
                "-invalidBallots",
                "testOut/election_private_data/invalid_ballots",
                "-fixedNonces",
                "-nthreads",
                "6"
            )
        )
    }
}