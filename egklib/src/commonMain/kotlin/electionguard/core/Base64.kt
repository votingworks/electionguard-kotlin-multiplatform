package electionguard.core

import kotlin.math.min
import io.github.oshai.kotlinlogging.KotlinLogging

// Engineering note for this and Base16: there's also base16 and base64 support
// in ktor-utils, but those don't reliably reject bogus input, instead just giving
// you some random value. This code seems to be pretty solid. If we were going to
// do anything particularly different, it would be some sort of multiplatform thing
// that uses built-in libraries on Java as well as whatever base64 support is baked
// into modern JavaScript environments, saving this code for the native-only case
// where we don't otherwise have a ready-made solution.

// The code below is borrowed from the link below and modified a bit.
// That code, in turn, is a Kotlin port of some code from within Java,
// which carries the copyright notice reproduced below.
// https://gist.githubusercontent.com/MarkusKramer/4db02c9983c76efc6aa56cf0bdc75a5b/raw/aa49830aa7db2717926421bdde18d6d153f69c53/Base64.kt

/* Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions. */

/**
 * This implements static methods for working with base64-encoded strings. The implementation of
 * this class supports the "basic" Base64 encoding scheme as specified in
 * [RFC 4648](http://www.ietf.org/rfc/rfc4648.txt) and
 * [RFC 2045](http://www.ietf.org/rfc/rfc2045.txt).
 */
object Base64 {
    private val logger = KotlinLogging.logger("Base64")

    /** Convert a ByteArray to a base64 string. */
    fun ByteArray.toBase64() = encoder.encode(this).decodeToString()

    /** Convert a ByteArray to a base64Url string. */
    fun ByteArray.toBase64Url() = encoder.encode(this, true).decodeToString()

    /** Convert a String to a base64 ByteArray. Throws an `IllegalArgumentException` if it's invalid. */
    fun String.fromBase64Safe(): ByteArray = decoder.decode(this.encodeToByteArray())

    /** Convert a String to a base64Url ByteArray. Throws an `IllegalArgumentException` if it's invalid. */
    fun String.fromBase64UrlSafe(): ByteArray = decoder.decode(this.encodeToByteArray(), true)

    /** Convert a String to a base64 ByteArray. Returns null if the input is not a valid base64 string. */
    fun String.fromBase64(): ByteArray? =
        try {
            if (this == "") null else this.fromBase64Safe()
        } catch (ex: IllegalArgumentException) {
            logger.warn { "fromBase64 '$this' not a valid base64 string" }
            null
        }

    /** Convert a String to a base64Url ByteArray. Returns null if the input is not a valid base64Url string. */
    fun String.fromBase64Url(): ByteArray? =
        try {
            if (this == "") null else this.fromBase64UrlSafe()
        } catch (ex: IllegalArgumentException) {
            logger.warn { "fromBase64Url '$this' not a valid base64 string" }
            null
        }


    private val encoder = Encoder(null, -1, true)
    private val decoder = Decoder()

