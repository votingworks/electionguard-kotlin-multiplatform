package electionguard.core

import electionguard.ballot.ElectionConstants
import electionguard.core.Base64.fromSafeBase64
import mu.KotlinLogging
import java.math.BigInteger

private val logger = KotlinLogging.logger("Group")

private val montgomeryI = BigInteger.ONE shl Primes4096.nbits
private val p = BigInteger(Primes4096.pStr, 16)
private val q = BigInteger(Primes4096.qStr, 16)
private val g = BigInteger(Primes4096.gStr, 16)
private val r = BigInteger(Primes4096.rStr, 16)

private val productionGroups4096 : Map<PowRadixOption, ProductionGroupContext> =
    PowRadixOption.values().associateWith {
        ProductionGroupContext(
            pBytes = p.toByteArray().normalize(512),
            qBytes = q.toByteArray().normalize(32),
            gBytes = g.toByteArray().normalize(512),
            rBytes = r.toByteArray().normalize(512),
            montIMinus1Bytes = (montgomeryI - BigInteger.ONE).toByteArray(),
            montIPrimeBytes = (montgomeryI.modPow(p - BigInteger.TWO, p)).toByteArray(),
            montPPrimeBytes = ((montgomeryI - p).modInverse(montgomeryI)).toByteArray(),
            name = "production group, ${it.description}, 4096 bits",
            powRadixOption = it,
            productionMode = ProductionMode.Mode4096,
            numPBits = intProduction4096PBits
        )
    }

/*
val montgomeryI = BigInteger.ONE shl numBits // 2^{4096} or 2^{3072}
val montgomeryIMinusOne = montgomeryI - BigInteger.ONE
val montgomeryIPrime = montgomeryI.modPow(p - BigInteger.TWO, p)
val montgomeryPPrime = (montgomeryI - p).modInverse(montgomeryI)
 */


private val productionGroups4096old : Map<PowRadixOption, ProductionGroupContext> =
    PowRadixOption.values().associateWith {
        ProductionGroupContext(
            pBytes = b64Production4096P.fromSafeBase64(),
            qBytes = b64Production4096Q.fromSafeBase64(),
            gBytes = b64Production4096G.fromSafeBase64(),
            rBytes = b64Production4096R.fromSafeBase64(),
            montIMinus1Bytes = b64Production4096MontgomeryIMinus1.fromSafeBase64(),
            montIPrimeBytes = b64Production4096MontgomeryIPrime.fromSafeBase64(),
            montPPrimeBytes = b64Production4096MontgomeryPPrime.fromSafeBase64(),
            name = "production group, ${it.description}, 4096 bits",
            powRadixOption = it,
            productionMode = ProductionMode.Mode4096,
            numPBits = intProduction4096PBits
        )
    }

private val productionGroups3072 =
    PowRadixOption.values().associateWith {
        ProductionGroupContext(
            pBytes = b64Production3072P.fromSafeBase64(),
            qBytes = b64Production3072Q.fromSafeBase64(),
            gBytes = b64Production3072G.fromSafeBase64(),
            rBytes = b64Production3072R.fromSafeBase64(),
            montIMinus1Bytes = b64Production3072MontgomeryIMinus1.fromSafeBase64(),
            montIPrimeBytes = b64Production3072MontgomeryIPrime.fromSafeBase64(),
            montPPrimeBytes = b64Production3072MontgomeryPPrime.fromSafeBase64(),
            name = "production group, ${it.description}, 3072 bits",
            powRadixOption = it,
            productionMode = ProductionMode.Mode3072,
            numPBits = intProduction3072PBits
        )
    }

actual fun productionGroup(acceleration: PowRadixOption, mode: ProductionMode) : GroupContext =
    when(mode) {
        ProductionMode.Mode4096 -> productionGroups4096[acceleration] ?: throw Error("can't happen")
        ProductionMode.Mode3072 -> productionGroups3072[acceleration] ?: throw Error("can't happen")
    }


/** Convert an array of bytes, in big-endian format, to a BigInteger */
internal fun UInt.toBigInteger() = BigInteger.valueOf(this.toLong())
internal fun ByteArray.toBigInteger() = BigInteger(1, this)

