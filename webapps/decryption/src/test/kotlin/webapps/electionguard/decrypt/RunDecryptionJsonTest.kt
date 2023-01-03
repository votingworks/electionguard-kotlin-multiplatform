package webapps.electionguard.decrypt

import kotlin.test.Test

/** Test Decryption with in-process DecryptingTrustee's. */
class RunDecryptionJsonTest {
    val remoteUrl = "http://0.0.0.0:11190"

    @Test
    fun testDecryptionAll() {
        val inputDir = "/home/snake/tmp/electionguard/RunKeyCeremonyTest"
        val trusteeDir = "$inputDir/private_data/trustees"
        main(
            arrayOf(
                "-in",
                inputDir,
                "-trustees",
                trusteeDir,
                "-out",
                "/home/snake/tmp/electionguard/RunRemoteDecryptionJsonTest",
                "-createdBy",
                "RunDecryptionJsonTest",
                "-remoteUrl",
                remoteUrl,
            )
        )
    }

    @Test
    fun testDecryptionSome() {
        val inputDir = "/home/snake/tmp/electionguard/RunKeyCeremonyTest"
        val trusteeDir = "$inputDir/private_data/trustees"
        main(
            arrayOf(
                "-in",
                inputDir,
                "-trustees",
                trusteeDir,
                "-out",
                "/home/snake/tmp/electionguard/RunRemoteDecryptionJsonTest",
                "-createdBy",
                "RunDecryptionJsonTest",
                "-remoteUrl",
                remoteUrl,
                "-missing",
                "3"
            )
        )
    }
}
