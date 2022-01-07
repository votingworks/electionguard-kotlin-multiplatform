package electionguard

import kotlinx.serialization.json.JsonElement

private val testGroupContext =
    TestGroupContext(
        p = intTestP.toUInt(),
        q = intTestQ.toUInt(),
        r = intTestR.toUInt(),
        g = intTestG.toUInt(),
        name = "16-bit test group",
        powRadixOption = PowRadixOption.NO_ACCELERATION
    )

/**
 * Fetches the [GroupContext] suitable for unit tests and such, where everything is a 16-bit number
 * on the inside, so it runs really fast, but is (of course) completely insecure.
 */
fun testGroup(): GroupContext = testGroupContext

private fun Element.getCompat(other: GroupContext): UInt {
    context.assertCompatible(other)
    return when (this) {
        is TestElementModP -> this.element
        is TestElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

class TestGroupContext(
    val p: UInt,
    val q: UInt,
    val g: UInt,
    val r: UInt,
    val name: String,
    val powRadixOption: PowRadixOption
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
    val dlogger: TestDLog
    val qMinus1Q: ElementModQ

    init {
        zeroModP = TestElementModP(0U, this)
        oneModP = TestElementModP(1U, this)
        twoModP = TestElementModP(2U, this)
        gModP = TestElementModP(g, this).acceleratePow()
        gSquaredModP = TestElementModP((g * g) % p, this)
        qModP = TestElementModP(q, this)
        zeroModQ = TestElementModQ(0U, this)
        oneModQ = TestElementModQ(1U, this)
        twoModQ = TestElementModQ(2U, this)
        dlogger = TestDLog(this)
        qMinus1Q = zeroModQ - oneModQ
    }

    override fun isProductionStrength() = false

    override fun toJson(): JsonElement {
        TODO("Not yet implemented")
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

    override fun isCompatible(json: JsonElement): Boolean {
        TODO("Not yet implemented")
    }

    override fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val u16: UInt =
            when (b.size) {
                0 -> 0U
                1 -> b[0].toUInt() % p
                2 -> ((b[0].toUInt() shl 8) or b[1].toUInt()) % p
                else -> {
                    // time for a total hack, since our goal is to output *something*
                    b.fold(0U) { prev, next -> ((prev shl 8) or next.toUInt()) % p }
                }
            }

        val result = if (u16 < minimum.toUInt()) u16 + minimum.toUInt() else u16
        return uIntToElementModP(result)
    }

    override fun safeBinaryToElementModQ(
        b: ByteArray,
        minimum: Int,
        maxQMinus1: Boolean
    ): ElementModQ {
        val modulus = if (maxQMinus1) (q - 1U) else q
        val u16: UInt =
            when (b.size) {
                0 -> 0U
                1 -> b[0].toUInt() % modulus
                2 -> ((b[0].toUInt() shl 8) or b[1].toUInt()) % modulus
                else -> {
                    b.fold(0U) { prev, next -> ((prev shl 8) or next.toUInt()) % modulus }
                }
            }

        val result = if (u16 < minimum.toUInt()) u16 + minimum.toUInt() else u16
        return uIntToElementModQ(result)
    }

    override fun binaryToElementModP(b: ByteArray): ElementModP? {
        val u16: UInt =
            when (b.size) {
                0 -> 0U
                1 -> b[0].toUInt()
                2 -> ((b[0].toUInt() shl 8) or b[1].toUInt())
                else -> return null
            }

        if (u16 >= p) return null

        return uIntToElementModP(u16)
    }

    override fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val u16: UInt =
            when (b.size) {
                0 -> 0U
                1 -> b[0].toUInt()
                2 -> ((b[0].toUInt() shl 8) or b[1].toUInt())
                else -> return null
            }

        if (u16 >= q) return null

        return uIntToElementModQ(u16)
    }

    override fun uIntToElementModQ(i: UInt): ElementModQ =
        if (i >= q)
            throw ArithmeticException("out of bounds for TestElementModQ: $i")
        else
            TestElementModQ(i, this)

    override fun uIntToElementModP(i: UInt): ElementModP =
        if (i >= p)
            throw ArithmeticException("out of bounds for TestElementModP: $i")
        else
            TestElementModP(i, this)

    override fun Iterable<ElementModQ>.addQ(): ElementModQ =
        uIntToElementModQ(fold(0U) { a, b -> (a + b.getCompat(this@TestGroupContext)) % q })

    override fun Iterable<ElementModP>.multP(): ElementModP =
        uIntToElementModP(fold(1U) { a, b -> (a * b.getCompat(this@TestGroupContext)) % p })

    override fun gPowP(e: ElementModQ): ElementModP = gModP powP e

    override fun dLog(p: ElementModP): Int? = dlogger.dLog(p)
}

class TestElementModP(val element: UInt, val groupContext: TestGroupContext) : ElementModP {
    internal fun UInt.modWrap(): ElementModP = (this % groupContext.p).wrap()
    internal fun UInt.wrap(): ElementModP = TestElementModP(this, groupContext)

    override fun isValidResidue(): Boolean {
        val residue =
            this powP TestElementModQ(groupContext.q, groupContext) == groupContext.ONE_MOD_P
        return inBounds() && residue
    }

    override fun powP(e: ElementModQ): ElementModP {
        var result: UInt = 1U
        var base: UInt = element
        val exp: UInt = e.getCompat(groupContext)
        (0..16)
            .forEach { bit ->
                val eBitSet = ((exp shr bit) and 1U) == 1U
                if (eBitSet) result = (result * base) % groupContext.p
                base = (base * base) % groupContext.p
            }
        return result.wrap()
    }

    override fun times(other: ElementModP): ElementModP =
        (this.element * other.getCompat(groupContext)).modWrap()

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

    override fun byteArray(): ByteArray =
        byteArrayOf(((element shr 8) and 0xffU).toByte(), (element and 0xffU).toByte())

    override fun equals(other: Any?): Boolean = other is TestElementModP && element == other.element

    override fun hashCode(): Int = element.hashCode()

    override fun toString(): String = "ElementModP($element)"
}

class TestElementModQ(val element: UInt, val groupContext: TestGroupContext) : ElementModQ {
    internal fun UInt.modWrap(): ElementModQ = (this % groupContext.q).wrap()
    internal fun UInt.wrap(): ElementModQ = TestElementModQ(this, groupContext)

    override fun plus(other: ElementModQ): ElementModQ =
        (this.element + other.getCompat(groupContext)).modWrap()

    override fun minus(other: ElementModQ): ElementModQ =
        (this.element + (-other).getCompat(groupContext)).modWrap()

    override fun times(other: ElementModQ): ElementModQ =
        (this.element * other.getCompat(groupContext)).modWrap()

    override operator fun unaryMinus(): ElementModQ =
        if (this == groupContext.zeroModQ) this else (groupContext.q - element).wrap()

    override fun compareTo(other: ElementModQ): Int =
        element.compareTo(other.getCompat(groupContext))

    override val context: GroupContext
        get() = groupContext

    override fun inBounds(): Boolean = element < groupContext.q

    override fun inBoundsNoZero(): Boolean = element > 0U && inBounds()

    override fun isZero(): Boolean = element == 0U

    override fun byteArray(): ByteArray =
        byteArrayOf(((element shr 8) and 0xffU).toByte(), (element and 0xffU).toByte())

    override fun equals(other: Any?): Boolean = other is TestElementModQ && element == other.element

    override fun hashCode(): Int = element.hashCode()

    override fun toString(): String = "ElementModQ($element)"
}