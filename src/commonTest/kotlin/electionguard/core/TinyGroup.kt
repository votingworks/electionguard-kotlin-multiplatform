package electionguard.core

import electionguard.ballot.ElectionConstants

private val tinyGroupContext =
    TinyGroupContext(
        p = intTestP.toUInt(),
        q = intTestQ.toUInt(),
        r = intTestR.toUInt(),
        g = intTestG.toUInt(),
        name = "32-bit test group",
    )

/**
 * The "tiny" group is built around having p and q values that fit inside unsigned 32-bit integers.
 * The purpose of this group is to make the unit tests run (radically) faster by avoiding the
 * overheads associated with bignum arithmetic on thousands of bits. The values of p and q are big
 * enough to make it unlikely that, for example, false hash collisions might crop up in texting.
 *
 * Needless to say, THIS GROUP SHOULD NOT BE USED IN PRODUCTION CODE! And, in fact, this is why this
 * group isn't exported as part of the "main" code of our library but is only visible to test code.
 */
fun tinyGroup(): GroupContext = tinyGroupContext

private fun Element.getCompat(other: GroupContext): UInt {
    context.assertCompatible(other)
    return when (this) {
        is TinyElementModP -> this.element
        is TinyElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

private class TinyGroupContext(
    val p: UInt,
    val q: UInt,
    val g: UInt,
    val r: UInt,
    val name: String,
) : GroupContext {
    val zeroModP: ElementModP
    val oneModP: ElementModP
    val twoModP: ElementModP
    val gModP: ElementModP
    val gInvModP by lazy { gPowP(qMinus1Q) }
    val gSquaredModP: ElementModP
    val qModP: ElementModP
    val zeroModQ: ElementModQ
    val oneModQ: ElementModQ
    val twoModQ: ElementModQ
    val dlogger: DLog by lazy { dLoggerOf(this) }
    val qMinus1Q: ElementModQ

    init {
        zeroModP = TinyElementModP(0U, this)
        oneModP = TinyElementModP(1U, this)
        twoModP = TinyElementModP(2U, this)
        gModP = TinyElementModP(g, this).acceleratePow()
        gSquaredModP = TinyElementModP((g * g) % p, this)
        qModP = TinyElementModP(q, this)
        zeroModQ = TinyElementModQ(0U, this)
        oneModQ = TinyElementModQ(1U, this)
        twoModQ = TinyElementModQ(2U, this)
        qMinus1Q = zeroModQ - oneModQ
    }

    override fun isProductionStrength() = false

    override val constants: ElectionConstants by
        lazy {
            ElectionConstants(p.toByteArray(), q.toByteArray(), r.toByteArray(), g.toByteArray(),)
        }

    override fun toString(): String = name

    override val ZERO_MOD_P: ElementModP
        get() = zeroModP
    override val ONE_MOD_P: ElementModP
        get() = oneModP
    override val TWO_MOD_P: ElementModP
        get() = twoModP
    override val G_MOD_P: ElementModP
        get() = gModP
    override val GINV_MOD_P: ElementModP
        get() = gInvModP
    override val G_SQUARED_MOD_P: ElementModP
        get() = gSquaredModP
    override val Q_MOD_P: ElementModP
        get() = qModP
    override val ZERO_MOD_Q: ElementModQ
        get() = zeroModQ
    override val ONE_MOD_Q: ElementModQ
        get() = oneModQ
    override val TWO_MOD_Q: ElementModQ
        get() = twoModQ
    override val MAX_BYTES_P: Int
        get() = 2
    override val MAX_BYTES_Q: Int
        get() = 2

    override fun isCompatible(ctx: GroupContext): Boolean = !ctx.isProductionStrength()

    /**
     * Convert a ByteArray, of arbitrary size, to a UInt, mod the given modulus. If the ByteArray
     * happens to be of size eight or less, the result is exactly the same as treating the ByteArray
     * as a big-endian integer and computing the modulus afterward. For anything longer, this method
     * uses the smallest 64 bits (i.e., the final eight bytes of the array) and ignores the rest.
     *
     * If the modulus is zero, it's ignored, and the intermediate value is truncated to a UInt and
     * returned.
     */
    private fun ByteArray.toUIntMod(modulus: UInt = 0U): UInt {
        val preModulus = this.fold(0UL) { prev, next -> ((prev shl 8) or next.toUByte().toULong()) }
        return if (modulus == 0U) {
            preModulus.toUInt()
        } else {
            (preModulus % modulus).toUInt()
        }
    }

    override fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val u32 = b.toUIntMod(p)
        val result = if (u32 < minimum.toUInt()) u32 + minimum.toUInt() else u32
        return uIntToElementModP(result)
    }

    override fun safeBinaryToElementModQ(
        b: ByteArray,
        minimum: Int,
    ): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }
        val u32 = b.toUIntMod(q)
        val result = if (u32 < minimum.toUInt()) u32 + minimum.toUInt() else u32
        return uIntToElementModQ(result)
    }

    override fun binaryToElementModP(b: ByteArray): ElementModP? {
        if (b.size > 4) return null // guaranteed to be out of bounds

        val u32: UInt = b.toUIntMod()
        return if (u32 >= p) null else uIntToElementModP(u32)
    }

    override fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        if (b.size > 4) return null // guaranteed to be out of bounds

        val u32: UInt = b.toUIntMod()
        return if (u32 >= q) null else uIntToElementModQ(u32)
    }

    override fun uIntToElementModQ(i: UInt): ElementModQ =
        if (i >= q)
            throw ArithmeticException("out of bounds for TestElementModQ: $i")
        else
            TinyElementModQ(i, this)

    override fun uIntToElementModP(i: UInt): ElementModP =
        if (i >= p)
            throw ArithmeticException("out of bounds for TestElementModP: $i")
        else
            TinyElementModP(i, this)

    override fun Iterable<ElementModQ>.addQ(): ElementModQ =
        uIntToElementModQ(fold(0U) { a, b -> (a + b.getCompat(this@TinyGroupContext)) % q })

    override fun Iterable<ElementModP>.multP(): ElementModP =
        uIntToElementModP(
            fold(1UL) { a, b -> (a * b.getCompat(this@TinyGroupContext).toULong()) % p }.toUInt()
        )

    override fun gPowP(e: ElementModQ): ElementModP = gModP powP e

    override fun dLog(p: ElementModP): Int? = dlogger.dLog(p)
}

