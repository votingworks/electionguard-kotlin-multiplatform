package electionguard.core

import electionguard.ballot.parameterBaseHash
import electionguard.core.Base16.fromHex
import io.ktor.utils.io.core.*
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class TestPrimes {

    @Test
    fun testPrimes4096() {
        Primes4096.pStr.forEach {
            assertTrue( !it.isWhitespace())
        }
        Primes4096.qStr.forEach {
            assertTrue( !it.isWhitespace())
        }
        Primes4096.rStr.forEach {
            assertTrue( !it.isWhitespace())
        }
        Primes4096.gStr.forEach {
            assertTrue( !it.isWhitespace())
        }

        checkPrime(Primes4096.qStr, 32)
        checkPrime(Primes4096.pStr, 512)
        checkPrime(Primes4096.rStr, 481)
        checkPrime(Primes4096.gStr, 512)
    }

    @Test
    fun testPrimes3072() {
        Primes3072.pStr.forEach {
            assertTrue( !it.isWhitespace())
        }
        Primes3072.qStr.forEach {
            assertTrue( !it.isWhitespace())
        }
        Primes3072.rStr.forEach {
            assertTrue( !it.isWhitespace())
        }
        Primes3072.gStr.forEach {
            assertTrue( !it.isWhitespace())
        }

        checkPrime(Primes3072.qStr, 32)
        checkPrime(Primes3072.pStr, 384)
        checkPrime(Primes3072.rStr, 353)
        checkPrime(Primes3072.gStr, 384)
    }

    fun checkPrime(primeString : String, expectSize : Int) {
        val fromHex = primeString.fromHex()
        assertNotNull(fromHex)
        assertEquals(expectSize, fromHex.size)

        val fromBI = BigInteger(primeString, 16).toByteArray()
        val fromBInormal = fromBI.normalize(expectSize)
        assertTrue(fromHex.contentEquals(fromBInormal))
    }

    // we have confirmation from Michael that this is the correct value of Hp
    @Test
    fun checkHp() {
        val version = "v2.0".toByteArray()
        val HV = ByteArray(32) { if (it < version.size) version[it] else 0 }

        val largePrime = Primes4096.pStr.fromHex()!!
        val smallPrime = Primes4096.qStr.fromHex()!!
        val generator = Primes4096.gStr.fromHex()!!

        val parameterBaseHash = hashFunction(
            HV,
            0x00.toByte(),
            largePrime,
            smallPrime,
            generator,
        )

        assertEquals("AB91D83C3DC3FEB76E57C2783CFE2CA85ADB4BC01FC5123EEAE3124CC3FB6CDE", parameterBaseHash.toHex())
    }

    @Test
    fun checkHp2() {
        val version = "2.0.0".toByteArray()
        val HV = ByteArray(32) { if (it < version.size) version[it] else 0 }

        val largePrime = Primes4096.pStr.fromHex()!!
        val smallPrime = Primes4096.qStr.fromHex()!!
        val generator = Primes4096.gStr.fromHex()!!

        val parameterBaseHash = hashFunction(
            HV,
            0x00.toByte(),
            largePrime,
            smallPrime,
            generator,
        )

        assertEquals("BAD5EEBFE2C98C9031BA8C36E7E4FB76DAC20665FD3621DF33F3F666BEC9AC0D", parameterBaseHash.toHex())
    }

    @Test
    fun checkHp3() {
        val version = "v2.0.0".toByteArray()
        val HV = ByteArray(32) { if (it < version.size) version[it] else 0 }

        val largePrime = Primes4096.pStr.fromHex()!!
        val smallPrime = Primes4096.qStr.fromHex()!!
        val generator = Primes4096.gStr.fromHex()!!

        val parameterBaseHash = hashFunction(
            HV,
            0x00.toByte(),
            largePrime,
            smallPrime,
            generator,
        )

        assertEquals("2B3B025E50E09C119CBA7E9448ACD1CABC9447EF39BF06327D81C665CDD86296", parameterBaseHash.toHex())
    }

    @Test
    fun parameterBaseHashTest() {
        val parameterBaseHash = parameterBaseHash(productionGroup().constants)
        assertEquals("2B3B025E50E09C119CBA7E9448ACD1CABC9447EF39BF06327D81C665CDD86296", parameterBaseHash.toHex())
    }

}