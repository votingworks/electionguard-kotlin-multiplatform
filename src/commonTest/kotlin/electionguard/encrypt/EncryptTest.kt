package electionguard.encrypt

import electionguard.ballot.ElectionContext
import electionguard.ballot.ElectionRecord
import electionguard.core.ElGamalKeypair
import electionguard.core.elGamalKeyPairFromSecret
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.core.toUInt256
import electionguard.publish.Consumer
import kotlin.test.Test

class EncryptTest {
    val input = "src/commonTest/data/testPython/"

    @Test
    fun testEncryption() {
        runTest {
            val group = productionGroup()
            val consumer = Consumer(input, group)
            val electionRecord: ElectionRecord = consumer.readElectionRecord()
            val ballot = makeBallot(electionRecord.manifest, "congress-district-7-arlington", 3, 0)
            val keypair: ElGamalKeypair = elGamalKeyPairFromSecret(group.TWO_MOD_Q)

            //     val numberOfGuardians: Int,
            //    /** The quorum of guardians necessary to decrypt an election. Must be <= number_of_guardians. */
            //    val quorum: Int,
            //    /** The joint public key (K) in the ElectionGuard Spec. */
            //    val jointPublicKey: ElementModP,
            //    val manifestHash: UInt256, // matches Manifest.cryptoHash
            //    val cryptoBaseHash: UInt256,
            //    val cryptoExtendedBaseHash: UInt256,
            //    val commitmentHash: UInt256,
            //    val extendedData: Map<String, String>?
            val context = ElectionContext(1,1, keypair.publicKey.key, electionRecord.manifest.cryptoHash,
                group.TWO_MOD_Q.toUInt256(), group.TWO_MOD_Q.toUInt256(), group.TWO_MOD_Q.toUInt256(), null)
            val encryptor = Encryptor(group, electionRecord.manifest, context)
            val result = encryptor.encrypt(ballot, group.TWO_MOD_Q, group.TWO_MOD_Q)

            var first = true
            println("electionRecord.manifest.cryptoHash = ${electionRecord.manifest.cryptoHash}")
            println("result = ${result.cryptoHash} nonce ${result.ballotNonce()}")
            for (contest in result.contests) {
                println(" contest ${contest.contestId} = ${contest.cryptoHash} nonce ${contest.contestNonce}")
                for (selection in contest.selections) {
                    println("  selection ${selection.selectionId} = ${selection.cryptoHash} nonce ${selection.selectionNonce}")
                    if (first) println("\n*****first ${selection}\n")
                    first = false
                }
            }
        }
    }

}