private class TinyElementModP(val element: UInt, val groupContext: TinyGroupContext) : ElementModP {
    fun UInt.modWrap(): ElementModP = (this % groupContext.p).wrap()
    fun ULong.modWrap(): ElementModP = (this % groupContext.p).wrap()
    fun UInt.wrap(): ElementModP = TinyElementModP(this, groupContext)
    fun ULong.wrap(): ElementModP = toUInt().wrap()

    override fun isValidResidue(): Boolean {
        val residue =
            this powP TinyElementModQ(groupContext.q, groupContext) == groupContext.ONE_MOD_P
        return inBounds() && residue
    }

    override fun powP(e: ElementModQ): ElementModP {
        var result: ULong = 1U
        var base: ULong = element.toULong()
        val exp: UInt = e.getCompat(groupContext)

        // we know that all the bits above this are zero because q < 2^28
        (0..28)
            .forEach { bit ->
                val eBitSet = ((exp shr bit) and 1U) == 1U

                // We're doing arithmetic in the larger 64-bit space, but we'll never overflow
                // because the internal values are always mod p or q, and thus fit in 32 bits.
                if (eBitSet) result = (result * base) % groupContext.p
                base = (base * base) % groupContext.p
            }
        return result.wrap()
    }

    override fun times(other: ElementModP): ElementModP =
        (this.element.toULong() * other.getCompat(groupContext).toULong()).modWrap()

    override fun multInv(): ElementModP = this powP groupContext.qMinus1Q

    override fun div(denominator: ElementModP): ElementModP = this * denominator.multInv()

    // we could try to use the whole powradix thing, but it's overkill for 16-bit numbers
    override fun acceleratePow(): ElementModP = this

    override fun compareTo(other: ElementModP): Int =
        element.compareTo(other.getCompat(groupContext))

