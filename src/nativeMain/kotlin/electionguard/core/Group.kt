package electionguard.core

import electionguard.core.Base64.fromSafeBase64
import electionguard.core.Base64.toBase64
import electionguard.ballot.ElectionConstants
import hacl.*
import kotlinx.cinterop.*
import platform.posix.free
import platform.posix.uint64_tVar

private val productionGroups4096 =
    PowRadixOption.values().associateWith {
        ProductionGroupContext(
            pBytes = b64Production4096P.fromSafeBase64(),
            qBytes = b64Production4096Q.fromSafeBase64(),
            p256minusQBytes = b64Production4096P256MinusQ.fromSafeBase64(),
            gBytes = b64Production4096G.fromSafeBase64(),
            rBytes = b64Production4096R.fromSafeBase64(),
            name = "production group, ${it.description}, 4096 bits",
            powRadixOption = it,
            productionMode = ProductionMode.Mode4096,
            numPBits = intProduction4096PBits.toUInt(),
            numPBytes = intProduction4096PBits.toUInt() / 8U,
            numPLWords = intProduction4096PBits.toUInt() / 64U,
        )
    }

private val productionGroups3072 =
    PowRadixOption.values().associateWith {
        ProductionGroupContext(
            pBytes = b64Production3072P.fromSafeBase64(),
            qBytes = b64Production3072Q.fromSafeBase64(),
            p256minusQBytes = b64Production3072P256MinusQ.fromSafeBase64(),
            gBytes = b64Production3072G.fromSafeBase64(),
            rBytes = b64Production3072R.fromSafeBase64(),
            name = "production group, ${it.description}, 3072 bits",
            powRadixOption = it,
            productionMode = ProductionMode.Mode3072,
            numPBits = intProduction3072PBits.toUInt(),
            numPBytes = intProduction3072PBits.toUInt() / 8U,
            numPLWords = intProduction3072PBits.toUInt() / 64U,
        )
    }

actual fun productionGroup(acceleration: PowRadixOption, mode: ProductionMode) : GroupContext =
    when(mode) {
        ProductionMode.Mode4096 -> productionGroups4096[acceleration] ?: throw Error("can't happen")
        ProductionMode.Mode3072 -> productionGroups3072[acceleration] ?: throw Error("can't happen")
    }

typealias HaclBignumP = LongArray
typealias HaclBignumQ = LongArray

internal const val HaclBignumQ_LongWords = 4
internal const val HaclBignumQ_Bytes = HaclBignumQ_LongWords * 8

internal fun newZeroBignumP(mode: ProductionMode) = HaclBignumP(mode.numLongWordsInP)

internal fun newZeroBignumQ() = HaclBignumQ(HaclBignumQ_LongWords)

// Helper functions that make it less awful to go back and forth from Kotlin to C interaction.
// What's going on here: before we can pass a pointer from a Kotlin-managed LongArray into
// a C function, we need to "pin" it in memory, guaranteeing that a hypothetical copying
// garbage collector won't come along and relocate that buffer while we were sending a
// raw pointer to it into the C library. That's what usePinned() is all about. At least in
// how we're using HACL, we're relying on these functions being stateless. We know that
// whatever we pass in won't be retained, so once the call returns, the pinning is no
// longer necessary.

internal inline fun <T> nativeElems(a: LongArray,
                                    b: LongArray,
                                    c: LongArray,
                                    d: LongArray,
                                    e: LongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>, cp: CPointer<ULongVar>, dp: CPointer<ULongVar>, ep: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> c.useNative { cp -> d.useNative { dp -> e.useNative { ep -> f(ap, bp, cp, dp, ep) } } } } }

internal inline fun <T> nativeElems(a: LongArray,
                                    b: LongArray,
                                    c: LongArray,
                                    d: LongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>, cp: CPointer<ULongVar>, dp: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> c.useNative { cp -> d.useNative { dp -> f(ap, bp, cp, dp) } } } }

