package electionguard

import electionguard.Base64.decodeFromBase64
import electionguard.Base64.encodeToBase64

// 4096-bit P and 256-bit Q primes, plus generator G and cofactor R
internal val b64ProductionP =
    "//////////////////////////////////////////+TxGfjfbDHpNG+P4EBUstWoc7MOvZcwBkMA980cJr/vY5LWfoDqfDu0GScy2IQV9EQVq6RMhNaCOQ7RnPXS6/qWN64eMyG1zPb5784FUs2z4qW0VZ4maquDAnUyLa3uG/Soeod5i/4ZD7HwnGCeXciXmrC8L1hx0aWFUKjzjvqXbVP5w5j5tCfj8KGWOgFZ6R8/eYO50Hl2Fp71GkxztgiA2VZSWS4OYlvyqvMybMZWcCD8irT7lkcMvqyx0SPKgV9sttJ7lLgGCdB5Thl8ATMjnBLfFxAvzBMTYxPE+32BHxVUwLSI42M4R3yQk8bZsLF0jjQdE22ea8okEhwMfnArqHEu2/pVU7lKP3xsF5bJWIjsvCSFfNxn5x8zGnd8XLQ1iNCF/zAA38YuT71OJEwt6Zh5cJuVCFAaLvK/qMqZ4GL0wda0fXH6cw9Fzf7KBcbr4TbtmEreIHBpI5DnNA6kr9SIlorOOZULp9yK84Vo4G1dT6oQnYzgcyug1ErMFEbMuXo2ANiFJrQMKq6XzpXmLsiqn7BttDxeQP04i2EBzSqhZc/eak/+4KnXEfAPUPS+coC0DGZus7d1FM6UlZq//////////////////////////////////////////8="
internal val b64ProductionQ = "/////////////////////////////////////////0M="
internal val b64ProductionP256MinusQ = "////////////////////////////////////////AA8=" // 2^256 - Q
internal val b64ProductionG =
    "HUHknEd+Feru8MXkrAjUpGwmjNNCT8AdE3ab20NnMhhYe8hsTBRI0AagNpnzq65f6xnilvXRQ8xeSj/IkIjJ9FI9Fm7jrp1fsDwL3Xet1cAX9sVeLsksIm/vXGwd8ufDbZDn6q3gmCQdNAmYO8zStTeek5H7xi+fjZOdEgixYDZ8E0JkEiGJWV7IXIzb5fnTB/RpEsBJMvjBaBWna0aCvWvcDtUrANjTD1nHMdWn/66BZdU8+WZJqsK3Q9pW8U8Z2sxSNvKbGrn5vvxpaXKT1d6ti1v13purbeZ8RXGeVjRKPL3zYJgksbV4406utt0xkKs1cdbWccUSKCwdp702tCUdJYT63qgLnhQUIwdN2bX7g6y96tTIelj/9Rf5d6gwgDcKOwz5ihvCl4xHqsKWEf1sQOL5h1w11QRDqao/SWEdzToNb/PLP6zzFHG9thhguSxZTU5GVpuzn+6t/x/WTINqbW24XGunJBdmt6tWv3OWM7BUFH9xcJIUEulI2eR0AtFbscJXMYYSwSHDa4DrhDPAjn0LcUnjqwqHNaku3Oj/lD4ootzqz8xp7DGJCcsEe+HFhYhEta1E8i7rKJ5MxVT3peLz3qAmh3/5KFGBYHHOAo64aNllzLLSKVqMVb0cBws5sJrgazfSk0O52Jl9wkTEaLmAlwcxc27gGLutuYc="
