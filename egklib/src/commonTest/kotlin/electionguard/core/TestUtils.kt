package electionguard.core

import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestUtils {
    @Test
    fun integersToByteArrays() {
        assertContentEquals(byteArrayOf(0x12), 0x12U.toULong().toByteArray())
        assertContentEquals(byteArrayOf(0x12, 0x34), 0x1234U.toULong().toByteArray())
        assertContentEquals(
            byteArrayOf(0x12, 0x34, 0x56, 0x78),
            0x12345678U.toULong().toByteArray()
        )
        assertContentEquals(byteArrayOf(0x00, 0x34, 0x56, 0x78), 0x345678U.toULong().toByteArray())
        assertContentEquals(
            byteArrayOf(
                0x12,
                0x34,
                0x56,
                0x78,
                0x9a.toByte(),
                0xbc.toByte(),
                0xde.toByte(),
                0xf0.toByte()
            ),
            0x123456789abcdef0U.toByteArray()
        )
        assertContentEquals(
            byteArrayOf(
                0x00,
                0x00,
                0x56,
                0x78,
                0x9a.toByte(),
                0xbc.toByte(),
                0xde.toByte(),
                0xf0.toByte()
            ),
            0x56789abcdef0U.toByteArray()
        )

        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), 0x12345678U.toByteArray())
        assertContentEquals(byteArrayOf(0x12, 0x34, 0x56, 0x78), 0x12345678.toByteArray())
        assertContentEquals((-0x12345678).toUInt().toByteArray(), (-0x12345678).toByteArray())
        assertContentEquals(byteArrayOf(0x00, 0x34, 0x56, 0x78), 0x345678.toByteArray())
    }

    @Test
    fun testListNullExclusion() {
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).noNullValuesOrNull())
        assertEquals(null, listOf(1, 2, 3, null).noNullValuesOrNull())
    }

    @Test
    fun testMapNullExclusion() {
        assertEquals(
            mapOf(1 to "one", 2 to "two"),
            mapOf(1 to "one", 2 to "two").noNullValuesOrNull()
        )
        assertEquals(null, mapOf(1 to "one", 2 to "two", 3 to null).noNullValuesOrNull())
    }

    @Test
    fun byteConcatenation() {
        assertContentEquals(byteArrayOf(1, 2, 3, 4), byteArrayOf(1, 2) + byteArrayOf(3, 4))
        assertContentEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6),
            concatByteArrays(byteArrayOf(1, 2), byteArrayOf(3), byteArrayOf(), byteArrayOf(4, 5, 6))
        )
    }
}

fun generateRangeChaumPedersenProofKnownNonce(
    context: GroupContext
): ChaumPedersenRangeProofKnownNonce {
    return ChaumPedersenRangeProofKnownNonce(
        listOf(generateGenericChaumPedersenProof(context)),
    )
}

fun generateGenericChaumPedersenProof(context: GroupContext): ChaumPedersenProof {
    return ChaumPedersenProof(generateElementModQ(context), generateElementModQ(context),)
}

fun generateSchnorrProof(context: GroupContext): SchnorrProof {
    return SchnorrProof(
        generatePublicKey(context),
        generateElementModQ(context),
        generateElementModQ(context),
    )
}

fun generateCiphertext(context: GroupContext): ElGamalCiphertext {
    return ElGamalCiphertext(generateElementModP(context), generateElementModP(context))
}

fun generateHashedCiphertext(context: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(generateElementModP(context), "what".encodeToByteArray(), generateUInt256(context), 42)
}

fun generateElementModQ(context: GroupContext): ElementModQ {
    return context.uIntToElementModQ(Random.nextUInt(134217689.toUInt()))
}

fun generateUInt256(context: GroupContext): UInt256 {
    return generateElementModQ(context).toUInt256();
}

fun generateElementModP(context: GroupContext) = context.uIntToElementModP(Random.nextUInt(1879047647.toUInt()))

fun generatePublicKey(group: GroupContext): ElementModP =
    group.gPowP(group.randomElementModQ())