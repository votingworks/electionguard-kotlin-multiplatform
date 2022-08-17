package electionguard.encrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionInitialized
import electionguard.core.ElGamalPublicKey
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.publish.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"

    @Test
    fun testEncryption() {
        runTest {
            val group = productionGroup()
            val consumerIn = Consumer(input, group)
            val electionInit: ElectionInitialized = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }
            val ballot = makeBallot(electionInit.manifest(), "congress-district-7-arlington", 3, 0)

            val encryptor = Encryptor(group, electionInit.manifest(), ElGamalPublicKey(electionInit.jointPublicKey), electionInit.cryptoExtendedBaseHash)
            val result = encryptor.encrypt(ballot, group.TWO_MOD_Q, group.TWO_MOD_Q)

            var first = true
            println("electionRecord.manifest.cryptoHash = ${electionInit.manifestHash}")
            println("result = ${result.cryptoHash} nonce ${result.ballotNonce()}")
            for (contest in result.contests) {
                // println(" contest ${contest.contestId} = ${contest.cryptoHash} nonce ${contest.contestNonce}")
                for (selection in contest.selections) {
                    // println("  selection ${selection.selectionId} = ${selection.cryptoHash} nonce ${selection.selectionNonce}")
                    if (first) println("\n*****first ${selection}\n")
                    first = false
                }
            }
        }
    }

    @Test
    fun testEncryptionWithMasterNonce() {
        runTest {
            val group = productionGroup()
            val consumerIn = Consumer(input, group)
            val electionInit: ElectionInitialized = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException( it ) }
            val ballot = makeBallot(electionInit.manifest(), "congress-district-7-arlington", 3, 0)

            val encryptor = Encryptor(group, electionInit.manifest(), ElGamalPublicKey(electionInit.jointPublicKey), electionInit.cryptoExtendedBaseHash)
            val nonce1 = group.randomElementModQ(minimum = 2)
            val nonce2 = group.randomElementModQ(minimum = 3)
            val result1 = encryptor.encrypt(ballot, nonce1, nonce2, 0)
            val result2 = encryptor.encrypt(ballot, nonce1, nonce2, 0)

            result1.contests.forEachIndexed { index, contest1 ->
                val contest2 = result2.contests[index]
                contest1.selections.forEachIndexed { sindex, selection1 ->
                    val selection2 = contest2.selections[sindex]
                    assertEquals(selection1, selection2)
                }
                assertEquals(contest1, contest2)
            }
            assertEquals(result1, result2)
        }
    }
}