internal val b64ProductionR =
    "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCbivamTAztzy1VnanZfwlcMHbGhgN2GRSNLIbDFxAq+iFIAx8ERArA/wyaQXqJISUS52B7JQHapNOKLBQQxINhSeK9uMgmDmJ8RkaWPv/p4W5JXUi9IVxtjsnRZnZXoqHIUG8hE/+tGaayvHxFdgRWcZGDMJ+HS8ms5XD/2od6orI6LW8pHBVUyi6xLxLNAJuLhzSmStUeuJO9iRdQuFFiJB2QjwyXCYeXWOfoIz6rO/LWq1OvoyqhU61mguWgZIiXyb4YoNUL7OAww0MjNq2RY+M/jn2vSY8UuyhSr/qBSEHrGN1fDolRbVV3dihcFgcdIRGU7hw/NGQgNquIbj7CiILOQAPeozW02TW65LWCNbn7K6txPI9wWhx95CIgIJ1rvKzEZzGGAVZScuSmPjjiSZdUrkk6wajoNGnu81yifCcbx5Lu4hFW5he5IuqPcTwizygtxdY4W7EoaOt4Enj6CrKolY/Mtf/i5cNh/BdEIBIrAWPKSkYwjIxGyR6nRXwTan2f1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg=="

// 16-bit everything, suitable for accelerated testing
internal val b64TestP = "/vM="
internal val b64TestQ = "f3k="
internal val b64Test256MinusQ = "////////////////////////////////////////gIc=" // 2^256 - Q
internal val b64TestG = "Aw=="
internal val b64TestR = "Ag=="

// useful for checking the decoders from base64
internal val intTestP = 65267
internal val intTestQ = 32633
internal val intTestG = 3
internal val intTestR = 2

/**
 * Computes the sum of the given elements, mod q; this can be faster than using the addition
 * operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
fun addQ(vararg elements: ElementModQ) = elements.asIterable().addQ()

/**
 * Computes the product of the given elements, mod p; this can be faster than using the
 * multiplication operation for large numbers of inputs by potentially reusing scratch-space memory.
 */
fun multP(vararg elements: ElementModP) = elements.asIterable().multP()

/**
 * Converts a base-64 string to an [ElementModP]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModP(s: String): ElementModP? =
    binaryToElementModP(s.decodeFromBase64())

/**
 * Converts a base-64 string to an [ElementModP]. Guarantees the result is in [0, P), by computing
 * the result mod P.
 */
fun GroupContext.safeBase64ToElementModP(s: String): ElementModP =
    safeBinaryToElementModP(s.decodeFromBase64())

/**
 * Converts a base-64 string to an [ElementModQ]. Guarantees the result is in [0, Q), by computing
 * the result mod Q.
 */
fun GroupContext.safeBase64ToElementModQ(s: String): ElementModQ =
    safeBinaryToElementModQ(s.decodeFromBase64())

/**
 * Converts a base-64 string to an [ElementModQ]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModQ(s: String): ElementModQ? =
    binaryToElementModQ(s.decodeFromBase64())

/** Converts from any [Element] to a base64 string representation. */
fun Element.base64(): String = byteArray().encodeToBase64()

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModQ(ctx: GroupContext) =
    if (this < 0)
        throw NoSuchElementException("no negative numbers allowed")
    else
        this.toUInt().toElementModQ(ctx)

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModP(ctx: GroupContext) =
    if (this < 0)
        throw NoSuchElementException("no negative numbers allowed")
    else
        this.toUInt().toElementModP(ctx)

interface Element {
    /**
     * Every Element knows the [GroupContext] that was used to create it. This simplifies code that
     * computes with elements, allowing arithmetic expressions to be written in many cases without
     * needing to pass in the context.
     */
    val context: GroupContext
        get

    /**
     * Normal computations should ensure that every [Element] is in the modular bounds defined by
     * the group, but deserialization of hostile inputs or buggy code might not preserve this
     * property, so it's valuable to have a way to check. This method allows anything in [0, N)
     * where N is the group modulus.
     */
    fun inBounds(): Boolean

    /**
     * Normal computations should ensure that every [Element] is in the modular bounds defined by
     * the group, but deserialization of hostile inputs or buggy code might not preserve this
     * property, so it's valuable to have a way to check. This method allows anything in [1, N)
     * where N is the group modulus.
     */
    fun inBoundsNoZero(): Boolean

    /** Converts from any [Element] to a compact [ByteArray] representation. */
    fun byteArray(): ByteArray
}