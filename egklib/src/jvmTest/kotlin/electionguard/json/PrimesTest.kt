package electionguard.json

import electionguard.core.*
import electionguard.core.Base64.fromBase64
import electionguard.core.Base64.toBase64
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PrimesTest {
    val group = productionGroup()

    @Test
    fun testDecodePBase64FromR() {
        val ps =
            "//////////////////////////////////////////+xchf30c95q8njs5gD8vavQPNDJnKYti2KDRdbi6r6K+e4diBt66yYVZVS+0r6GxDtLq41wTghRCdXOykRabglPpbKFiJK6MUay9oRMXw4frnqm8OxNmA7JW+g7HZX90tyzoexnWVIyvXfpr04MDJIZV+hhy8g46LaLZfFDz/Vxgf0yhH7W/uQYQ0w+I/lUaLuVp1t/B76FX0uI94UALOWF0YHdduJkOXJQ+cytHnNM8zMTmWTk1FMTBoeC9HWCV0lZpszNWSjN2qcf4peFI6CB022AVz+eqMMSApUFzUNLJVdUXmx4XudrjE822xgbLEHj3NdGy2zG19QtRhQZMGLTRYts7NlhT11mKGVGuJz7lVwtsaPlpg0ltTm0zCviJtEoCVUcxzcjqFyk9Eiik75jW9Rd/vPB1UmilwflTi5gmGv/URrHKPPXpIiuIxm08VCIYPtyZQhCQu7Fvrz2UnyNuArIM7ohrkFwSjVPQvS+WITYxlq9QMCAGDkmQg5GgxXM5uivrp9BSrFthzE6SB87y8M4tc3OVjXYiZYkERXRPtfLaS3UQBYktNWiQ3v6crZudS3E+BhYqLY/dDfL9YI//////////////////////////////////////////8="
        val ba = ps.fromBase64()
        assertNotNull(ba)
        println("nbytes = ${ba.size}")

        assertTrue(group.constants.largePrime.contentEquals(ba))
    }

    @Test
    fun testDecodeQBase64FromR() {
        val qs = "/////////////////////////////////////////0M="
        val ba = qs.fromBase64()
        assertNotNull(ba)
        println("nbytes = ${ba.size}")
        assertTrue(group.constants.smallPrime.contentEquals(ba))
    }

    @Test
    fun testDecodeRBase64FromR() {
        val r = Primes4096.residualBytes
        println("r in base64 = ${r.toBase64()}")
        val relem = ProductionElementModP(r.toBigInteger(), group as ProductionGroupContext)

        val rnn = Primes4096.residualNotNormalized
        println("rnn in base64 = ${rnn.toBase64()}")
        val rnnelem = ProductionElementModP(rnn.toBigInteger(), group as ProductionGroupContext)
        println("rnn == r as bigInt ${r.toBigInteger() == rnn.toBigInteger()}")
        println("rnn == r as ElementModP ${relem == rnnelem}")

        // r = (p − 1)/q
        val p = Primes4096.largePrimeBytes.toBigInteger()
        val q = Primes4096.smallPrimeBytes.toBigInteger()
        val cr = (p - BigInteger.ONE) / q
        println("computed r in base64 = ${cr.toByteArray().toBase64()}")
        val crelem = ProductionElementModP(cr, group as ProductionGroupContext)
        println("cr == r as bigInt ${r.toBigInteger() == cr}")
        println("cr == r as ElementModP ${relem == crelem}")

        val rs =
            "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8sXIX99HPeavJ47OYA/L2r0DzQyZymLYtig0XW4urhXro9CgWVBiAbGKw6jY1Wjpz4MdBmFv2oOMTAXm/LwtD4zrYYpI4YbjJ92jEFpUZYAutBgk/lksn4C2GgxIxqRYN5I9NpT2KteaeOGtpS+wa5yLUdXkknVQkdnxcM7kVHgfFwR0QasRG0zC0fbWdNS5HpTFX3gRGGQD2/jYNuJffUxbYfJSucdrQvoS2R8S8+BjCOi1Ou1PHAqXIBi0Z9em1AzqU9/9zL1QSlxKGnZe4yWxBKSGp2GeXcPSZoEHCl8/3nUyRSetsr2e56j3FY9ll86rRN3/yLenD5iBo3Q7WFRw3tPdGNMK9CdqRL9WZ9DM6jSzABWJ9yje61D5ko5YxGcC/40gQoh7nz8Qh1TOYy8epWzv1heWgS3kOL+H+m8Jk/agQn2RUoIL177LzfqI3qinfMg1uqGDEGpBUzNJIdsYlP2Z7+wE5tVMf8wGJlhIC/SsNVadScsf9czQ/eJm8oLNqTEcKZKAJJEyE53zrySQX1bsTvxgWfYAz62xN14ef1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg=="
        val nbits = 4096
        val nbytes = nbits/8
        val ba = rs.fromBase64()
        assertNotNull(ba)
        println("nbytes = ${ba.size}")
        val ba2 = ba.normalize(nbytes)
        val rsb = ba2.toBigInteger()
        println("rs == r as bigInt ${r.toBigInteger() == rsb}")
        val rsbelem = ProductionElementModP(rsb, group as ProductionGroupContext)
        println("rs == r as ElementModP ${relem == rsbelem}")
        assertEquals(relem, rsbelem)
    }

    @Test
    fun testDecodeGBase64FromR() {
        val gs =
            "NgNv7SFPO1DcVm06MS/kEx/uHCvObQLqObR3rAX3+IXzjP53p+Raz0ApEUxNepv+BYvy+ZXSR5092mGP/ZENPEI2qyz914OlAW90Zc9Zu/RdJKIvEw8tBP6TstWLucHR0n/JoX0q9Jp3nz/73KIpAMFCAu5smWFgNL41y83T57t5lq3+U0tjzKQeIf9dx3jrsbhsU7++mZh9euoHViN/tAkiE5+Qpi8qqNmtNN/3meM8hXpkaNABrPO2gduH3EJCdV4qxaUCfbgZhPAzxNF4Nx8nPbtPzqHmKMI+UnWbx3ZXKANc6ia0TEmmVmaImCCkXDPdN+pKHQDLYjBc1UG+HoqSaFoHASsaIKdGw1kaLbOBUADSqsz+Q9xJ6CjB7XOHRmr9jkvxk1WTsqRC7sJxxQrTn3M3l6HqEYAqJVeRZTRmKmt+mp5EmiTIz/gJ55pNgG62gRGTMObFeYXjmyALSJNjn9/epJ92rRrNmX66E2V1QeeexXQ35QTtqd0BEGFRbGQ/sw1tWK/M0otz/top7BKwGl64Y5mlk6nV9FDeOcuSlixexpJTSNtU0Sj9mcFLRX+IPsIBEqdaagWB09gKO07wnshvlVL/2hZT8TOqJTSYOm8xsO5Gl5Naax6i91uF5+uhUbpIYJTWhyKwVGM/7FHKPymzHnfjF7F4trnYrg8="
        val ba = gs.fromBase64()
        assertNotNull(ba)
        println("nbytes = ${ba.size}")
        assertTrue(group.constants.generator.contentEquals(ba))
    }

    @Test
    fun showConstantsInBase64() {
        println("p = ${group.constants.largePrime.toBase64()}")
        println("q = ${group.constants.smallPrime.toBase64()}")
        println("r = ${group.constants.cofactor.toBase64()}")
        println("g = ${group.constants.generator.toBase64()}")
    }
}