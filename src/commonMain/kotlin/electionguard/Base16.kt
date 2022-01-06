package electionguard

object Base16 {
    private val hexChars =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    private val inverseChars = mapOf('0' to 0, '1' to 1, '2' to 2, '3' to 3, '4' to 4, '5' to 5,
        '6' to 6, '7' to 7, '8' to 8, '9' to 9, 'A' to 0xA, 'B' to 0xB, 'C' to 0xC, 'D' to 0xD,
        'E' to 0xE, 'F' to 0xF)

    /** Converts the byte-array to a hexadecimal string, using upper-case letters for A-F. */
    fun ByteArray.toHex(): String {
        // Performance note: since we're doing lookups in an array of characters, this
        // is going to run pretty quickly. This code is in the path for computing
        // cryptographic hashes, so performance matters here.

        if (size == 0) return "" // hopefully won't happen

        val result = CharArray(2 * this.size) {
            val offset: Int = it / 2
            val even: Boolean = (it and 1) == 0
            val nibble = if (even)
                (this[offset].toInt() and 0xf0) shr 4
            else
                this[offset].toInt() and 0xf
            hexChars[nibble]
        }
        return result.concatToString()
    }

    fun String.fromHex(): ByteArray? {
        // Performance note: we could probably speed this up by doing math on characters,
        // but we're only ever going to be using this code when reading from external
        // and possibly untrusted input. Correctness is important here, because this
        // is part of the attack surface.

        if (length == 0) return ByteArray(0) // hopefully won't happen

        // ensure we have an even number of characters
        val input = (if (this.length and 1 == 1) "0" else "") + this.uppercase()
        return ByteArray(input.length / 2) {
            // any invalid input characters will cause null to get returned
            val i0 = inverseChars[input[it * 2]] ?: return null
            val i1 = inverseChars[input[it * 2 + 1]] ?: return null
            ((i0 shl 4) or i1).toByte()
        }
    }
}