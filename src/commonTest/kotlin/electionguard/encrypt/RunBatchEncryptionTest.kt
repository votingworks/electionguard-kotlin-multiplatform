package electionguard.encrypt

import kotlin.test.Test

class RunBatchEncryptionTest {

    @Test
    fun testRunBatchEncryptionTest() {
        main(arrayOf("-in",
                "/home/snake/tmp/electionguard/kickstart/encryptor",
                "-ballots",
                "/home/snake/tmp/electionguard/kickstart/encryptor/election_private_data/plaintext_ballots",
                "-out",
                "/home/snake/tmp/electionguard/kotlin/runBatchEncryption",
                "-device",
                "CountyCook-precinct079-device24358"))
    }
}