    private class Encoder(
        private val newline: ByteArray?,
        private val linemax: Int,
        private val doPadding: Boolean
    ) {
        private fun outLength(srclen: Int): Int {
            var len =
                if (doPadding) {
                    4 * ((srclen + 2) / 3)
                } else {
                    val n = srclen % 3
                    4 * (srclen / 3) + if (n == 0) 0 else n + 1
                }
            if (linemax > 0) {
                // line separators
                len += (len - 1) / linemax * newline!!.size
            }
            return len
        }

        /**
         * Encodes all bytes from the specified byte array into a newly-allocated byte array using
         * the [Base64] encoding scheme. The returned byte array is of the length of the resulting bytes.
         *
         * @param src the byte array to encode
         * @param useBase64URL use Base64URL else use Base64
         * @return A newly-allocated byte array containing the resulting encoded bytes.
         */
        fun encode(src: ByteArray, useBase64URL : Boolean = false): ByteArray {
            val len = outLength(src.size) // dst array size
            val dst = ByteArray(len)
            val ret = encode0(src, 0, src.size, dst, useBase64URL)
            return if (ret != dst.size) dst.copyOf(ret) else dst
        }

        private fun encodeBlock(src: ByteArray, sp: Int, sl: Int, dst: ByteArray, dp: Int, base64: CharArray) {
            var sp0 = sp
            var dp0 = dp
            while (sp0 < sl) {
                val bits: Int =
                    src[sp0++].toInt() and 0xff shl 16 or (src[sp0++].toInt() and 0xff shl 8) or
                        (src[sp0++].toInt() and 0xff)
                dst[dp0++] = base64[bits ushr 18 and 0x3f].code.toByte()
                dst[dp0++] = base64[bits ushr 12 and 0x3f].code.toByte()
                dst[dp0++] = base64[bits ushr 6 and 0x3f].code.toByte()
                dst[dp0++] = base64[bits and 0x3f].code.toByte()
            }
        }

        private fun encode0(src: ByteArray, off: Int, end: Int, dst: ByteArray, useBase64URL : Boolean): Int {
            val base64 = if (useBase64URL) toBase64URL else toBase64
            var sp = off
            var slen = (end - off) / 3 * 3
            val sl = off + slen
            if (linemax > 0 && slen > linemax / 4 * 3) slen = linemax / 4 * 3
            var dp = 0
            while (sp < sl) {
                val sl0: Int = min(sp + slen, sl)
                encodeBlock(src, sp, sl0, dst, dp, base64)
                val dlen = (sl0 - sp) / 3 * 4
                dp += dlen
                sp = sl0
                if (dlen == linemax && sp < end) {
                    for (b in newline!!) {
                        dst[dp++] = b
                    }
                }
            }
            if (sp < end) {
                // 1 or 2 leftover bytes
                val b0: Int = src[sp++].toInt() and 0xff
                dst[dp++] = base64[b0 shr 2].code.toByte()
                if (sp == end) {
                    dst[dp++] = base64[b0 shl 4 and 0x3f].code.toByte()
                    if (doPadding) {
                        dst[dp++] = '='.code.toByte()
                        dst[dp++] = '='.code.toByte()
                    }
                } else {
                    val b1: Int = src[sp].toInt() and 0xff
                    dst[dp++] = base64[b0 shl 4 and 0x3f or (b1 shr 4)].code.toByte()
                    dst[dp++] = base64[b1 shl 2 and 0x3f].code.toByte()
                    if (doPadding) {
                        dst[dp++] = '='.code.toByte()
                    }
                }
            }
            return dp
        }

        companion object {
            /**
             * This array is a lookup table that translates 6-bit positive integer index values into
             * their "Base64 Alphabet" equivalents as specified in "Table 1: The Base64 Alphabet" of
             * RFC 2045 (and RFC 4648).
             */
            val toBase64 =
                charArrayOf(
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
                )

            /**
             * It's the lookup table for "URL and Filename safe Base64" as specified in Table 2 of
             * the RFC 4648, with the '+' and '/' changed to '-' and '_'. This table is used when
             * BASE64_URL is specified.
             */
            internal val toBase64URL =
                charArrayOf(
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
                )
        }
    }

    private class Decoder {
        companion object {
            /**
             * Lookup table for decoding unicode characters drawn from the "Base64 Alphabet" (as
             * specified in Table 1 of RFC 2045) into their 6-bit positive integer equivalents.
             * Characters that are not in the Base64 alphabet but fall within the bounds of the
             * array are encoded to -1.
             */
            internal val fromBase64 = IntArray(256)

            /**
             * Lookup table for decoding "URL and Filename safe Base64 Alphabet" as specified in
             * Table2 of the RFC 4648.
             */
            private val fromBase64URL = IntArray(256)

            init {
                fromBase64.fill(-1)
                for (i in Encoder.toBase64.indices) fromBase64[Encoder.toBase64[i].code] = i
                fromBase64['='.code] = -2
            }

            init {
                fromBase64URL.fill(-1)
                for (i in Encoder.toBase64URL.indices) fromBase64URL[Encoder.toBase64URL[i].code] = i
                fromBase64URL['='.code] = -2
            }
        }

