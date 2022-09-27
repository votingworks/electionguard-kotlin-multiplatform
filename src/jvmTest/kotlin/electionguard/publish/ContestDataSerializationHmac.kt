package electionguard.publish

import electionguard.core.decrypt
import electionguard.core.elGamalKeyPairFromRandom
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.productionGroup
import electionguard.protoconvert.importHashedCiphertext
import electionguard.protoconvert.publishHashedCiphertext
import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.import
import org.junit.jupiter.api.Test
import pbandk.decodeFromByteBuffer
import pbandk.encodeToStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class ContestDataSerializationHmac {
    val context = productionGroup()
    val keypair = elGamalKeyPairFromRandom(context)

    @Test
    fun serializeContestData() {
        println("\nbytes  ContestData")
        doOne( ContestData(listOf(), listOf()))
        doOne( ContestData(listOf(), listOf(), ContestDataStatus.null_vote))
        doOne( ContestData(listOf(), listOf(), ContestDataStatus.under_vote))

        doOne( ContestData(listOf(1,2,3), listOf()))
        doOne( ContestData(listOf(1,2,3,4), listOf()))
        doOne( ContestData(listOf(111,211,311), listOf()))
        doOne( ContestData(listOf(111,211,311,411), listOf()))
        doOne( ContestData(listOf(111,211,311,411, 511), listOf()))

        doOne( ContestData(listOf(1,2,3,4), listOf(
            "a string",
        )))

        doOne( ContestData(listOf(1,2,3,4), listOf(
            "a long string ",
        )))

        doOne( ContestData(listOf(1,2,3,4), listOf(
            "a longer longer longer string",
        )))

        doOne( ContestData(listOf(1,2,3,4), listOf(
            "1000000",
            "a string",
            "a long string ",
            "a longer longer longer string",
            "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
        )))
        println()
    }

    fun doOne(contestData: ContestData) {
        println("")
        var starting = getSystemTimeInMillis()

        val contestDataProto = contestData.publish()
        val contestDataBB = serialize(contestDataProto)
        val contestDataBA = contestDataBB.array()
        println("  contestDataBB = ${contestDataBB.limit()}")

        // HMAC encryption
        val hashedElGamalEncrypt = contestDataBA.hashedElGamalEncrypt(keypair.publicKey)
        println("  hashed = ${hashedElGamalEncrypt.c1.size}")
        val hashedProto = hashedElGamalEncrypt.publishHashedCiphertext()
        println("  hashedProto = ${hashedProto.c1.array.size}")
        val hashedProtoBB = serialize(hashedProto)
        println("  hashedProtoBB = ${hashedProtoBB.limit()}")

        var took = getSystemTimeInMillis() - starting
        println(" hashedElGamalEncrypt took $took millisecs")
        starting = getSystemTimeInMillis()

        // HMAC decryption
        val hashedProtoRoundtrip = deserializeHashedProto(hashedProtoBB)
        val hashedRoundtrip = context.importHashedCiphertext(hashedProtoRoundtrip)!!
        val contestDataBArt = hashedRoundtrip.decrypt(keypair)!!
        println("  contestDataBArt = ${contestDataBArt.size}")

        // ContestData roundtrip
        val contestDataProtoRoundtrip = deserializeContestData(ByteBuffer.wrap(contestDataBArt))
        val contestDataRoundtrip = contestDataProtoRoundtrip.import()

        assertEquals(contestDataRoundtrip, contestData)
        // println("  ${hashedProtoBB.limit()} = $contestDataRoundtrip")

        took = getSystemTimeInMillis() - starting
        println(" hashedElGamalDecrypt took $took millisecs")
    }

    fun deserializeContestData(buffer: ByteBuffer): electionguard.protogen.ContestData {
        return electionguard.protogen.ContestData.decodeFromByteBuffer(buffer)
    }

    fun deserializeHashedProto(buffer: ByteBuffer): electionguard.protogen.HashedElGamalCiphertext {
        return electionguard.protogen.HashedElGamalCiphertext.decodeFromByteBuffer(buffer)
    }

    fun serialize(proto: pbandk.Message): ByteBuffer {
        val bb = ByteArrayOutputStream()
        proto.encodeToStream(bb)
        return ByteBuffer.wrap(bb.toByteArray())
    }
}
