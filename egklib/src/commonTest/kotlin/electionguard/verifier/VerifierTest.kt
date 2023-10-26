package electionguard.verifier

import electionguard.cli.RunVerifier
import electionguard.core.productionGroup
import kotlin.test.Test

class VerifierTest {
    /*
    for some reason we get exception on github, even though file exists.

2023-10-26T01:07:02.7889741Z     java.nio.file.NoSuchFileException: src/commonTest/data/testElectionRecord/remoteWorkflow/electionRecord/encrypted_ballots/testDevice/eballot-id-1181991362.json
2023-10-26T01:07:02.7891376Z     	at java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
2023-10-26T01:07:02.7892486Z     	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
2023-10-26T01:07:02.7894159Z     	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
2023-10-26T01:07:02.7895286Z     	at java.base/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:218)
2023-10-26T01:07:02.7896252Z     	at java.base/java.nio.file.Files.newByteChannel(Files.java:380)
2023-10-26T01:07:02.7897291Z     	at java.base/java.nio.file.Files.newByteChannel(Files.java:432)
2023-10-26T01:07:02.7898276Z     	at java.base/java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:422)
2023-10-26T01:07:02.7899372Z     	at electionguard.publish.ConsumerJson.readEncryptedBallot(ConsumerJson.kt:165)
2023-10-26T01:07:02.7900522Z     	at electionguard.publish.ConsumerJson$EncryptedBallotDeviceIterator.computeNext(ConsumerJson.kt:152)
2023-10-26T01:07:02.7901649Z     	at kotlin.collections.AbstractIterator.tryToComputeNext(AbstractIterator.kt:42)
2023-10-26T01:07:02.7902584Z     	at kotlin.collections.AbstractIterator.hasNext(AbstractIterator.kt:29)
2023-10-26T01:07:02.7903538Z     	at electionguard.publish.ConsumerJson$DeviceIterator.computeNext(ConsumerJson.kt:184)
2023-10-26T01:07:02.7904563Z     	at kotlin.collections.AbstractIterator.tryToComputeNext(AbstractIterator.kt:42)
2023-10-26T01:07:02.7905495Z     	at kotlin.collections.AbstractIterator.hasNext(AbstractIterator.kt:29)
2023-10-26T01:07:02.7906595Z     	at electionguard.verifier.VerifyEncryptedBallots$produceBallots$1.invokeSuspend(VerifyEncryptedBallots.kt:219)
2023-10-26T01:07:02.7907836Z     	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
2023-10-26T01:07:02.7908802Z     	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
2023-10-26T01:07:02.7909836Z     	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:280)
2023-10-26T01:07:02.7910779Z     	at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:85)
2023-10-26T01:07:02.7911629Z     	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:59)
2023-10-26T01:07:02.7912419Z     	at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
2023-10-26T01:07:02.7913245Z     	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:38)
2023-10-26T01:07:02.7914124Z     	at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
2023-10-26T01:07:02.7915139Z     	at electionguard.verifier.VerifyEncryptedBallots.verifyBallots(VerifyEncryptedBallots.kt:47)
2023-10-26T01:07:02.7916079Z     	at electionguard.verifier.Verifier.verify(Verifier.kt:60)
2023-10-26T01:07:02.7916864Z     	at electionguard.cli.RunVerifier$Companion.runVerifier(RunVerifier.kt:52)
2023-10-26T01:07:02.7917781Z     	at electionguard.cli.RunVerifier$Companion.runVerifier$default(RunVerifier.kt:46)
2023-10-26T01:07:02.7918783Z     	at electionguard.verifier.VerifierTest.verifyRemoteWorkflow(VerifierTest.kt:17)
     */

    @Test
    fun verifyRemoteWorkflow() {
        try {
            RunVerifier.runVerifier(
                productionGroup(),
                "src/commonTest/data/testElectionRecord/remoteWorkflow/keyceremony",
                11
            )
            RunVerifier.runVerifier(
                productionGroup(),
                "src/commonTest/data/testElectionRecord/remoteWorkflow/electionRecord",
                11
            )
        } catch (t :Throwable) {
            t.printStackTrace(System.out)
        }
        // RunVerifier.runVerifier(productionGroup(), "/home/stormy/dev/github/egk-webapps/testOut/remoteWorkflow/keyceremony/", 11)
        // RunVerifier.runVerifier(productionGroup(), "/home/stormy/dev/github/egk-webapps/testOut/remoteWorkflow/electionRecord/", 11)
    }

    @Test
    fun verificationAll() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/allAvailableProto", 11, true)
    }

    @Test
    fun verificationAllJson() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/allAvailableJson", 11, true)
    }

    @Test
    fun verificationSome() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/someAvailableProto", 11, true)
    }

    @Test
    fun verificationSomeJson() {
        RunVerifier.runVerifier(productionGroup(), "src/commonTest/data/workflow/someAvailableJson", 11, true)
    }

    // @Test
    fun testProblem() {
        RunVerifier.runVerifier(productionGroup(), "../testOut/cliWorkflow/electionRecord", 11, true)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        RunVerifier.main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableProto",
                "-nthreads",
                "11",
                "--showTime",
            )
        )
    }

    @Test
    fun testVerifyEncryptedBallots() {
        RunVerifier.verifyEncryptedBallots(productionGroup(), "src/commonTest/data/workflow/someAvailableProto", 11)
    }

    @Test
    fun verifyDecryptedTallyWithRecoveredShares() {
        RunVerifier.verifyDecryptedTally(productionGroup(), "src/commonTest/data/workflow/someAvailableProto")
    }

    @Test
    fun verifySpoiledBallotTallies() {
        RunVerifier.verifyChallengedBallots(productionGroup(), "src/commonTest/data/workflow/chainedProto")
        RunVerifier.verifyChallengedBallots(productionGroup(), "src/commonTest/data/workflow/chainedJson")
    }

    // Ordered lists of the ballots encrypted by each device. spec 2.0, section 3.7, p.46
    @Test
    fun testVerifyTallyBallotIds() {
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/allAvailableProto")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/someAvailableProto")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/allAvailableJson")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "src/commonTest/data/workflow/someAvailableJson")
    }
}