package electionguard.workflow

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.ballot.importContestData
import electionguard.core.decrypt
import electionguard.core.elGamalKeyPairFromRandom
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.encrypt.Encryptor
import electionguard.input.BallotInputBuilder
import electionguard.publish.Consumer
import pbandk.decodeFromByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContestDataTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val context = productionGroup()
    val keypair = elGamalKeyPairFromRandom(context)

    @Test
    fun testEncryptionWithWriteIn() {
        runTest {
            val consumerIn = Consumer(input, context)
            val electionInit: ElectionInitialized =
                consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val manifest: Manifest = electionInit.manifest()

            val builder = BallotInputBuilder(manifest, "ballot_id").setStyle("congress-district-7-arlington")
            val ballot = builder
                .addContest(0)
                    .addSelection(0, vote = 1)
                .done()
                .addContest(1)
                    .addWriteIn("Sekula-Gibbs")
                .done()
                .addContest(2)
                    .addWriteIn("SekulaGibbs")
                    .addSelection(0, vote = 1)
                .done()
                .addContest(3)
                    .addWriteIn("SekulaGibbs")
                    .addWriteIn("Sekula-Gibbs")
                    .addSelection(0, vote = 1)
                    .addSelection(1, vote = 1)
                .done()
                .addContest(4)
                .addWriteIn("01234567890123456789012345678901234567890123456789012345678901234567890123456789")
                .done()
                .addContest(5)
                    .addWriteIn("01234567890123456789012345678901234567890123456789012345678901234567890123456789")
                    .addSelection(0, vote = 1)
                .done()
                .addContest(6)
                    .addSelection(0, vote = 1)
                    .addSelection(1, vote = 1)
                    .addSelection(2, vote = 1)
                    .addSelection(3, vote = 1)
                .done()
                .build()


            val encryptor = Encryptor(context, manifest, keypair.publicKey, electionInit.cryptoExtendedBaseHash)
            val nonce1 = context.randomElementModQ(minimum = 2)
            val nonce2 = context.randomElementModQ(minimum = 3)
            val eballot = encryptor.encrypt(ballot, nonce1, nonce2, 0)

            eballot.contests.forEachIndexed { idx, it ->
                assertNotNull(it.contestData)
                assertEquals(64, it.contestData.c1.size)

                val baRT = it.contestData.decrypt(keypair)!!
                val protoRoundtrip = electionguard.protogen.ContestData.decodeFromByteArray(baRT)
                val contestDataResult = importContestData(protoRoundtrip)
                assertTrue( contestDataResult is Ok)
                val contestDataRoundtrip = contestDataResult.unwrap()

                println("  $contestDataRoundtrip")
                if (idx == 0) {
                    assertEquals(ContestDataStatus.normal, contestDataRoundtrip.status)
                    assertEquals(emptyList(), contestDataRoundtrip.writeIns)
                } else if (idx == 1) {
                    assertEquals(ContestDataStatus.normal, contestDataRoundtrip.status)
                    assertEquals(listOf("Sekula-Gibbs"), contestDataRoundtrip.writeIns, )
                } else if (idx == 2) {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status, )
                    assertEquals(listOf("SekulaGibbs"), contestDataRoundtrip.writeIns, )
                    assertEquals(listOf(10), contestDataRoundtrip.overvotes, )
                } else if (idx == 3) {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status, )
                    assertEquals(listOf("SekulaGibbs", "Sekula-Gibbs"), contestDataRoundtrip.writeIns, )
                    assertEquals(listOf(15, 16), contestDataRoundtrip.overvotes, )
                } else if (idx == 4) {
                    assertEquals(ContestDataStatus.normal, contestDataRoundtrip.status)
                    assertEquals(listOf("012345678901234567890123456789*"), contestDataRoundtrip.writeIns, )
                    assertEquals(emptyList(), contestDataRoundtrip.overvotes)
                } else if (idx == 5) {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status)
                    assertEquals(listOf("012345678901234567890123456789*"), contestDataRoundtrip.writeIns, )
                    assertEquals(listOf(25), contestDataRoundtrip.overvotes)
                } else if (idx == 6) {
                    assertEquals(ContestDataStatus.over_vote, contestDataRoundtrip.status)
                    assertEquals(emptyList(), contestDataRoundtrip.writeIns, )
                    assertEquals(listOf(30, 31, 32, 33), contestDataRoundtrip.overvotes)
                } else {
                    assertEquals(ContestDataStatus.null_vote, contestDataRoundtrip.status, )
                    assertEquals(emptyList(), contestDataRoundtrip.writeIns)
                }
            }
        }
    }
}