internal inline fun <T> nativeElems(a: LongArray,
                                    b: LongArray,
                                    c: LongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>, cp: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> c.useNative { cp -> f(ap, bp, cp) } } }

internal inline fun <T> nativeElems(a: LongArray,
                                    b: LongArray,
                                    f: (ap: CPointer<ULongVar>, bp: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> b.useNative { bp -> f(ap, bp) } }

internal inline fun <T> nativeElems(a: LongArray,
                                    f: (ap: CPointer<ULongVar>) -> T): T =
    a.useNative { ap -> f(ap) }

internal inline fun <T> LongArray.useNative(f: (CPointer<ULongVar>) -> T): T =
    usePinned { ptr ->
        f(ptr.addressOf(0).reinterpret())
    }

internal inline fun <T> ByteArray.useNative(f: (CPointer<UByteVar>) -> T): T =
    usePinned { ptr ->
        f(ptr.addressOf(0).reinterpret())
    }


internal fun UInt.toHaclBignumQ(): HaclBignumQ = toByteArray().toHaclBignumQ()

internal fun UInt.toHaclBignumP(mode: ProductionMode): HaclBignumP = toByteArray().toHaclBignumP(mode = mode)

/** Convert an array of bytes, in big-endian format, to a HaclBignum256. */
internal fun ByteArray.toHaclBignumQ(doubleMemory: Boolean = false): HaclBignumQ {
    // See detailed comments in ByteArray.toHaclBignumP() for details on
    // what's going on here.
    val bytesToUse = when {
        size == HaclBignumQ_Bytes -> this
        size == HaclBignumQ_Bytes + 1 && this[0] == 0.toByte() ->
            ByteArray(HaclBignumQ_Bytes) { i -> this[i+1] }
        size > HaclBignumQ_Bytes ->
            throw IllegalArgumentException("ByteArray size $size is too big for $HaclBignumQ_Bytes")

        else -> {
            // leading padding with zero
            val delta = HaclBignumQ_Bytes - size
            ByteArray(HaclBignumQ_Bytes) { i ->
                if (i < delta) 0 else this[i - delta]
            }
        }
    }
    bytesToUse.useNative { bytes ->
        val tmp: CPointer<ULongVar>? =
            Hacl_Bignum256_new_bn_from_bytes_be(HaclBignumQ_Bytes.convert(), bytes)
        if (tmp == null) {
            throw OutOfMemoryError()
        }

        // make a copy to Kotlin-managed memory and free the Hacl-managed original
        val result = LongArray((if (doubleMemory) 2 else 1) * HaclBignumQ_LongWords) {
            if (it >= HaclBignumQ_LongWords)
                0L
            else
                tmp[it].convert()
        }
        free(tmp)
        return result
    }
}

/** Convert an array of bytes, in big-endian format, to a HaclBignumP. */
internal fun ByteArray.toHaclBignumP(
    doubleMemory: Boolean = false,
    mode: ProductionMode
): HaclBignumP {
    // This code, as well as ByteArray.toHaclBignumQ() is making a bunch
    // of copies. We do a first copy to get the input to exactly the right
    // length, padding or chopping zeros as necessary. Then HACL makes a
    // copy into memory that it allocated, rearranging the bytes as necessary
    // for its internal representation. Then, we make a copy of *that* into
    // memory that's managed by Kotlin, which allows the Kotlin native runtime
    // (which has a GC implementation that's evolving over time) to dispose
    // of it when it's no longer in use.

    // This might seem like it's really awful, but it's only happening when
    // we're reading this data in from an external source. What really matters
    // for performance is when we're doing arithmetic, and there we're doing
    // all the right things to avoid unnecessary copies.

    val numBytes = mode.numBytesInP
    val numLongWords = mode.numLongWordsInP

    val bytesToUse = when {
        size == numBytes -> this
        size == numBytes + 1 && this[0] == 0.toByte() ->
            ByteArray(numBytes) { i -> this[i+1] }
        size > numBytes ->
            throw IllegalArgumentException("ByteArray size $size is too big for $numBytes ($mode)")

        else -> {
            // leading padding with zero
            val delta = numBytes - size
            ByteArray(numBytes) { i ->
                if (i < delta) 0 else this[i - delta]
            }
        }
    }
    bytesToUse.useNative { bytes ->
        val tmp: CPointer<uint64_tVar> =
            Hacl_Bignum64_new_bn_from_bytes_be(numBytes.convert(), bytes) ?: throw OutOfMemoryError()

        // make a copy to Kotlin-managed memory and free the Hacl-managed original
        val result = LongArray((if (doubleMemory) 2 else 1) * numLongWords) {
            if (it >= numLongWords)
                0L
            else
                tmp[it].convert()
        }
        free(tmp)
        return result
    }
}

/** Returns true if the given element is strictly less than the other */
internal infix fun HaclBignumQ.ltQ(other: HaclBignumQ): Boolean {
    nativeElems(this, other) { a, b ->
        val aLtB = Hacl_Bignum256_lt_mask(a, b) != 0UL
        return aLtB
    }
}

/** Returns true if the given element is strictly greater than the other */
internal infix fun HaclBignumQ.gtQ(other: HaclBignumQ): Boolean {
    nativeElems(this, other) { a, b ->
        val bLtA = Hacl_Bignum256_lt_mask(b, a) != 0UL
        return bLtA
    }
}

/** Returns true if the given element is strictly less than the other */
internal fun HaclBignumP.ltP(other: HaclBignumP, mode: ProductionMode): Boolean {
    nativeElems(this, other) { a, b ->
        val aLtB = Hacl_Bignum64_lt_mask(mode.numLongWordsInP.toUInt(), a, b) != 0UL
        return aLtB
    }
}

/** Returns true if the given element is strictly greater than the other */
internal fun HaclBignumP.gtP(other: HaclBignumP, mode: ProductionMode): Boolean {
    nativeElems(this, other) { a, b ->
        val bLtA = Hacl_Bignum64_lt_mask(mode.numLongWordsInP.toUInt(), b, a) != 0UL
        return bLtA
    }
}

private fun Element.getCompat(other: ProductionGroupContext): LongArray {
    context.assertCompatible(other)
    return when (this) {
        is ProductionElementModP -> this.element
        is ProductionElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

class ProductionGroupContext(
    val pBytes: ByteArray,
    val qBytes: ByteArray,
    val gBytes: ByteArray,
    val p256minusQBytes: ByteArray,
    val rBytes: ByteArray,
    val name: String,
    val powRadixOption: PowRadixOption,
    val productionMode: ProductionMode,
    val numPBits: UInt,
    val numPBytes: UInt,
    val numPLWords: UInt
) : GroupContext {
    val p: HaclBignumP
    val q: HaclBignumQ
    val g: HaclBignumP
    val p256minusQ: HaclBignumQ
    val r: HaclBignumP
    val zeroModP: ProductionElementModP
    val oneModP: ProductionElementModP
    val twoModP: ProductionElementModP
    val gModP: ProductionElementModP
    val gInvModP by lazy { gPowP(qMinus1ModQ) }
    val gSquaredModP by lazy { G_MOD_P * G_MOD_P }
    val qModP: ProductionElementModP
    val zeroModQ: ProductionElementModQ
    val oneModQ: ProductionElementModQ
    val twoModQ: ProductionElementModQ
    val qMinus1ModQ: ProductionElementModQ
    val genericFieldP: CPointer<Hacl_Bignum_MontArithmetic_bn_mont_ctx_u64>
    val montCtxP: CPointer<Hacl_Bignum_MontArithmetic_bn_mont_ctx_u64>
    val montCtxQ: CPointer<Hacl_Bignum_MontArithmetic_bn_mont_ctx_u64>
    val dlogger: DLog

    init {
        p = pBytes.toHaclBignumP(mode = productionMode)
        q = qBytes.toHaclBignumQ()
        g = gBytes.toHaclBignumP(mode = productionMode)
        p256minusQ = p256minusQBytes.toHaclBignumQ()
        r = rBytes.toHaclBignumP(mode = productionMode)
        zeroModP = ProductionElementModP(0U.toHaclBignumP(productionMode), this)
        oneModP = ProductionElementModP(1U.toHaclBignumP(productionMode), this)
        twoModP = ProductionElementModP(2U.toHaclBignumP(productionMode), this)
        gModP = ProductionElementModP(g, this).acceleratePow() as ProductionElementModP
        qModP = ProductionElementModP(
            LongArray(numPLWords.toInt()) {
                // Copy from 256-bit to 4096-bit, avoid problems later on. Hopefully.
                    i -> if (i >= HaclBignumQ_LongWords) 0 else q[i]
            },
            this)
        zeroModQ = ProductionElementModQ(0U.toHaclBignumP(productionMode), this)
        oneModQ = ProductionElementModQ(1U.toHaclBignumQ(), this)
        twoModQ = ProductionElementModQ(2U.toHaclBignumQ(), this)
        qMinus1ModQ = (zeroModQ - oneModQ) as ProductionElementModQ

        // This context is something that normally needs to be freed, otherwise memory
        // leaks could occur, but we'll keep it live for the duration of the program
        // running, so we won't worry about it.
        montCtxP = p.useNative {
            Hacl_Bignum64_mont_ctx_init(numPLWords, it)
                ?: throw RuntimeException("failed to make montCtxP")
        }

        montCtxQ = q.useNative {
            Hacl_Bignum256_mont_ctx_init(it)
                ?: throw RuntimeException("failed to make montCtxQ")
        }

        genericFieldP = p.useNative {
            // Is the object we get back from this equivalent to montCtxP?
            // Maybe, but we're going to try to follow the API super closely,
            // which doesn't provide us any guarantees.

            Hacl_GenericField64_field_init(numPLWords, it)
                ?: throw RuntimeException("failed to make genericFieldP")
        }

        dlogger = DLog(gModP)
    }

    override val constants: ElectionConstants by lazy {
        ElectionConstants(name, pBytes, qBytes, rBytes, gBytes)
    }

    override fun isProductionStrength() = true

    override fun toString() : String = name

    override val ZERO_MOD_P
        get() = zeroModP

    override val ONE_MOD_P
        get() = oneModP

    override val TWO_MOD_P
        get() = twoModP

    override val G_MOD_P
        get() = gModP

    override val GINV_MOD_P
        get() = gInvModP

    override val G_SQUARED_MOD_P
        get() = gSquaredModP

    override val Q_MOD_P
        get() = qModP

    override val ZERO_MOD_Q
        get() = zeroModQ

    override val ONE_MOD_Q
        get() = oneModQ

    override val TWO_MOD_Q
        get() = twoModQ

    override val MAX_BYTES_P: Int
        get() = productionMode.numBytesInP

    override val MAX_BYTES_Q: Int
        get() = 32

    override val NUM_P_BITS: Int
        get() = productionMode.numBitsInP

    override fun isCompatible(ctx: GroupContext): Boolean =
        ctx.isProductionStrength() && productionMode == (ctx as ProductionGroupContext).productionMode

    override fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0)
            throw IllegalArgumentException("minimum $minimum may not be negative")
        else {
            val bytesToUse = if (b.size > numPBytes.toInt()) {
                // Vague assumption: input is big-endian, so we're getting the
                // lower bytes.
                b.copyOfRange(b.size - numPBytes.toInt(), b.size)
            } else b

            // we've got an optimized path, using our Montgomery context
            val bignumP = bytesToUse.toHaclBignumP(doubleMemory = true, mode = productionMode)
            val result = newZeroBignumP(productionMode)
            val minimumP = minimum.toUInt().toHaclBignumP(productionMode)

            nativeElems(bignumP, result, minimumP) { s, r, m ->
                Hacl_Bignum64_mod_precomp(montCtxP, s, r)

                // Hack to deal with the minimum part: if we're less than
                // the minimum, we'll just add the minimum back in. Since minimums
                // tend to be significantly smaller than the modulus, this won't
                // overflow. We can get away with this because the exact behavior
                // of the "safety" here is undefined, so long as the output is
                // within the expected bounds.

                if (minimum > 0 && Hacl_Bignum64_lt_mask(numPLWords, r, m) != 0UL) {
                    Hacl_Bignum64_add(numPLWords, r, m, r)
                }
            }
            return ProductionElementModP(result, this)
        }
    }

    override fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0)
            throw IllegalArgumentException("minimum $minimum may not be negative")
        else {
            // Performance note: this function runs as part of hashing, where we convert
            // the hash result back to an ElementModQ, which means that we really don't
            // want to call Hacl_Bignum256_mod, because it's relatively slow. We're instead
            // taking advantage of the fact that q is very close to 2^256, so if we're
            // in the region above or equal to q, we can wrap around with a simple addition.

            // But if we've got more than 256 bits, we need a fallback solution.

            if (b.size > MAX_BYTES_Q) {
                return hashElements(b.toBase64()).toElementModQ(this)
            }

            val minimum256 = minimum.toUInt().toHaclBignumQ()
            val nonZeroMinimum = minimum > 0

            val result = b.toHaclBignumQ()

            nativeElems(result, minimum256, q, p256minusQ) { r, m, qq, p256 ->
                if (Hacl_Bignum256_lt_mask(r, qq) == 0UL) {
                    // r >= q, so need to wrap around
                    // r = r + p256
                    //   = r + (2^256 - q)
                    //   = r - q
                    Hacl_Bignum256_add(r, p256, r)
                }

                // Same hack as above.

                if (nonZeroMinimum && Hacl_Bignum256_lt_mask(r, m) != 0UL) {
                    Hacl_Bignum256_add(r, m, r)
                }
            }

            return ProductionElementModQ(result, this)
        }
    }

    override fun binaryToElementModP(b: ByteArray): ElementModP? {
        try {
            val bignumP = ProductionElementModP(b.toHaclBignumP(mode = productionMode), this)
            if (!bignumP.inBounds()) return null
            return bignumP
        } catch (ex: IllegalArgumentException) {
            return null
        }
    }

    override fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        try {
            val bignum256 = ProductionElementModQ(b.toHaclBignumQ(), this)
            if (!bignum256.inBounds()) return null
            return bignum256
        } catch (ex: IllegalArgumentException) {
            return null
        }
    }

    override fun uIntToElementModQ(i: UInt) : ElementModQ = when (i) {
        0U -> ZERO_MOD_Q
        1U -> ONE_MOD_Q
        2U -> TWO_MOD_Q
        else -> ProductionElementModQ(i.toHaclBignumQ(), this)
    }

    override fun uIntToElementModP(i: UInt) : ElementModP = when (i) {
        0U -> ZERO_MOD_P
        1U -> ONE_MOD_P
        2U -> TWO_MOD_P
        else -> ProductionElementModP(i.toHaclBignumP(mode = productionMode), this)
    }

    override fun Iterable<ElementModQ>.addQ(): ElementModQ {
        val input = iterator().asSequence().toList()

        if (input.isEmpty()) {
            throw ArithmeticException("addQ not defined on empty lists")
        }

        if (input.count() == 1) {
            return input[0]
        }

        // There's an opportunity here to avoid creating intermediate ElementModQ instances
        // and mutate a running total instead. For now, we're just focused on correctness
        // and will circle back if/when this is performance relevant.

        return input.reduce { a, b -> a + b }
    }

    override fun Iterable<ElementModP>.multP(): ElementModP {
        val input = iterator().asSequence().toList()

        if (input.isEmpty()) {
            throw ArithmeticException("multP not defined on empty lists")
        }

        if (input.count() == 1) {
            return input[0]
        }

        // There's an opportunity here to avoid creating intermediate ElementModQ instances
        // and mutate a running total instead. For now, we're just focused on correctness
        // and will circle back if/when this is performance relevant.

        return input.reduce { a, b -> a * b }
    }

    override fun gPowP(e: ElementModQ) = gModP powP e

    override fun dLogG(p: ElementModP, maxResult: Int): Int? = dlogger.dLog(p, maxResult)
}

