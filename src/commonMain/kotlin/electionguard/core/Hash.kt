package electionguard.core

/**
 * Any ElectionGuard type can implement this interface, and is then supported by [hashElements]. If
 * both [CryptoHashableString] and [CryptoHashableElement] are implemented by the same class, the
 * former will be used in preference to the latter.
 */
interface CryptoHashableString {
    /** Returns a string suitable for input to a hash function. */
    fun cryptoHashString(): String
}

/**
 * Any ElectionGuard type can implement this interface, and is then supported by [hashElements]. If
 * both [CryptoHashableString] and [CryptoHashableElement] are implemented by the same class, the
 * former will be used in preference to the latter.
 */
interface CryptoHashableElement {
    /** Returns an [Element], suitable for input to a hash function. */
    fun cryptoHashElement(): Element
}

/**
 * Given zero or more elements, calculate their cryptographic hash using SHA256. Specifically
 * handled types are [Element], [String], and [Iterable] containers of those types, as well as
 * anything implementing [CryptoHashableString] or [CryptoHashableElement]. Unsupported types yield
 * an [IllegalArgumentException].
 *
 * Of course, infinitely long iterables cannot be hashed, and will cause this function to run
 * forever or until it runs out of memory.
 *
 * @param elements Zero or more elements of any of the accepted types.
 * @return A cryptographic hash of these elements, converted to strings and suitably concatenated.
 */
fun GroupContext.hashElements(vararg elements: Any?): ElementModQ {
    val hashMe =
        elements.joinToString(prefix = "|", separator = "|", postfix = "|") {
            when (it) {
                null -> "null"
                is CryptoHashableString -> it.cryptoHashString()
                is CryptoHashableElement -> it.cryptoHashElement().cryptoHashString()
                is String -> it
                is Number, UInt, ULong, UShort, UByte -> it.toString()
                is Iterable<*> ->
                    // The simplest way to deal with lists and such are to crunch them
                    // recursively.

                    // We special-case the empty list, because it hashes to "null".
                    // Note that empty lists will hopefully never occur in practice,
                    // anywhere in ElectionGuard. Nulls should be uncommon, but might
                    // show up in the various data structures representing ballots,
                    // candidates, and so forth, which have optional fields.

                    if (it.none())
                        "null"
                    else
                        hashElements(*(it.toList().toTypedArray())).cryptoHashString()
                else -> throw IllegalArgumentException("unknown type in hashElements: ${it::class}")
            }
        }

    val digest = hashMe.encodeToByteArray().sha256()
    return safeBinaryToElementModQ(digest)
}

// TODO: do we need to be able to hash anything else that wouldn't already be covered
//   by CryptoHashable? The Python reference code, if it sees an unknown type, just
//   calls str() on it, getting whatever Python implementation-specific string representation
//   might be present. For contrast, we throw an exception here. This might at least
//   become a useful way to detect things that need to be CryptoHashable but aren't.
//   At the very least, any code that doesn't know what to do can simply implement
//   CryptoHashableString and return the value of a call to toString(). It's probably
//   a desirable engineering goal for all of this to be explicitly specified, rather
//   than quietly using toString(), yielding undefined or unexpected behavior.