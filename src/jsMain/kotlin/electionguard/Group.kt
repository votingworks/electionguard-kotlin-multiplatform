package electionguard

import electionguard.Base64.decodeFromBase64
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.gciatto.kt.math.BigInteger

// This implementation uses kt-math (https://github.com/gciatto/kt-math), which is something
// of a port of Java's BigInteger. It's not terribly fast, but it at least seems to give
// correct answers. And, unsurprisingly, this code is *almost* but not exactly the same
// as the JVM code. This really needs to be replaced with something that will be performant,
// probably using WASM. The "obvious" choices are:
//
// - GMP-WASM (https://github.com/Daninet/gmp-wasm)
//   (Kotlin's "Dukat" TypeScript interface extraction completely fails on this, which is sad.)
//
// - HACL-WASM (https://github.com/project-everest/hacl-star/tree/master/bindings/js#readme)
//   (Hash many other HACL features, but doesn't expose any of the BigInt-related types.)
//
// But, for now, JS will at least "work".

internal val productionGroupContext =
    GroupContext(
        pBytes = b64ProductionP.decodeFromBase64(),
        qBytes = b64ProductionQ.decodeFromBase64(),
        gBytes = b64ProductionG.decodeFromBase64(),
        rBytes = b64ProductionR.decodeFromBase64(),
        strong = true,
        name = "production group, no acceleration"
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

actual fun productionGroup(acceleration: PowRadixOption): GroupContext {
    return productionGroupContext // for now
}

actual fun testGroup() = testGroupContext

/** Convert an array of bytes, in big-endian format, to a BigInteger */
internal fun UInt.toBigInteger() = BigInteger.of(this.toLong())
internal fun ByteArray.toBigInteger() = BigInteger(1, this)

actual class GroupContext(pBytes: ByteArray, qBytes: ByteArray, gBytes: ByteArray, rBytes: ByteArray, strong: Boolean, val name: String) {
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
        val tmp = g*g
        val tmp2 = tmp % p
        gSquaredModP = ElementModP(tmp2, this)
        qModP = ElementModP(q, this)
        zeroModQ = ElementModQ(0U.toBigInteger(), this)
        oneModQ = ElementModQ(1U.toBigInteger(), this)
        twoModQ = ElementModQ(2U.toBigInteger(), this)
    }


    actual fun toJson(): JsonElement = JsonObject(mapOf()) // fixme

    actual fun isProductionStrength() = productionStrength

    actual val ZERO_MOD_P: ElementModP
        get() = zeroModP

    actual val ONE_MOD_P: ElementModP
        get() = oneModP

    actual val TWO_MOD_P: ElementModP
        get() = twoModP

    actual val G_MOD_P: ElementModP
        get() = gModP

    actual val G_SQUARED_MOD_P: ElementModP
        get() = gSquaredModP

    actual val Q_MOD_P: ElementModP
        get() = qModP

    actual val ZERO_MOD_Q: ElementModQ
        get() = zeroModQ

    actual val ONE_MOD_Q: ElementModQ
        get() = oneModQ

    actual val TWO_MOD_Q: ElementModQ
        get() = twoModQ

    actual fun isCompatible(ctx: GroupContext) = productionStrength == ctx.productionStrength

    actual fun isCompatible(json: JsonElement): Boolean {
        TODO("Not yet implemented")
    }

    actual fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger() % p

        val mv = BigInteger.of(minimum)
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModP(tmp2, this)

        return result
    }

    actual fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger() % q

        val mv = BigInteger.of(minimum)
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModQ(tmp2, this)

        return result
    }


    /*
    actual fun safeBinaryToElementModP(b: ByteArray) =
        ElementModP(b.toBigInteger() % p, this)

    actual fun safeBinaryToElementModQ(b: ByteArray) =
        ElementModQ(b.toBigInteger() % q, this)
     */

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
        if (minimum < 0)
            throw IllegalArgumentException("minimum $minimum must be greater than zero")

        // https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues
        // This code is claimed to be inappropriate for generating keys, because it
        // might not be running in a "secure context" (i.e., if there's hostile JavaScript
        // on the page, then it could nuke these methods and cause something else to run).

        // If ElectionGuard is used in a browser context, this would most likely impact
        // the way the seed is generated. Under the threat model where we've got malicious
        // JavaScript dorking with a web page, they could do a whole lot more than just
        // changing the seed, so this isn't something to really stress out about.

        // Conversely, if ElectionGuard is running in a server context (node.js, etc.),
        // then we're not worried about hostile content, and this should be just fine.

        // Also, note that Kotlin should support this directly, but doesn't. Yet.
        // https://youtrack.jetbrains.com/issue/KT-43322

        val bytes = ByteArray(32)

        // returns an object of type Crypto, of which Kotlin only knows it's "dynamic"
        val crypto = js("crypto")

        // Kotlin allows us to call methods on a "dyanmic" object without any typechecking!
        crypto.getRandomValues(bytes)

        // And now those bytes have been overwritten, so we can process them normally
        return safeBinaryToElementModQ(bytes, minimum)
    }
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
        ((element * denominator.getCompat(groupContext).modInverse(groupContext.q)) % groupContext.q).wrap()

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
        ((element * denominator.getCompat(groupContext).modInverse(groupContext.p)) % groupContext.p).wrap()

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
        (a + b.getCompat(context)) % context.q
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
        (a * b.getCompat(context)) % context.p
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