class ProductionElementModQ(val element: HaclBignumQ, val groupContext: ProductionGroupContext): ElementModQ,
    Element, Comparable<ElementModQ> {
    override val context: GroupContext
        get() = groupContext

    private fun HaclBignumQ.wrap(): ElementModQ = ProductionElementModQ(this, groupContext)

    override fun inBounds(): Boolean = element ltQ groupContext.q

    override fun isZero() = element.contentEquals(groupContext.ZERO_MOD_Q.element)

    override fun inBoundsNoZero(): Boolean = inBounds() && !isZero()

    override fun byteArray(): ByteArray {
        val results = ByteArray(32)
        results.useNative { r ->
            element.useNative { e ->
                Hacl_Bignum256_bn_to_bytes_be(e, r)
            }
        }
        return results
    }

    override operator fun compareTo(other: ElementModQ): Int {
        val otherElement = other.getCompat(groupContext)
        val thisLtOther = element ltQ otherElement

        return when {
            thisLtOther -> -1
            element.contentEquals(otherElement) -> 0
            else -> 1
        }
    }

    override operator fun plus(other: ElementModQ): ElementModQ {
        val result = newZeroBignumQ()

        nativeElems(result,
            element,
            other.getCompat(groupContext),
            groupContext.q,
            groupContext.p256minusQ) { r, a, b, q, p256 ->

            val carry = Hacl_Bignum256_add(a, b, r)
            val inBoundsQ = Hacl_Bignum256_lt_mask(r, q) != 0UL
            val zeroCarry = (carry == 0UL)

            when {
                inBoundsQ && zeroCarry -> { }
                !inBoundsQ && zeroCarry -> {
                    // result - Q; which we're guaranteed is in [0,Q) because there isn't
                    // much space between Q and 2^256; this wouldn't work for the general case
                    // of arbitrary primes Q, but works for ElectionGuard because 2Q > 2^256.
//                    Hacl_Bignum256_sub(r, q, r)

                    // Cleverly, by adding the difference between q and 2^256, we'll
                    // wrap around and end up where we should have been anyway.
                    Hacl_Bignum256_add(r, p256, r)
                }
                else -> {
                    // (2^256 - Q) + result; because we overflowed; again, this only works because
                    // 2Q > 2^256, so we're just adding in the bit that was lost when we wrapped
                    // around from the original add
                    Hacl_Bignum256_add(r, p256, r)
                }
            }
        }

        return result.wrap()
    }

    override operator fun minus(other: ElementModQ): ElementModQ =
        this + (-other)

    override operator fun times(other: ElementModQ): ElementModQ {
        val result = newZeroBignumQ()
        val scratch = LongArray(HaclBignumQ_LongWords * 2) // 512-bit intermediate value

        nativeElems(result, element, other.getCompat(groupContext), scratch) {
                r, a, b, s, ->
            Hacl_Bignum256_mul(a, b, s)
            Hacl_Bignum256_mod_precomp(groupContext.montCtxQ, s, r)
        }

        return result.wrap()
    }

//    override operator fun unaryMinus(): ElementModQ {
//        val inverse = groupContext.q - element
//         if element is zero, then we get an inverse that's out of bounds
//        return (if (inverse == groupContext.q) groupContext.zeroModQ else inverse.wrap())
//    }

    override operator fun unaryMinus(): ElementModQ {
        val result = newZeroBignumQ()

        if (this == groupContext.zeroModQ)
            return this

        nativeElems(result, element, groupContext.q) { r, e, q ->
            // We're guaranteed from our type system that e is in [0,Q), so we don't
            // have to worry about underflow, and we handle the degenerate zero case
            // above.
            Hacl_Bignum256_sub(q, e, r)
        }

        return result.wrap()
    }

    override fun multInv(): ElementModQ {
        val result = newZeroBignumQ()

        nativeElems(result, element) { r, e ->
            Hacl_Bignum256_mod_inv_prime_vartime_precomp(groupContext.montCtxQ, e, r)
        }

        return result.wrap()
    }

    override fun div(denominator: ElementModQ): ElementModQ = this * denominator.multInv()

    override infix fun powQ(e: ElementModQ): ElementModQ {
        val result = newZeroBignumQ()
        nativeElems(result, element, e.getCompat(groupContext)) { r, a, b ->
            // We're using the faster "variable time" modular exponentiation; timing attacks
            // are not considered a significant threat against ElectionGuard.
            Hacl_Bignum256_mod_exp_vartime_precomp(groupContext.montCtxQ, a, 256, b, r)
        }
        return result.wrap()
    }

    override fun equals(other: Any?) = when (other) {
        // We're converting from the internal representation to a byte array
        // for equality checking; possibly overkill, but if there are ever
        // multiple internal representations, this guarantees normalization.
        is ProductionElementModQ ->
            other.byteArray().contentEquals(this.byteArray()) &&
                    other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = base64()  // unpleasant, but available
}

open class ProductionElementModP(val element: HaclBignumP, val groupContext: ProductionGroupContext): ElementModP,
    Element, Comparable<ElementModP> {
    override val context: GroupContext
        get() = groupContext

    private fun HaclBignumP.wrap(): ElementModP = ProductionElementModP(this, groupContext)

    override fun inBounds(): Boolean = element.ltP(groupContext.p, groupContext.productionMode)

    override fun isZero() = element.contentEquals(groupContext.ZERO_MOD_P.element)

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override operator fun compareTo(other: ElementModP): Int {
        val otherElement = other.getCompat(groupContext)
        val thisLtOther = element.ltP(otherElement, groupContext.productionMode)

        return when {
            thisLtOther -> -1
            element.contentEquals(otherElement) -> 0
            else -> 1
        }
    }

    override fun byteArray(): ByteArray {
        val result = ByteArray(groupContext.numPBytes.toInt())
        result.useNative { r ->
            element.useNative { e ->
                Hacl_Bignum64_bn_to_bytes_be(groupContext.numPBytes, e, r)
            }
        }
        return result
    }

    override fun isValidResidue(): Boolean {
        val result = newZeroBignumP(groupContext.productionMode)
        nativeElems(result, element, groupContext.q) { r, a, q ->
            Hacl_Bignum64_mod_exp_vartime_precomp(groupContext.montCtxP, a, 256, q, r)
        }
        val residue = result.wrap() == groupContext.ONE_MOD_P
        return inBounds() && residue
    }

    override infix fun powP(e: ElementModQ): ElementModP {
        val result = newZeroBignumP(groupContext.productionMode)
        nativeElems(result, element, e.getCompat(groupContext)) { r, a, b ->
            // We're using the faster "variable time" modular exponentiation; timing attacks
            // are not considered a significant threat against ElectionGuard.
            Hacl_Bignum64_mod_exp_vartime_precomp(groupContext.montCtxP, a, 256, b, r)
        }
        return result.wrap()
    }

    override operator fun times(other: ElementModP): ElementModP {
        val result = newZeroBignumP(groupContext.productionMode)
        val scratch = LongArray(groupContext.numPLWords.toInt() * 2)
        nativeElems(result, element, other.getCompat(groupContext), scratch) { r, a, b, s ->
            Hacl_Bignum64_mul(groupContext.numPLWords, a, b, s)
            Hacl_Bignum64_mod_precomp(groupContext.montCtxP, s, r)
        }

        return result.wrap()
    }

    override fun multInv(): ElementModP {
        // Performance note: the code below is really, really slow. Like, it's 1/17
        // the speed of the equivalent code in java.math.BigInteger. The solution
        // is that we basically never call it. Instead, throughout the code for
        // things like Chaum-Pedersen proofs, we instead raise things to powers that
        // are equivalent to taking the inverse.

//        val result = newZeroBignum4096()
//
//        nativeElems(result, element) { r, e ->
//            Hacl_Bignum4096_mod_inv_prime_vartime_precomp(groupContext.montCtxP, e, r)
//        }
//
//        return result.wrap()

        // Alternative design: taking advantage of the smaller size of the subgroup
        // reachable from the generator.

        return this powP groupContext.qMinus1ModQ
    }

    override infix operator fun div(denominator: ElementModP) = this * denominator.multInv()

    override fun equals(other: Any?) = when (other) {
        // We're converting from the internal representation to a byte array
        // for equality checking; possibly overkill, but if there are ever
        // multiple internal representations, this guarantees normalization.
        is ProductionElementModP ->
            other.byteArray().contentEquals(this.byteArray()) &&
                    other.groupContext.isCompatible(this.groupContext)
        else -> false
    }

    override fun hashCode() = element.hashCode()

    override fun toString() = base64()  // unpleasant, but available

    override fun acceleratePow() : ElementModP =
        AcceleratedElementModP(this)

    override fun toMontgomeryElementModP(): MontgomeryElementModP {
        val result = newZeroBignumP(groupContext.productionMode)
        nativeElems(result, element) { r, e ->
            Hacl_GenericField64_to_field(groupContext.genericFieldP, e, r)
        }

        return ProductionMontgomeryElementModP(result, groupContext)
    }
}

class AcceleratedElementModP(p: ProductionElementModP) : ProductionElementModP(p.element, p.groupContext) {
    // Laziness to delay computation of the table until its first use; saves space
    // for PowModOptions that are never used. Also, the context isn't fully initialized
    // when constructing the GroupContext, and this avoids using it until it's ready.

    val powRadix by lazy { PowRadix(p, p.groupContext.powRadixOption) }

    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
}

private fun MontgomeryElementModP.getCompat(other: ProductionGroupContext): HaclBignumP {
    context.assertCompatible(other)
    return (this as ProductionMontgomeryElementModP).element
}

data class ProductionMontgomeryElementModP(
    val element: HaclBignumP,
    val groupContext: ProductionGroupContext
): MontgomeryElementModP {
    override fun times(other: MontgomeryElementModP): MontgomeryElementModP {
        val result = newZeroBignumP(groupContext.productionMode)

        nativeElems(result, element, other.getCompat(groupContext)) { r, a, b ->
            // Contrast this with what happens in ProductionElementModP.times(), where
            // there's a modulo operation. That's not necessary when we're operating
            // in Montgomery form, resulting (hopefully) in a significant speedup.
            Hacl_GenericField64_mul(groupContext.genericFieldP, a, b, r)
        }

        return ProductionMontgomeryElementModP(result, groupContext)
    }

    override fun toElementModP(): ElementModP {
        val result = newZeroBignumP(groupContext.productionMode)

        nativeElems(result, element) { r, a ->
            Hacl_GenericField64_from_field(groupContext.genericFieldP, a, r)
        }

        return ProductionElementModP(result, groupContext)
    }

    override val context: GroupContext
        get() = groupContext
}