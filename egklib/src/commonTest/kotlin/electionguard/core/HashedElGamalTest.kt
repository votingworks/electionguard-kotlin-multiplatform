package electionguard.core

import electionguard.ballot.ContestData
import electionguard.ballot.decryptContestData
import electionguard.ballot.encryptContestData
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

class HashedElGamalTest {
        val group = productionGroup()
        val keypair = elGamalKeyPairFromRandom(group)
        val extendedBaseHash = UInt256.random()

    @Test
    fun testRoundtrip() {
        roundtrip("what the heckeroo?".toByteArray())

        val starting = getSystemTimeInMillis()
        var count = 0
        runTest {
            checkAll(
                propTestSlowConfig,
                Arb.string(minSize = 1, maxSize = 1000),
            ) { testMessage ->
                // expect fun randomBytes(length: Int): ByteArray
                roundtrip(testMessage.toByteArray())
                count++
            }
        }
        val took = getSystemTimeInMillis() - starting
        val perTrip = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
        println(" that took $took millisecs for $count roundtrips = $perTrip msecs/trip wallclock")
    }

    fun roundtrip(testMessage : ByteArray) {
        val subject = testMessage.encryptToHashedElGamal(
            group,
            keypair.publicKey,
            extendedBaseHash,
            0x42.toByte(),
            "care",
            "whatever",
        )

        val beta = subject.c0 powP keypair.secretKey.key

        val roundtrip = subject.decryptToByteArray(
            keypair.publicKey,
            extendedBaseHash,
            0x42.toByte(),
            "care",
            "whatever",
            subject.c0,
            beta
        )!!

        assertTrue(testMessage.contentEquals(roundtrip))
    }

    @Test
    fun testContestData() {
        roundtripContestData("what the heckeroo?".toByteArray())
    }

    fun roundtripContestData(testMessage : ByteArray) {
        //  fun ByteArray.encryptContestData(
        //            publicKey: ElGamalPublicKey, // aka K
        //            extendedBaseHash: UInt256, // aka He
        //            contestId: String, // aka Λ
        //            contestIndex: Int, // ind_c(Λ)
        //            ballotNonce: UInt256
        //        ): HashedElGamalCiphertext {

        val subject = testMessage.encryptContestData(
            keypair.publicKey,
            extendedBaseHash,
            "contestId",
            42,
            UInt256.random(),
        )

        val beta = subject.c0 powP keypair.secretKey.key

        // fun HashedElGamalCiphertext.decryptContestData(
        //    publicKey: ElGamalPublicKey, // aka K
        //    extendedBaseHash: UInt256, // aka He
        //    contestId: String, // aka Λ
        //    alpha: ElementModP,
        //    beta: ElementModP): ByteArray? {

        val roundtrip = subject.decryptContestData(
            keypair.publicKey,
            extendedBaseHash,
            "contestId",
            subject.c0,
            beta
        )!!

        assertTrue(testMessage.contentEquals(roundtrip))
    }

    @Test
    fun testCompareContestData() {
        val ballotNonce = UInt256.random() // 42U.toUInt256() // UInt256.random()
        //val extendedBaseHash = 11U.toUInt256() // UInt256.random()
        //val keypair = elGamalKeyPairFromSecret(1129U.toUInt256().toElementModQ(group))
        compareWithContestData("what the heckeroo?".toByteArray(), extendedBaseHash, keypair, ballotNonce)

        var count = 0
        runTest {
            checkAll(
                propTestSlowConfig,
                Arb.string(minSize = 1, maxSize = 1000),
            ) { testMessage ->
                // expect fun randomBytes(length: Int): ByteArray
                roundtrip(testMessage.toByteArray())
                compareWithContestData(testMessage.toByteArray(), extendedBaseHash, keypair, ballotNonce)
                count++
            }
        }
        println("count = $count")
    }

    fun compareWithContestData(testMessage : ByteArray, extendedBaseHash: UInt256, keypair: ElGamalKeypair, ballotNonce : UInt256) {
        val contestId = "whatever"
        val contestIndex = 42

        val contestData = testMessage.encryptContestData(
            keypair.publicKey,
            extendedBaseHash,
            contestId,
            contestIndex,
            ballotNonce,
        )

        val contestDataNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestIndex, ContestData.contestDataLabel)
        val result = testMessage.encryptToHashedElGamal(
            group,
            keypair.publicKey,
            extendedBaseHash,
            0x22.toByte(),
            ContestData.label,
            context = "${ContestData.contestDataLabel}$contestId",
            contestDataNonce.toElementModQ(group),
        )

        assertEquals(contestData, result)
    }
}