class ProductionGroupContext(
    pBytes: ByteArray,
    qBytes: ByteArray,
    gBytes: ByteArray,
    rBytes: ByteArray,
    montIMinus1Bytes: ByteArray,
    montIPrimeBytes: ByteArray,
    montPPrimeBytes: ByteArray,
    val name: String,
    val powRadixOption: PowRadixOption,
    val productionMode: ProductionMode,
    val numPBits: Int
) : GroupContext {
    val p: BigInteger
    val q: BigInteger
    val g: BigInteger
    val r: BigInteger
    val zeroModP: ProductionElementModP
    val oneModP: ProductionElementModP
    val twoModP: ProductionElementModP
    val gModP: ProductionElementModP
    val gInvModP by lazy { gPowP(qMinus1Q) }
    val gSquaredModP: ProductionElementModP
    val qModP: ProductionElementModP
    val zeroModQ: ProductionElementModQ
    val oneModQ: ProductionElementModQ
    val twoModQ: ProductionElementModQ
    val dlogger: DLog
    val qMinus1Q: ProductionElementModQ
    val montgomeryIMinusOne: BigInteger
    val montgomeryIPrime: BigInteger
    val montgomeryPPrime: BigInteger

    init {
        p = pBytes.toBigInteger()
        q = qBytes.toBigInteger()
        g = gBytes.toBigInteger()
        r = rBytes.toBigInteger()
        zeroModP = ProductionElementModP(0U.toBigInteger(), this)
        oneModP = ProductionElementModP(1U.toBigInteger(), this)
        twoModP = ProductionElementModP(2U.toBigInteger(), this)
        gModP = ProductionElementModP(g, this).acceleratePow() as ProductionElementModP
        gSquaredModP = ProductionElementModP((g * g).mod(p), this)
        qModP = ProductionElementModP(q, this)
        zeroModQ = ProductionElementModQ(0U.toBigInteger(), this)
        oneModQ = ProductionElementModQ(1U.toBigInteger(), this)
        twoModQ = ProductionElementModQ(2U.toBigInteger(), this)
        dlogger = DLog(gModP)
        qMinus1Q = (zeroModQ - oneModQ) as ProductionElementModQ
        montgomeryIMinusOne = montIMinus1Bytes.toBigInteger()
        montgomeryIPrime = montIPrimeBytes.toBigInteger()
        montgomeryPPrime = montPPrimeBytes.toBigInteger()
    }

    override fun isProductionStrength() = true

    override val constants: ElectionConstants by lazy {
        ElectionConstants(name, pBytes, qBytes, rBytes, gBytes)
    }

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
        get() = 512

    override val MAX_BYTES_Q: Int
        get() = 32

    override val NUM_P_BITS: Int
        get() = numPBits

    override fun isCompatible(ctx: GroupContext): Boolean =
        ctx.isProductionStrength() && productionMode == (ctx as ProductionGroupContext).productionMode

    override fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger().mod(p)

        val mv = minimum.toBigInteger()
        val tmp2 = if (tmp < mv) tmp + mv else tmp

        return ProductionElementModP(tmp2, this)
    }

    override fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigInteger().mod(q)

        val mv = minimum.toBigInteger()
        val tmp2 = if (tmp < mv) tmp + mv else tmp

        return ProductionElementModQ(tmp2, this)
    }

    override fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = b.toBigInteger()
        return if (tmp >= p || tmp < BigInteger.ZERO) null else ProductionElementModP(tmp, this)
    }

    override fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = b.toBigInteger()
        return if (tmp >= q || tmp < BigInteger.ZERO) null else ProductionElementModQ(tmp, this)
    }

    override fun uIntToElementModQ(i: UInt) : ElementModQ = when (i) {
        0U -> ZERO_MOD_Q
        1U -> ONE_MOD_Q
        2U -> TWO_MOD_Q
        else -> ProductionElementModQ(i.toBigInteger(), this)
    }

    override fun uIntToElementModP(i: UInt) : ElementModP = when (i) {
        0U -> ZERO_MOD_P
        1U -> ONE_MOD_P
        2U -> TWO_MOD_P
        else -> ProductionElementModP(i.toBigInteger(), this)
    }

    override fun Iterable<ElementModQ>.addQ(): ElementModQ {
        val input = iterator().asSequence().toList()

        if (input.isEmpty()) {
            throw ArithmeticException("addQ not defined on empty lists")
        }

        if (input.count() == 1) {
            return input[0]
        }

        val result = input.map {
            it.getCompat(this@ProductionGroupContext)
        }.reduce { a, b ->
            (a + b).mod(this@ProductionGroupContext.q)
        }

        return ProductionElementModQ(result, this@ProductionGroupContext)
    }

    override fun Iterable<ElementModP>.multP(): ElementModP {
        val input = iterator().asSequence().toList()

        if (input.isEmpty()) {
            throw ArithmeticException("multP not defined on empty lists")
        }

        if (input.count() == 1) {
            return input[0]
        }

        val result = input.map {
            it.getCompat(this@ProductionGroupContext)
        }.reduce { a, b ->
            (a * b).mod(this@ProductionGroupContext.p)
        }

        return ProductionElementModP(result, this@ProductionGroupContext)
    }

    override fun gPowP(e: ElementModQ) = gModP powP e

    override fun dLogG(p: ElementModP, maxResult: Int): Int? = dlogger.dLog(p, maxResult)
}

