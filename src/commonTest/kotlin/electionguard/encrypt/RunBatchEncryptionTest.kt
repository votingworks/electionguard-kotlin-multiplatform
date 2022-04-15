package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(
            arrayOf(
                "-in",
                "/home/snake/tmp/electionguard/remoteWorkflow/encryptor",
                "-ballots",
                "/home/snake/tmp/electionguard/remoteWorkflow/encryptor/election_private_data/plaintext_ballots/",
                "-out",
                "/home/snake/tmp/electionguard/kotlin/runBatchEncryption",
                "-invalidBallots",
                "/home/snake/tmp/electionguard/kotlin/runBatchEncryption/election_private_data/invalid_ballots",
                "-device",
                "CountyCook-precinct079-device24358"
            )
        )
    }
}