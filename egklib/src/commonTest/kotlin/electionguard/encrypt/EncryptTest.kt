package electionguard.encrypt

import electionguard.core.*
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class EncryptTest {
    val input = "src/commonTest/data/allAvailable"

    // sanity check that encryption doesnt barf
    @Test
    fun testEncryption() {
        runTest {
            val group = productionGroup()
            val electionRecord = readElectionRecord(group, input)
            val electionInit = electionRecord.electionInit()!!
            val ballot = makeBallot(electionRecord.manifest(), "ballotStyle", 3, 0)

            val encryptor = Encryptor(group, electionRecord.manifest(), ElGamalPublicKey(electionInit.jointPublicKey), electionInit.extendedBaseHash, "device")
            val result = encryptor.encrypt(ballot)

            var first = true
            println("result = ${result.confirmationCode} nonce ${result.ballotNonce}")
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

    // test that if you pass in the same ballot nonce, you get the same encryption
    @Test
    fun testEncryptionWithBallotNonce() {
        runTest {
            val group = productionGroup()
            val electionRecord = readElectionRecord(group, input)
            val electionInit = electionRecord.electionInit()!!
            val ballot = makeBallot(electionRecord.manifest(), "ballotStyle", 3, 0)

            val encryptor = Encryptor(group, electionRecord.manifest(), ElGamalPublicKey(electionInit.jointPublicKey), electionInit.extendedBaseHash, "device")
            val nonce1 = UInt256.random()
            val result1 = encryptor.encrypt(ballot, nonce1, 0)
            val result2 = encryptor.encrypt(ballot, nonce1, 0)

            result1.contests.forEachIndexed { index, contest1 ->
                val contest2 = result2.contests[index]
                contest1.selections.forEachIndexed { sindex, selection1 ->
                    val selection2 = contest2.selections[sindex]
                    assertEquals(selection1, selection2)
                }
                assertEquals(contest1, contest2)
            }
            // data class equals doesnt compare bytearray.contentEquals()
            assertEquals(result1.confirmationCode, result2.confirmationCode)
            assertEquals(result1.timestamp, result2.timestamp)
            assertEquals(result1.ballotNonce, result2.ballotNonce)
        }
    }
}