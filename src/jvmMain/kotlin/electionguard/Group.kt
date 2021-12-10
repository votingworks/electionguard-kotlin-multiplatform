package electionguard

import electionguard.Base64.decodeFromBase64
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.math.BigInteger
import java.security.SecureRandom

internal val productionGroupContext =
    GroupContext(
        pBytes = b64ProductionP.decodeFromBase64(),
        qBytes = b64ProductionQ.decodeFromBase64(),
        gBytes = b64ProductionG.decodeFromBase64(),
        rBytes = b64ProductionR.decodeFromBase64(),
        strong = true,
        name = "production group"
    )

internal val testGroupContext =
    GroupContext(
        pBytes = b64TestP.decodeFromBase64(),
        qBytes = b64TestQ.decodeFromBase64(),
        gBytes = b64TestG.decodeFromBase64(),
        rBytes = b64TestR.decodeFromBase64(),
        strong = false,
        name = "16-bit test group"
    )

actual fun highSpeedProductionGroup() = productionGroupContext

actual fun lowMemoryProductionGroup() = productionGroupContext // for now

actual fun testGroup() = testGroupContext

/** Convert an array of bytes, in big-endian format, to a BigInteger */
internal fun UInt.toBigInteger() = BigInteger.valueOf(this.toLong())
internal fun ByteArray.toBigInteger() = BigInteger(1, this)

// TODO: add PowRadix

actual class GroupContext(
    pBytes: ByteArray,
    qBytes: ByteArray,
    gBytes: ByteArray,
    rBytes: ByteArray,
    strong: Boolean,
    val name: String
) {
    val p: BigInteger
    val q: BigInteger
    val g: BigInteger
    val r: BigInteger
    val zeroModP: ElementModP
    val oneModP: ElementModP
    val twoModP: ElementModP
    val gModP: ElementModP
    val gSquaredModP: ElementModP
    val qModP: ElementModP
    val zeroModQ: ElementModQ
    val oneModQ: ElementModQ
    val twoModQ: ElementModQ
    val productionStrength: Boolean = strong

    init {
        p = pBytes.toBigInteger()
        q = qBytes.toBigInteger()
        g = gBytes.toBigInteger()
        r = rBytes.toBigInteger()
        zeroModP = ElementModP(0U.toBigInteger(), this)
        oneModP = ElementModP(1U.toBigInteger(), this)
        twoModP = ElementModP(2U.toBigInteger(), this)
        gModP = ElementModP(g, this)
        gSquaredModP = ElementModP((g * g) % p, this)
        qModP = ElementModP(q, this)
        zeroModQ = ElementModQ(0U.toBigInteger(), this)
        oneModQ = ElementModQ(1U.toBigInteger(), this)
        twoModQ = ElementModQ(2U.toBigInteger(), this)
    }

    actual fun isProductionStrength() = productionStrength

    actual fun toJson(): JsonElement = JsonObject(mapOf()) // fixme

    override fun toString() = toJson().toString()

    actual val ZERO_MOD_P
        get() = zeroModP

    actual val ONE_MOD_P
        get() = oneModP

    actual val TWO_MOD_P
        get() = twoModP

    actual val G_MOD_P
        get() = gModP

    actual val G_SQUARED_MOD_P
        get() = gSquaredModP

    actual val Q_MOD_P
        get() = qModP

    actual val ZERO_MOD_Q
        get() = zeroModQ

    actual val ONE_MOD_Q
        get() = oneModQ

    actual val TWO_MOD_Q
        get() = twoModQ

    actual fun isCompatible(ctx: GroupContext) = this.productionStrength == ctx.productionStrength

    actual fun isCompatible(json: JsonElement): Boolean {
        throw NotImplementedError()
    }

    actual fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        val result = when {
            minimum < 0 ->
                throw IllegalArgumentException("minimum $minimum may not be negative")
            minimum == 0 ->
                ElementModP(b.toBigInteger() % p, this)
            else ->
                minimum.toBigInteger().let { mv ->
                    ElementModP(mv + b.toBigInteger() % (p - mv), this)
                }
        }

        assert(result.inBounds()) { "result not in bounds! ${result.element}" }
        return result
    }

    actual fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        val result = when {
            minimum < 0 ->
                throw IllegalArgumentException("minimum $minimum may not be negative")
            minimum == 0 ->
                ElementModQ(b.toBigInteger() % q, this)
            else ->
                minimum.toBigInteger().let { mv ->
                    ElementModQ(mv + b.toBigInteger() % (q - mv), this)
                }
        }

        assert(result.inBounds()) { "result not in bounds! ${result.element}" }
        return result
    }

    actual fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = b.toBigInteger()
        return if (tmp >= p) null else ElementModP(tmp, this)
    }

    actual fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = b.toBigInteger()
        return if (tmp >= q) null else ElementModQ(tmp, this)
    }

    actual fun gPowP(e: Int) = when(e) {
        0 -> oneModP
        1 -> gModP
        2 -> gSquaredModP
        else -> gPowP(e.toElementModQ(this))
    }

    actual fun gPowP(e: ElementModQ) = gModP.powP(e)

    actual fun randRangeQ(minimum: Int): ElementModQ {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)

        val minimumBig = BigInteger.valueOf(minimum.toLong())
        return ElementModQ(
            minimumBig + bytes.toBigInteger() % (q - minimumBig),
            this)
    }
}