    override val context: GroupContext
        get() = groupContext

    override fun inBounds(): Boolean = element < groupContext.p

    override fun inBoundsNoZero(): Boolean = element > 0U && inBounds()

    override fun isZero(): Boolean = element == 0U

    override fun byteArray(): ByteArray = element.toByteArray()

    override fun equals(other: Any?): Boolean = other is TinyElementModP && element == other.element

    override fun hashCode(): Int = element.hashCode()

    override fun toString(): String = "ElementModP($element)"
}

private class TinyElementModQ(val element: UInt, val groupContext: TinyGroupContext) : ElementModQ {
    fun ULong.modWrap(): ElementModQ = (this % groupContext.q).wrap()
    fun UInt.modWrap(): ElementModQ = (this % groupContext.q).wrap()
    fun ULong.wrap(): ElementModQ = toUInt().wrap()
    fun UInt.wrap(): ElementModQ = TinyElementModQ(this, groupContext)

    override fun plus(other: ElementModQ): ElementModQ =
        (this.element + other.getCompat(groupContext)).modWrap()

    override fun minus(other: ElementModQ): ElementModQ =
        (this.element + (-other).getCompat(groupContext)).modWrap()

    override fun times(other: ElementModQ): ElementModQ =
        (this.element.toULong() * other.getCompat(groupContext).toULong()).modWrap()

    override operator fun unaryMinus(): ElementModQ =
        if (this == groupContext.zeroModQ) this else (groupContext.q - element).wrap()

    override fun multInv(): ElementModQ {
        // https://en.wikipedia.org/wiki/Extended_Euclidean_algorithm

        // This implementation is functional and lazy, which is kinda fun, although it's
        // going to allocate a bunch of memory and otherwise be much less efficient than
        // writing it as a vanilla while-loop. This function is rarely used in real code,
        // so efficient doesn't matter as much as correctness.

        // We're using Long, rather than UInt, to insure that we never experience over-
        // or under-flow. This allows the normalization of the result, which checks for
        // finalState.t < 0, to work correctly.

        data class State(val t: Long, val newT: Long, val r: Long, val newR: Long)
        val seq =
            generateSequence(State(0, 1, groupContext.q.toLong(), element.toLong()))
                { (t, newT, r, newR) ->
                    val quotient = r / newR
                    State(newT, t - quotient * newT, newR, r - quotient * newR)
                }

        val finalState = seq.find { it.newR == 0L } ?: throw Error("should never happen")

        if (finalState.r > 1) {
            throw ArithmeticException("element $element is not invertible")
        }

        return TinyElementModQ(
            (if (finalState.t < 0) finalState.t + groupContext.q.toLong() else finalState.t)
                .toUInt(),
            groupContext
        )
    }

    override fun div(denominator: ElementModQ): ElementModQ = this * denominator.multInv()

    override infix fun powQ(e: ElementModQ): ElementModQ {
        var result: ULong = 1U
        var base: ULong = element.toULong()
        val exp: UInt = e.getCompat(groupContext)

        // we know that all the bits above this are zero because q < 2^28
        (0..28)
            .forEach { bit ->
                val eBitSet = ((exp shr bit) and 1U) == 1U

                // We're doing arithmetic in the larger 64-bit space, but we'll never overflow
                // because the internal values are always mod p or q, and thus fit in 32 bits.
                if (eBitSet) result = (result * base) % groupContext.q
                base = (base * base) % groupContext.q
            }
        return result.wrap()
    }

    override fun compareTo(other: ElementModQ): Int =
        element.compareTo(other.getCompat(groupContext))

    override val context: GroupContext
        get() = groupContext

    override fun inBounds(): Boolean = element < groupContext.q

    override fun inBoundsNoZero(): Boolean = element > 0U && inBounds()

    override fun isZero(): Boolean = element == 0U

    override fun byteArray(): ByteArray = element.toByteArray()

    override fun equals(other: Any?): Boolean = other is TinyElementModQ && element == other.element

    override fun hashCode(): Int = element.hashCode()

    override fun toString(): String = "ElementModQ($element)"
}