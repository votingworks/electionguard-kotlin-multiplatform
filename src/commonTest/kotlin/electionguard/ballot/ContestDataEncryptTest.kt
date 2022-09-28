package electionguard.ballot

import electionguard.core.decrypt
import electionguard.core.elGamalKeyPairFromRandom
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.protoconvert.importHashedCiphertext
import electionguard.protoconvert.publishHashedCiphertext
import pbandk.decodeFromByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class ContestDataEncryptTest {
    val context = productionGroup()
    val keypair = elGamalKeyPairFromRandom(context)

    @Test
    fun serializeContestData() {
        doOne( ContestData(listOf(), listOf()))
        doOne( ContestData(listOf(), listOf(), ContestDataStatus.null_vote))
        doOne( ContestData(listOf(), listOf(), ContestDataStatus.under_vote))

        doOne( ContestData(listOf(1,2,3), listOf()))
        doOne( ContestData(listOf(1,2,3,4), listOf()))
        doOne( ContestData(listOf(111,211,311), listOf()))
        doOne( ContestData(listOf(111,211,311,411), listOf()))
        doOne( ContestData(listOf(111,211,311,411, 511), listOf()))

        doOne( ContestData(listOf(1,2,3,4), listOf("a string")))

        doOne( ContestData(listOf(1,2,3,4), listOf("a long string ")))

        doOne( ContestData(listOf(1,2,3,4), listOf("a longer longer longer string")))

        doOne( ContestData(MutableList(100) {it}, emptyList()), true)
        doOne( ContestData(MutableList(100) {it}, listOf("a longer longer longer string")), true)

        doOne( ContestData(listOf(1,2,3,4), listOf(
            "1000000",
            "a string",
            "a long string ",
            "a longer longer longer string",
            "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
        )), true)

        println()
    }

    fun doOne(contestData: ContestData, isTruncated: Boolean = false) {
        println("")
        var starting = getSystemTimeInMillis()

        val target = contestData.encrypt(keypair.publicKey, 1)
        assertEquals(64, target.c1.size)
        // assertEquals(target.numBytes, target.c1.size)
        var took = getSystemTimeInMillis() - starting
        println(" contestData.encrypt took $took millisecs")

        val hashProto = target.publishHashedCiphertext()
        val hashRoundtrip = context.importHashedCiphertext(hashProto)
        assertEquals(target, hashRoundtrip)

        // HMAC decryption
        starting = getSystemTimeInMillis()
        val contestDataBArt = target.decrypt(keypair)!!
        took = getSystemTimeInMillis() - starting
        println(" contestData.decrypt took $took millisecs")

        // ContestData roundtrip
        val contestDataProtoRoundtrip = electionguard.protogen.ContestData.decodeFromByteArray(contestDataBArt)
        val contestDataRoundtrip = contestDataProtoRoundtrip.import()

        if (isTruncated) {
            println("truncated $contestData")
            println("          $contestDataRoundtrip")
            assertEquals(contestData.status, contestDataRoundtrip.status)
        } else {
            assertEquals(contestData, contestDataRoundtrip)
        }
    }
}