internal fun Element.getCompat(context: GroupContext) =
    if (this.context.isCompatible(context))
        when(this) {
            is ElementModP -> this.element
            is ElementModQ -> this.element
            else -> throw NotImplementedError("should only be two kinds of elements")
        }
    else
        throw ArithmeticException("cannot mix and match incompatible element contexts")

actual class ElementModQ(val element: BigInteger, val groupContext: GroupContext): Element, Comparable<ElementModQ> {
    internal fun BigInteger.wrap(): ElementModQ = ElementModQ(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element >= groupContext.ZERO_MOD_Q.element && element < groupContext.q

    override fun inBoundsNoZero() = element > groupContext.ZERO_MOD_Q.element && element < groupContext.q

    override fun byteArray(): ByteArray = element.toByteArray()

    actual override operator fun compareTo(other: ElementModQ): Int = element.compareTo(other.getCompat(groupContext))

    actual operator fun plus(other: ElementModQ) =
        ((this.element + other.getCompat(groupContext)) % groupContext.q).wrap()

    actual operator fun minus(other: ElementModQ) =
        ((this.element - other.getCompat(groupContext)) % groupContext.q).wrap()

    actual operator fun times(other: ElementModQ) =
        ((this.element * other.getCompat(groupContext)) % groupContext.q).wrap()

    actual fun multInv() = element.modInverse(groupContext.q).wrap()

    actual operator fun unaryMinus() = (groupContext.q - element).wrap()

    actual infix operator fun div(denominator: ElementModQ) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.q)).wrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModQ -> other.element == this.element && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)
}

actual class ElementModP(val element: BigInteger, val groupContext: GroupContext): Element, Comparable<ElementModP> {
    internal fun BigInteger.wrap(): ElementModP = ElementModP(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element >= groupContext.ZERO_MOD_P.element && element < groupContext.p

    override fun inBoundsNoZero() = element > groupContext.ZERO_MOD_P.element && element < groupContext.p

    override fun byteArray(): ByteArray = element.toByteArray()

    actual override operator fun compareTo(other: ElementModP): Int = element.compareTo(other.getCompat(groupContext))

    actual fun isValidResidue(): Boolean {
        val residue = this.element.modPow(groupContext.q, groupContext.p) == groupContext.ONE_MOD_P.element
        return inBounds() && residue
    }

    actual infix fun powP(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.p).wrap()

    actual operator fun times(other: ElementModP) =
        ((this.element * other.getCompat(groupContext)) % groupContext.p).wrap()

    actual fun multInv() = element.modInverse(groupContext.p).wrap()

    actual infix operator fun div(denominator: ElementModP) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.p)).wrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModP -> other.element == this.element && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)
}


actual fun Iterable<ElementModQ>.addQ(): ElementModQ {
    val input = iterator().asSequence().toList()
    if (input.isEmpty()) {
        throw ArithmeticException("addQ not defined on empty lists")
    }

    // We're going to mutate the state of the result; it starts with the
    // first entry in the list, and then we add each subsequent entry.
    val context = input[0].groupContext

    val result = input.subList(1, input.count()).fold(input[0].element) { a, b ->
        if (!context.isCompatible(b.groupContext)) {
            throw ArithmeticException("addQ on a inputs with incompatible group contexts")
        }
        (a + b.element) % context.q
    }

    return ElementModQ(result, context)
}

actual fun Iterable<ElementModP>.multP(): ElementModP {
    val input = iterator().asSequence().toList()
    if (input.isEmpty()) {
        throw ArithmeticException("multP not defined on empty lists")
    }
    if (input.count() == 1) {
        return input[0]
    }

    // We're going to mutate the state of the result; it starts with the
    // first entry in the list, and then we add each subsequent entry.
    val context = input[0].groupContext

    val result = input.subList(1, input.count()).fold(input[0].element) { a, b ->
        if (!context.isCompatible(b.groupContext)) {
            throw ArithmeticException("multP on a inputs with incompatible group contexts")
        }
        (a * b.element) % context.p
    }

    return ElementModP(result, context)
}

actual fun UInt.toElementModQ(ctx: GroupContext) : ElementModQ = when (this) {
    0U -> ctx.ZERO_MOD_Q
    1U -> ctx.ONE_MOD_Q
    2U -> ctx.TWO_MOD_Q
    else -> ElementModQ(this.toBigInteger(), ctx)
}

actual fun UInt.toElementModP(ctx: GroupContext) : ElementModP = when (this) {
    0U -> ctx.ZERO_MOD_P
    1U -> ctx.ONE_MOD_P
    2U -> ctx.TWO_MOD_P
    else -> ElementModP(this.toBigInteger(), ctx)
}
