package electionguard

import electionguard.Base64.fromSafeBase64
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.math.BigInteger

private val testGroupContext =
    GroupContext(
        pBytes = b64TestP.fromSafeBase64(),
        qBytes = b64TestQ.fromSafeBase64(),
        gBytes = b64TestG.fromSafeBase64(),
        rBytes = b64TestR.fromSafeBase64(),
        strong = false,
        name = "16-bit test group",
        powRadixOption = PowRadixOption.NO_ACCELERATION
    )

private val productionGroups: HashMap<PowRadixOption, GroupContext> =
    hashMapOf(
        PowRadixOption.NO_ACCELERATION to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, no acceleration",
                    powRadixOption = PowRadixOption.NO_ACCELERATION
                ),
        PowRadixOption.LOW_MEMORY_USE to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, low memory use",
                    powRadixOption = PowRadixOption.LOW_MEMORY_USE
                ),
        PowRadixOption.HIGH_MEMORY_USE to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, high memory use",
                    powRadixOption = PowRadixOption.HIGH_MEMORY_USE
                ),
        PowRadixOption.EXTREME_MEMORY_USE to
                GroupContext(
                    pBytes = b64ProductionP.fromSafeBase64(),
                    qBytes = b64ProductionQ.fromSafeBase64(),
                    gBytes = b64ProductionG.fromSafeBase64(),
                    rBytes = b64ProductionR.fromSafeBase64(),
                    strong = true,
                    name = "production group, extreme memory use",
                    powRadixOption = PowRadixOption.EXTREME_MEMORY_USE
                )
    )

actual fun productionGroup(acceleration: PowRadixOption) : GroupContext =
    productionGroups[acceleration] ?: throw Error("can't happen")

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
    val name: String,
    val powRadixOption: PowRadixOption
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
    val gPowRadix: Lazy<PowRadix>
    val dlogger: DLog

    init {
        p = pBytes.toBigInteger()
        q = qBytes.toBigInteger()
        g = gBytes.toBigInteger()
        r = rBytes.toBigInteger()
        zeroModP = ElementModP(0U.toBigInteger(), this)
        oneModP = ElementModP(1U.toBigInteger(), this)
        twoModP = ElementModP(2U.toBigInteger(), this)
        gModP = ElementModP(g, this)
        gSquaredModP = ElementModP((g * g).mod(p), this)
        qModP = ElementModP(q, this)
        zeroModQ = ElementModQ(0U.toBigInteger(), this)
        oneModQ = ElementModQ(1U.toBigInteger(), this)
        twoModQ = ElementModQ(2U.toBigInteger(), this)
        gPowRadix = lazy { PowRadix(G_MOD_P, powRadixOption) }
        dlogger = DLog(this)
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
        if(minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger().mod(p)

        val mv = minimum.toBigInteger()
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModP(tmp2, this)

        return result
    }

    actual fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if(minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger().mod(q)

        val mv = minimum.toBigInteger()
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModQ(tmp2, this)

        return result
    }

    actual fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = b.toBigInteger()
        return if (tmp >= p || tmp < BigInteger.ZERO) null else ElementModP(tmp, this)
    }

    actual fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = b.toBigInteger()
        return if (tmp >= q || tmp < BigInteger.ZERO) null else ElementModQ(tmp, this)
    }

    actual fun gPowP(e: ElementModQ) = gPowRadix.value.pow(e)

    actual fun dLog(p: ElementModP): Int? = dlogger.dLog(p)
}

internal fun Element.getCompat(other: GroupContext): BigInteger {
    context.assertCompatible(other)
    return when (this) {
        is ElementModP -> this.element
        is ElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

actual class ElementModQ(val element: BigInteger, val groupContext: GroupContext): Element, Comparable<ElementModQ> {
    internal fun BigInteger.modWrap(): ElementModQ = this.mod(groupContext.q).wrap()
    internal fun BigInteger.wrap(): ElementModQ = ElementModQ(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element >= BigInteger.ZERO && element < groupContext.q

    override fun inBoundsNoZero() = element > BigInteger.ZERO && element < groupContext.q

    override fun byteArray(): ByteArray = element.toByteArray()

    actual override operator fun compareTo(other: ElementModQ): Int = element.compareTo(other.getCompat(groupContext))

    actual operator fun plus(other: ElementModQ) =
        (this.element + other.getCompat(groupContext)).modWrap()

    actual operator fun minus(other: ElementModQ) =
        (this.element - other.getCompat(groupContext)).modWrap()

    actual operator fun times(other: ElementModQ) =
        (this.element * other.getCompat(groupContext)).modWrap()

    actual fun multInv() = element.modInverse(groupContext.q).wrap()

    actual operator fun unaryMinus() = (groupContext.q - element).wrap()

    actual infix operator fun div(denominator: ElementModQ) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.q)).modWrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModQ -> other.element == this.element && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)
}

actual open class ElementModP(val element: BigInteger, val groupContext: GroupContext): Element, Comparable<ElementModP> {
    internal fun BigInteger.modWrap(): ElementModP = this.mod(groupContext.p).wrap()
    internal fun BigInteger.wrap(): ElementModP = ElementModP(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element >= BigInteger.ZERO && element < groupContext.p

    override fun inBoundsNoZero() = element > BigInteger.ZERO && element < groupContext.p

    override fun byteArray(): ByteArray = element.toByteArray()

    actual override operator fun compareTo(other: ElementModP): Int = element.compareTo(other.getCompat(groupContext))

    actual fun isValidResidue(): Boolean {
        val residue = this.element.modPow(groupContext.q, groupContext.p) == groupContext.ONE_MOD_P.element
        return inBounds() && residue
    }

    actual infix open fun powP(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.p).wrap()

    actual operator fun times(other: ElementModP) =
        (this.element * other.getCompat(groupContext)).modWrap()

    actual fun multInv() = element.modInverse(groupContext.p).wrap()

    actual infix operator fun div(denominator: ElementModP) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.p)).modWrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModP -> other.element == this.element && other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)

    actual open fun acceleratePow() : ElementModP =
        AcceleratedElementModP(PowRadix(this, groupContext.powRadixOption), element, groupContext)
}

class AcceleratedElementModP(
    val powRadix: PowRadix,
    element: BigInteger,
    groupContext: GroupContext
) : ElementModP(element, groupContext) {
    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
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
        (a + b.getCompat(context)).mod(context.q)
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
        (a * b.getCompat(context)).mod(context.p)
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
