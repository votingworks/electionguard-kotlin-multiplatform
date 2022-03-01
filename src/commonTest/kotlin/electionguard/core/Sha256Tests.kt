package electionguard.core

import electionguard.core.Base16.fromHex
import electionguard.core.Base64.fromSafeBase64
import kotlin.experimental.xor
import kotlin.test.*

class Sha256Tests {
    val inputs: List<ByteArray> = arrayListOf(
        "rqoKvpTlYBYq6B8wLvWDhYHdyibiQ3isk4qjzxhwRJF5JJEagWOgcWCgMr2kqHBfc4lGDEH8hC30xVT2v3SVvRzjG6vWrvx3ugEyLHj6q8YApTtn/vr+vZMhaTB+UdFe2QE78uc5mxq1Ds30pdb5eU1rZWxMUOKmZjZf5WZLZHM=",
        "Y3hSYoc378mKVIu4Q63sS7Nb7gIKm8WwRixmv33Lqkt1RcFTx8lNAYObiPEP1a3lE8OHy/HZORLpbbsV3kx5F4vxMj7u9nMMSLZYQiIG8TMdUraPoYdsbG7p6cJltQXwz2sttOATZLwlVrj+g35eOHroUgF8fBwRUj85fGo66FE=",
        "bGswZ74G6s0Ax8kHficZrhwc6w+Lmb7OYm46Jb5EI+wNR66lThxNWr/mhxeeyuSKu1LGig2KDVHnOZFKZGU3Pb2c14r6FfwrE7mn7e/RKXSr6xa7IyzkgQ3qSTyCBsTvVK+Vbc52KpbRFV5IrpBi8Y+qgj3MyKiwzjFTnTklwHM=",
        "Mh7QPcqhNU9Uqr57rumnwtbC7oXu57qFYwFddCz1oqy69jYLdJ0qwhc5ohHZgknQi63yds7BxCbEFHATkox+DWkDs9s8nIUQFLFOuQi4FOPNmyP3PD67+3FD2XkY9ZFD0IFSVP6Ar8j9johsy7HGFu7rgfge40Jk3iqFou/0rrg=",
        "Iwh70Dg2hY3avsd2Jutj1exVNWHHKuGhBJKKWaaJ667TpvTdFXEVeU81PXyyABTkgpqTdJg7+F1EBQtUwE7PqX+uRookubD+3JUBCtGiKlISs+Ro3wREAJjTc7R82QfbktOT12G/1PEo+j8PpOZRroNG7tBGj6GqWTFwV2DNT7E=",
        "4B7h2lEUQPiCu9BAB6J0IoHhsGuml96CXffn7+sy4zklyRL9SfqAVV64YKBIWU/kgwQmHMgQs9zQVf3PB98W3/oXNTiiX1HN1MWkA40mYZPiO+Y/XQRd+n2Sligb8ztXmQ3I8EDy8qT6myNwH3nKgzDZPeB9GoDBJLAr5yIMcWI=",
        "sZS7Zr03mQQluqsDgA5V4d110Jz8bqyX8ci7o3/hZrWzPtN80kNEwwMsG75G+ZHci6VMQhroJRCXBV11BLB3NjqYurq+MlVCJ2H6C80oAHqABfHMySmqG9HF2cblEGGaGZPnjcPIWxfxVI25+Jsq4TspAZjnhHM8IkNaq8pFLeU=",
        "h7YcwdEMVwLPR/72NL5qTSnEKm3V2korqvBEuu0mMJ5+0gPOWdQGcliIiBAGyt9tGIreVF3iUv7apg8S81R/4JzqNfsESx0iyoGoDuL5aH5gKKNEmjE0wXlysKJYj3oROokPqE/CFoHbO7UZ1woNxo4o7Zx4BZHj75o3rwEUSzA=",
        "RNBvXlGgmZ/DdnQQj9L9kORexOUTYOsP4dHbksLh+kgQvebroRHiK1YtYJYiH/3ZHPs+TATDwePzNdeThEXzhadiWplOlWiXvSu4tWl4w1OzIrVrBRNxQPbQwv0HYxE5WMoXjojUGD2MHN/9rwhsPNvrC3MOEaAfwnFTr/D3ZpU=",
        "P4sfzDH7p2ttMIGdL/Xv0QZJ0NzIKo7XsYXMxq/r4uMlQAy+NELkkZpBzvw/V7BfdxmO7a2KRw4mZKPzlh1b/V3RFvWwf6uWJRr/ulDuvCEkOPB1ndB3nN/pScM2O9l00QMRL9v9eMz+IRkPPSgnb+H5rJ5ZjS9Fko5KoZX/Vsc=",
    ).map { it.fromSafeBase64() }