        /**
         * Decodes all bytes from the input byte array using the [Base64] encoding scheme, writing
         * the results into a newly-allocated output byte array. The returned byte array is of the
         * length of the resulting bytes.
         *
         * @param src the byte array to decode
         * @param useBase64URL use Base64URL else use Base64
         * @return A newly-allocated byte array containing the decoded bytes.
         *
         * @throws IllegalArgumentException if `src` is not in valid Base64 scheme
         */
        fun decode(src: ByteArray, useBase64URL : Boolean = false): ByteArray {
            var dst = ByteArray(outLength(src, 0, src.size))
            val ret = decode0(src, 0, src.size, dst, useBase64URL)
            if (ret != dst.size) {
                dst = dst.copyOf(ret)
            }
            return dst
        }

        private fun outLength(src: ByteArray, spx: Int, sl: Int): Int {
            var paddings = 0
            val len = sl - spx
            if (len == 0) return 0
            if (len < 2) {
                throw IllegalArgumentException(
                    "Input byte[] should at least have 2 bytes for base64 bytes"
                )
            }
            if (src[sl - 1].toInt().toChar() == '=') {
                paddings++
                if (src[sl - 2].toInt().toChar() == '=') paddings++
            }
            if (paddings == 0 && len and 0x3 != 0) paddings = 4 - (len and 0x3)
            return 3 * ((len + 3) / 4) - paddings
        }

        private fun decode0(src: ByteArray, spx: Int, sl: Int, dst: ByteArray, useBase64URL : Boolean): Int {
            var sp = spx
            val base64 = if (useBase64URL) fromBase64URL else fromBase64
            var dp = 0
            var bits = 0
            var shiftto = 18 // pos of first byte of 4-byte atom
            while (sp < sl) {
                if (shiftto == 18 && sp + 4 < sl) {
                    // fast path
                    val sl0 = sp + (sl - sp and 3.inv())
                    while (sp < sl0) {
                        val b1 = base64[src[sp++].toInt() and 0xff]
                        val b2 = base64[src[sp++].toInt() and 0xff]
                        val b3 = base64[src[sp++].toInt() and 0xff]
                        val b4 = base64[src[sp++].toInt() and 0xff]
                        if (b1 or b2 or b3 or b4 < 0) {
                            // non base64 byte
                            sp -= 4
                            break
                        }
                        val bits0 = b1 shl 18 or (b2 shl 12) or (b3 shl 6) or b4
                        dst[dp++] = (bits0 shr 16).toByte()
                        dst[dp++] = (bits0 shr 8).toByte()
                        dst[dp++] = bits0.toByte()
                    }
                    if (sp >= sl) break
                }
                var b: Int = src[sp++].toInt() and 0xff
                if (base64[b].also { b = it } < 0) {
                    if (b == -2) {
                        // padding byte '='
                        // =     shiftto==18 unnecessary padding
                        // x=    shiftto==12 a dangling single x
                        // x     to be handled together with non-padding case
                        // xx=   shiftto==6&&sp==sl missing last =
                        // xx=y  shiftto==6 last is not =
                        require(
                            !(shiftto == 6 && (sp == sl || src[sp++].toInt().toChar() != '=') ||
                                shiftto == 18)
                        ) { "Input byte array has wrong 4-byte ending unit" }
                        break
                    }
                    throw IllegalArgumentException(
                        "Illegal base64 character " + src[sp - 1].toInt().toString(16)
                    )
                }
                bits = bits or (b shl shiftto)
                shiftto -= 6
                if (shiftto < 0) {
                    dst[dp++] = (bits shr 16).toByte()
                    dst[dp++] = (bits shr 8).toByte()
                    dst[dp++] = bits.toByte()
                    shiftto = 18
                    bits = 0
                }
            }
            // reached end of byte array or hit padding '=' characters.
            when (shiftto) {
                6 -> {
                    dst[dp++] = (bits shr 16).toByte()
                }
                0 -> {
                    dst[dp++] = (bits shr 16).toByte()
                    dst[dp++] = (bits shr 8).toByte()
                }
                else ->
                    require(shiftto != 12) {
                        // dangling single "x", incorrectly encoded.
                        "Last unit does not have enough valid bits"
                    }
            }
            // anything left is invalid, if is not MIME.
            // if MIME, ignore all non-base64 character
            while (sp < sl) {
                throw IllegalArgumentException("Input byte array has incorrect ending byte at $sp")
            }
            return dp
        }
    }
}