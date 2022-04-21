package electionguard.encrypt

import electionguard.ballot.ElectionContext
import electionguard.ballot.ElectionRecord
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.elGamalKeyPairFromSecret
import electionguard.core.productionGroup
import electionguard.core.randomElementModQ
import electionguard.core.runTest
import electionguard.core.toUInt256
import electionguard.publish.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptTest {
    val input = "src/commonTest/data/testPython"

    @Test
    fun testEncryption() {
        runTest {
            val group = productionGroup()
            val consumer = Consumer(input, group)
            val electionRecord: ElectionRecord = consumer.readElectionRecord()
            val ballot = makeBallot(electionRecord.manifest, "congress-district-7-arlington", 3, 0)
            val keypair: ElGamalKeypair = elGamalKeyPairFromSecret(group.TWO_MOD_Q)

            val context = ElectionContext(1,1, keypair.publicKey.key, electionRecord.manifest.cryptoHash,
                group.TWO_MOD_Q.toUInt256(), group.TWO_MOD_Q.toUInt256(), group.TWO_MOD_Q.toUInt256(), null)
            val encryptor = Encryptor(group, electionRecord.manifest, ElGamalPublicKey(context.jointPublicKey), context.cryptoExtendedBaseHash)
            val result = encryptor.encrypt(ballot, group.TWO_MOD_Q, group.TWO_MOD_Q)

            var first = true
            println("electionRecord.manifest.cryptoHash = ${electionRecord.manifest.cryptoHash}")
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
            val consumer = Consumer(input, group)
            val electionRecord: ElectionRecord = consumer.readElectionRecord()
            val ballot = makeBallot(electionRecord.manifest, "congress-district-7-arlington", 3, 0)
            val keypair: ElGamalKeypair = elGamalKeyPairFromSecret(group.TWO_MOD_Q)

            val context = ElectionContext(1,1, keypair.publicKey.key, electionRecord.manifest.cryptoHash,
                group.TWO_MOD_Q.toUInt256(), group.TWO_MOD_Q.toUInt256(), group.TWO_MOD_Q.toUInt256(), null)
            val encryptor = Encryptor(group, electionRecord.manifest, ElGamalPublicKey(context.jointPublicKey), context.cryptoExtendedBaseHash)
            val nonce1 = group.randomElementModQ()
            val nonce2 = group.randomElementModQ()
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