    val keys: List<ByteArray> = arrayListOf(
        "VTHopnAQhaTI104GE8K9Y8XNgKa7MfzW/n4VymG3nng=",
        "WCSbQ4G3lVL4G7cK6MMGJiAcnjYPdipoT7qgtu4QlMc=",
        "RgDCwCg4COf4HOeXoSeUNR+yU9kWR0iGZsw91v+nrM0=",
        "IDER77GqFtS5C3ac1ZXhHtXrZD3zby/GpfbSt7/c1Vg=",
        "ty0ElL/DiONPDeVp7dejz1sFgslvvz8zw4X0nnynEk8=",
        "47GiV0YtiAt35TmBi/Is6o3oJesvWenLeAFo3aa52wM=",
        "qoXhCBEFu41G5EPYO4aQfzpq0OSdrR2VttDhdvqcZQQ=",
        "+t8ZVOjkseaxpt8RIR1shQKe5UjNaw9lswI00IneOWw=",
        "aYAUQe3r4zRuUc53b47xOsEhwmQeVhiQHcKjV2f+Ks0=",
        "GvY+0UG+GIjmby3yFM8NMwEu1xNJOm+6Q9sIzRT/C9A=",
    ).map { it.fromSafeBase64() }

    val hashes: List<ByteArray> = arrayListOf(
        "T67GzUyPGucRhZPSUE1310Nd/Gpxe0cW/umLBYPlbaM=",
        "WuDmdZb540ouDLuIeJ1akvvnyQ1JX9AdNbn92oUCYTo=",
        "ItnqXqAWhg/PPVJko0krBq8nbNFQxq407KzQQFCFsik=",
        "YGxK7VhFZG+xUTlC+yK2lhF0PBi9T1IoBJPhQGoMhWU=",
        "kxag70/7u8btk577BtNF8XYkLulMOx4lqHEBEiRSNl8=",
        "VXnygjJ+OrNfs+GevQKzrOchgjYl2ctv2HQ74c1NytU=",
        "RaMJA/8ZX1v/L5YuR9c8mJhwelMQPBC+HW7TTr3wf6A=",
        "aE+nfwOU2bYB7F7t4ZCmBoyxZF7ijHXz4uO3ilaAfhk=",
        "mcUdI6EPiHPn6gv6d2dSNwDoX6CGfxHT6/B+mLiL+5M=",
        "FqOKtZb5oBDg0rKG/vR+cgw/sg5UbeiuXB5psZ6b+Hw=",
    ).map { it.fromSafeBase64() }

    val hmacs: List<ByteArray> = arrayListOf(
        "dm2zvlv1TW9f309Tgag4fWaAwWmDin6GXLheZGygogw=",
        "pRrrzzc1uDcclk8uQe7z2nBKx+9DEqCvDZLhXWaKwBw=",
        "cPG72k5HxwjcF6fnbm6fBsuBqVnz2tbUhaG0mYtcZ2I=",
        "cmyGxS+v0qiv63xiXqEM9LFhV3B0dsrasFIEPApB3Is=",
        "7czVvhmSW4aL5s637CKnvTbmG5Rrmf1p5UvjzwsMvuQ=",
        "+8NXbldiH32dhVM3TRipL5uQRnHEn33gLx2+LX/QkWw=",
        "W9cnLXMOGODK7ap+d5q/R6U6WMCzvId8UomMRsRZNBI=",
        "JvQ+JIzWpDFSZClEr+VvZpi2c/o3uPR82DuPrAM+afo=",
        "dh4R4/QYQMHRfdkoMSx53ShZ/ULdJ6/N6aRr0/apec0=",
        "COE+427z9bEIOcbMsl2sJFoCiG//XDpbjGZJ/GC8HGg=",
    ).map { it.fromSafeBase64() }

    @Test
    fun testSha256() {
        (0..9).forEach { i ->
            assertContentEquals(hashes[i], inputs[i].sha256())
            assertContentEquals(hashes[i], internalSha256(inputs[i]))
        }
    }

    @Test
    fun testHmacSha256() {
        (0..9).forEach { i ->
            assertContentEquals(hmacs[i], inputs[i].hmacSha256(keys[i]))
            assertContentEquals(hmacs[i], internalHmacSha256(keys[i], inputs[i]))
        }
    }

    @Test
    fun testHmacSha256Homebrew() {
        val key = ("0b".repeat(32)).fromHex() ?: fail("shouldn't be null")
        val data = "4869205468657265".fromHex() ?: fail("shouldn't be null")
        val realHmac = data.hmacSha256(key)

        val ipad = ByteArray(32) { 0x0b.toByte() xor 0x36 }
        val opad = ByteArray(32) { 0x0b.toByte() xor 0x5c }
        val homebrewHmac = internalByteConcat(opad, internalByteConcat(ipad, data).sha256()).sha256()
        assertContentEquals(realHmac, homebrewHmac)

//            assertContentEquals(hmac, internalHmacSha256(key, data))
    }
}