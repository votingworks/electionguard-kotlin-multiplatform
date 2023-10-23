package electionguard.core

import electionguard.ballot.parameterBaseHash
import electionguard.ballot.protocolVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class HmacSha256Test {

    // is it true we can call update instead of concatenating ??
    @Test
    fun testUpdate() {
        val callUpdate = hashFunction(
            byteArrayOf(1, 2),
            Primes4096.largePrimeBytes,
            Primes4096.smallPrimeBytes,
        )
        val callConcat = hashFunctionConcat(
            byteArrayOf(1, 2),
            Primes4096.largePrimeBytes,
            Primes4096.smallPrimeBytes,
        )
        assertEquals(callUpdate, callConcat)
    }

    @Test
    fun parameterBaseHashTest() {
        val primes = productionGroup().constants

        // HP = H(HV ; 00, p, q, g)   spec 2.0 eq 4
        // The symbol HV denotes the version byte array that encodes the used version of this specification.
        // The array has length 32 and contains the UTF-8 encoding of the string "v2.0" followed by 00-
        // bytes, i.e. HV = 76322E30 ∥ b(0, 28).
        val version = protocolVersion.encodeToByteArray()
        val HV = ByteArray(32) { if (it < version.size ) version[it] else 0 }

        val callConcat = hashFunctionConcat(
            HV,
            0.toByte(),
            primes.largePrime,
            primes.smallPrime,
            primes.generator,
        )
        assertEquals(1057, hashFunctionConcatSize(
            HV,
            0.toByte(),
            primes.largePrime,
            primes.smallPrime,
            primes.generator,
        ))

        val callUpdate = hashFunction(
            HV,
            0.toByte(),
            primes.largePrime,
            primes.smallPrime,
            primes.generator,
        )
        assertEquals(callUpdate, callConcat)

        val callParameter = parameterBaseHash(productionGroup().constants)
        assertEquals(callUpdate, callParameter)
    }

    @Test
    fun testIterator() {
        val primes = productionGroup().constants

        // HP = H(HV ; 00, p, q, g)  ; eq 4
        val version = protocolVersion.encodeToByteArray()
        val HV = ByteArray(32) { if (it < version.size ) version[it] else 0 }

        val callConcat = hashFunctionConcat(
            HV,
            0.toByte(),
            listOf(primes.largePrime, primes.smallPrime, primes.generator),
        )
        assertEquals(1057, hashFunctionConcatSize(
            HV,
            0.toByte(),
            listOf(primes.largePrime, primes.smallPrime, primes.generator),
        ))

        val callUpdate = hashFunction(
            HV,
            0.toByte(),
            listOf(primes.largePrime, primes.smallPrime, primes.generator),
        )
        assertEquals(callUpdate, callConcat)

        val callParameter = parameterBaseHash(productionGroup().constants)
        assertEquals(callUpdate, callParameter)
    }

    @Test
    fun domainSeperator() {
        val sep = "43".encodeToByteArray()
        println("sep 43 = ${sep.contentToString()}")

        val sep2 = "43".toLong(radix = 16)
        println("sep2 43 = ${sep2}")

        val sep3 = 0x43
        println("sep3 43 = ${sep3}")
    }
}


////////////////////////////////////
//// test concatenation vs update

fun hashFunctionConcat(key: ByteArray, vararg elements: Any): UInt256 {
    var result = ByteArray(0)
    elements.forEach { result += hashElementsToByteArray(it) }
    val hmac = HmacSha256(key)
    hmac.update(result)
    println("size = ${result.size}")
    return hmac.finish()
}

fun hashFunctionConcatSize(key: ByteArray, vararg elements: Any): Int {
    var result = ByteArray(0)
    elements.forEach {
        val eh = hashElementsToByteArray(it)
        println("  size = ${eh.size}")
        result += eh
    }
    return result.size
}

private fun hashElementsToByteArray(element : Any) : ByteArray {
    if (element is Iterable<*>) {
        var result = ByteArray(0)
        element.forEach { result += hashElementsToByteArray(it!!) }
        return result
    } else {
        val ba : ByteArray = when (element) {
            is Byte -> ByteArray(1) { element }
            is ByteArray -> element
            is UInt256 -> element.bytes
            is Element -> element.byteArray()
            is String -> element.encodeToByteArray()
            is Int -> intToByteArray(element)
            else -> throw IllegalArgumentException("unknown type in hashElements: ${element::class}")
        }
        return ba
    }
}