package electionguard.core

/**
 * Generates a sequence of random elements in [0,Q), seeded from an initial element in [0,Q). If you
 * start with the same seed, you'll get exactly the same sequence. Optional string or [Element]
 * "headers" can be included alongside the seed both at construction time and when asking for the
 * next nonce. This is useful when specifying what a nonce is being used for, to avoid various kinds
 * of subtle cryptographic attacks.
 *
 * You can treat this as an array and index into it with integers, or you can get it as an infinite
 * (lazy) sequence using [Nonces.asSequence].
 *
 * Probably the easiest way to use this class is with its destructuring operators, so if you need to
 * expand one seed into three nonces, you might write:
 * ```kotlin
 * val (a, b, c) = Nonces(seed, "some-specific-purpose")
 * ```
 *
 * @param headers must be types that are accepted by hashFunction()
 */
class Nonces(seed: ElementModQ, vararg headers: Any) {
    val internalGroup = compatibleContextOrFail(seed)
    val internalSeed =
        if (headers.isNotEmpty()) hashFunction(seed.byteArray(), *headers).bytes else seed.byteArray()

    override fun equals(other: Any?) =
        when (other) {
            is Nonces -> internalSeed == other.internalSeed // TODO "dangerous array condition"
            else -> false
        }

    override fun hashCode() = internalSeed.hashCode()

    override fun toString() = "Nonces($internalSeed)"
}

/** Get the requested nonce from the sequence. */
operator fun Nonces.get(index: Int): ElementModQ = getWithHeaders(index)

/**
 * Get the requested nonce from the sequence, hashing the requested headers in with the result.
 * Headers can be included to optionally help specify what a nonce is being used for.
 */
fun Nonces.getWithHeaders(index: Int, vararg headers: String) =
    hashFunction(internalSeed, index, *headers).toElementModQ(internalGroup)

/**
 * Get an infinite (lazy) sequences of nonces. Equivalent to indexing with [Nonces.get] starting at
 * 0.
 */
fun Nonces.asSequence(): Sequence<ElementModQ> = generateSequence(0) { it + 1 }.map { this[it] }

/** Gets a list of the desired number (`n`) of nonces. */
fun Nonces.take(n: Int): List<ElementModQ> = asSequence().take(n).toList()

/** If you want a two-tuple (Kotlin's [Pair]) of nonces, this is helpful. */
fun Nonces.asPair() = Pair(this[0], this[1])

/** If you want a three-tuple (Kotlin's [Triple]) of nonces, this is helpful. */
fun Nonces.asTriple() = Triple(this[0], this[1], this[2])

// Destructuring operators, likely how nonces will be most commonly used

operator fun Nonces.component1() = this[0]

operator fun Nonces.component2() = this[1]

operator fun Nonces.component3() = this[2]

operator fun Nonces.component4() = this[3]

operator fun Nonces.component5() = this[4]

operator fun Nonces.component6() = this[5]

operator fun Nonces.component7() = this[6]

operator fun Nonces.component8() = this[7]

operator fun Nonces.component9() = this[8]
