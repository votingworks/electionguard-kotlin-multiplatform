package electionguard.ballot

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.protoconvert.importHashedCiphertext
import electionguard.protoconvert.publishProto
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val debug = false

class ContestDataEncryptTest {
    val group = tinyGroup()
    val keypair = elGamalKeyPairFromRandom(group)
    val extendedBaseHash = UInt256.random()

    @Test
    fun serializeContestData() {
        encryptDecrypt(ContestData(listOf(), listOf()))
        encryptDecrypt(ContestData(listOf(), listOf(), ContestDataStatus.null_vote))
        encryptDecrypt(ContestData(listOf(), listOf(), ContestDataStatus.under_vote))

        encryptDecrypt(ContestData(listOf(1, 2, 3), listOf()))
        encryptDecrypt(ContestData(listOf(1, 2, 3, 4), listOf()))
        encryptDecrypt(ContestData(listOf(111, 211, 311), listOf()))
        encryptDecrypt(ContestData(listOf(111, 211, 311, 411), listOf()))
        encryptDecrypt(ContestData(listOf(111, 211, 311, 411, 511), listOf()))

        encryptDecrypt(ContestData(listOf(1, 2, 3, 4), listOf("a string")))

        encryptDecrypt(ContestData(listOf(1, 2, 3, 4), listOf("a long string ")))

        encryptDecrypt(ContestData(listOf(1, 2, 3, 4), listOf("a longer longer longer string")))

        encryptDecrypt(ContestData(MutableList(100) { it }, emptyList()), true)
        encryptDecrypt(ContestData(MutableList(100) { it }, listOf("a longer longer longer string")), true)

        encryptDecrypt(
            ContestData(
                listOf(1, 2, 3, 4), listOf(
                    "1000000",
                    "a string",
                    "a long string ",
                    "a longer longer longer string",
                    "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
                )
            ), true
        )

        println()
    }

    fun encryptDecrypt(contestData: ContestData, isTruncated: Boolean = false) {
        println("")
        var starting = getSystemTimeInMillis()

        //  publicKey: ElGamalPublicKey, // aka K
        //        extendedBaseHash: UInt256, // aka He
        //        contestId: String, // aka Λ
        //        ballotNonce: UInt256,
        //        votesAllowed: Int
        val ballotNonce = UInt256.random()
        val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", ballotNonce, 1)
        assertEquals(64, target.c1.size)
        var took = getSystemTimeInMillis() - starting
        println(" contestData.encrypt took $took millisecs")

        val hashProto = target.publishProto()
        val hashRoundtrip = group.importHashedCiphertext(hashProto)
        assertEquals(target, hashRoundtrip)

        // HMAC decryption
        starting = getSystemTimeInMillis()

        val contestDataResult = target.decryptWithNonceToContestData(keypair.publicKey, extendedBaseHash, "contestId", ballotNonce)
        assertTrue( contestDataResult is Ok)
        val contestDataRoundtrip = contestDataResult.unwrap()

        took = getSystemTimeInMillis() - starting
        println(" contestData.decrypt took $took millisecs")

        if (isTruncated) {
            println("truncated $contestData")
            println("          $contestDataRoundtrip")
            assertEquals(contestData.status, contestDataRoundtrip.status)
        } else {
            assertEquals(contestData, contestDataRoundtrip)
        }
    }

    // fuzz test that ElGamal has a constant encryption length
    @Test
    fun hashedElGamalLength1vote1writein() {
        runTest {
            val group = tinyGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            checkAll(
                propTestSlowConfig,
                Arb.int(min = 0, max = 10),
                Arb.string(),
            ) { nover, writein ->
                val contestData = ContestData(MutableList(nover) { it }, listOf(writein))
                if (debug) println("\ncontestData = $contestData")

                val votes = 1
                val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", UInt256.random(), votes)
                assertEquals((1 + votes) * 32, target.c1.size)
            }
        }
    }

    @Test
    fun hashedElGamalLength1voteNwriteins() {
        runTest {
            val group = tinyGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            checkAll(
                propTestSlowConfig,
                Arb.int(min = 0, max = 10),
                Arb.string(),
                Arb.int(min = 1, max = 4),
            ) { nover, writein, nwriteins ->
                val contestData = ContestData(MutableList(nover) { it }, MutableList(nwriteins) { writein })
                if (debug) println("\ncontestData = $contestData")

                val votes = 1
                val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", UInt256.random(), votes)
                assertEquals((1 + votes) * 32, target.c1.size)
            }
        }
    }

    @Test
    fun hashedElGamalLength2voteNwriteins() {
        runTest {
            val group = tinyGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            checkAll(
                propTestSlowConfig,
                Arb.int(min = 0, max = 15),
                Arb.string(),
                Arb.int(min = 1, max = 5),
            ) { nover, writein, nwriteins ->
                val contestData = ContestData(MutableList(nover) { it }, MutableList(nwriteins) { writein })
                if (debug) println("\ncontestData = $contestData")

                val votes = 2
                val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", UInt256.random(), votes)
                assertEquals((1 + votes) * 32, target.c1.size)
            }
        }
    }

    @Test
    fun hashedElGamalLength1voteBigOvervote() {
        runTest {
            val group = tinyGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            checkAll(
                propTestSlowConfig,
                Arb.int(min = 0, max = 100),
                Arb.string(),
                Arb.int(min = 1, max = 5),
            ) { nover, writein, nwriteins ->
                val contestData = ContestData(MutableList(nover) { it }, MutableList(nwriteins) { writein })
                if (debug) println("\ncontestData = $contestData")

                val votes = 1
                val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", UInt256.random(), votes)
                assertEquals((1 + votes) * 32, target.c1.size)
            }
        }
    }

    @Test
    fun hashedElGamalLength3voteBig() {
        runTest {
            val group = tinyGroup()
            val keypair = elGamalKeyPairFromRandom(group)
            checkAll(
                propTestSlowConfig,
                Arb.int(min = 0, max = 1000),
                Arb.string(),
                Arb.int(min = 1, max = 50),
            ) { nover, writein, nwriteins ->
                val contestData = ContestData(MutableList(nover) { it }, MutableList(nwriteins) { writein })
                if (debug) println("\ncontestData = $contestData")

                val votes = 3
                val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", UInt256.random(), votes)
                if ((1 + votes) * 32 != target.c1.size) {
                    println("${(1 + votes) * 32} != ${target.c1.size}")
                }
                assertEquals((1 + votes) * 32, target.c1.size)
            }
        }
    }

    @Test
    fun problem() {
        runTest {
            val writein =
                "]e\$B-AGbal7P<A4,O%)fS%%IV1pv8h,-+PDs9M.%z-=2 9uJE;ZGDNDYt,Fq=p\"(7caN4j:(?z mUFW1C;yir]"
            val contestData = ContestData(MutableList(5) { it }, MutableList(3) { writein })
            if (debug) println("\ncontestData = $contestData")

            val votes = 1
            val target = contestData.encrypt(keypair.publicKey, extendedBaseHash, "contestId", UInt256.random(), votes)
            if ((1 + votes) * 32 != target.c1.size) {
                assertEquals((1 + votes) * 32, target.c1.size)
            }
        }
    }
}