private fun Element.getCompat(other: ProductionGroupContext): BigInteger {
    context.assertCompatible(other)
    return when (this) {
        is ProductionElementModP -> this.element
        is ProductionElementModQ -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

class ProductionElementModQ(internal val element: BigInteger, val groupContext: ProductionGroupContext): ElementModQ,
    Element, Comparable<ElementModQ> {

    override fun byteArray(): ByteArray = element.toByteArray().normalize(32)

    private fun BigInteger.modWrap(): ElementModQ = this.mod(groupContext.q).wrap()
    private fun BigInteger.wrap(): ElementModQ = ProductionElementModQ(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun isZero() = element == BigInteger.ZERO

    override fun inBounds() = element >= BigInteger.ZERO && element < groupContext.q

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override operator fun compareTo(other: ElementModQ): Int = element.compareTo(other.getCompat(groupContext))

    override operator fun plus(other: ElementModQ) =
        (this.element + other.getCompat(groupContext)).modWrap()

    override operator fun minus(other: ElementModQ) =
        this + (-other)

    override operator fun times(other: ElementModQ) =
        (this.element * other.getCompat(groupContext)).modWrap()

    override fun multInv(): ElementModQ = element.modInverse(groupContext.q).wrap()

    override operator fun unaryMinus(): ElementModQ =
        if (this == groupContext.zeroModQ)
            this
        else
            (groupContext.q - element).wrap()

    override infix operator fun div(denominator: ElementModQ): ElementModQ =
        this * denominator.multInv()

    override infix fun powQ(e: ElementModQ): ElementModQ =
        this.element.modPow(e.getCompat(groupContext), groupContext.q).wrap()

    override fun equals(other: Any?) = when (other) {
        is ElementModQ -> byteArray().contentEquals(other.byteArray())
        else -> false
    }

    override fun hashCode() = byteArray().contentHashCode()

    override fun toString() = base16()
}

open class ProductionElementModP(internal val element: BigInteger, val groupContext: ProductionGroupContext): ElementModP,
    Element, Comparable<ElementModP> {

    override fun byteArray(): ByteArray = element.toByteArray().normalize(512)

    private fun BigInteger.modWrap(): ElementModP = this.mod(groupContext.p).wrap()
    private fun BigInteger.wrap(): ElementModP = ProductionElementModP(this, groupContext)

    override val context: GroupContext
        get() = groupContext

    override fun isZero() = element == BigInteger.ZERO

    override fun inBounds() = element >= BigInteger.ZERO && element < groupContext.p

    override fun inBoundsNoZero() = inBounds() && !isZero()

    override operator fun compareTo(other: ElementModP): Int = element.compareTo(other.getCompat(groupContext))

    override fun isValidResidue(): Boolean {
        val residue = this.element.modPow(groupContext.q, groupContext.p) == groupContext.ONE_MOD_P.element
        return inBounds() && residue
    }

    override infix fun powP(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.p).wrap()

    override operator fun times(other: ElementModP) =
        (this.element * other.getCompat(groupContext)).modWrap()

    override fun multInv()
            = element.modInverse(groupContext.p).wrap()
//            = this powP groupContext.qMinus1Q

    // Performance note: multInv() can be expressed with the modInverse() method or we can do
    // this exponentiation thing with Q - 1, which works for the subgroup. On the JVM, we get
    // basically the same performance either way.

    override infix operator fun div(denominator: ElementModP) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.p)).modWrap()

    override fun acceleratePow() : ElementModP =
        AcceleratedElementModP(this)

    override fun toMontgomeryElementModP(): MontgomeryElementModP =
        ProductionMontgomeryElementModP(
            element.shiftLeft(groupContext.productionMode.numBitsInP).mod(groupContext.p),
            groupContext)

    override fun equals(other: Any?) = when (other) {
        is ElementModP -> byteArray().contentEquals(other.byteArray())
        else -> false
    }

    override fun hashCode() = byteArray().contentHashCode()

    override fun toString() = base16()
}

class AcceleratedElementModP(p: ProductionElementModP) : ProductionElementModP(p.element, p.groupContext) {
    // Laziness to delay computation of the table until its first use; saves space
    // for PowModOptions that are never used.

    val powRadix by lazy { PowRadix(p, p.groupContext.powRadixOption) }

    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
}

internal data class ProductionMontgomeryElementModP(val element: BigInteger, val groupContext: ProductionGroupContext): MontgomeryElementModP {
    internal fun MontgomeryElementModP.getCompat(other: GroupContext): BigInteger {
        context.assertCompatible(other)
        if (this is ProductionMontgomeryElementModP) {
            return this.element
        } else {
            throw NotImplementedError("unexpected MontgomeryElementModP type")
        }
    }

    internal fun BigInteger.modI(): BigInteger = this and groupContext.montgomeryIMinusOne

    internal fun BigInteger.divI(): BigInteger = this shr groupContext.productionMode.numBitsInP

    override fun times(other: MontgomeryElementModP): MontgomeryElementModP {
        val w: BigInteger = this.element * other.getCompat(this.context)

        // w = aI * bI = (ab)(I^2)

        // Z = ((((W mod I)⋅p^' )  mod I)⋅p+W)/I
        val z: BigInteger =
            (((w.modI() * groupContext.montgomeryPPrime).modI() * groupContext.p) + w).divI()

        return ProductionMontgomeryElementModP(
            if (z >= groupContext.p) z - groupContext.p else z,
            groupContext)
    }

    override fun toElementModP(): ElementModP =
        ProductionElementModP((element * groupContext.montgomeryIPrime).mod(groupContext.p), groupContext)

    override val context: GroupContext
        get() = groupContext

}
