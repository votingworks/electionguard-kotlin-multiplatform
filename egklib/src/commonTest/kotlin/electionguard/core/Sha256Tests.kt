package electionguard.core

import electionguard.core.Base16.fromSafeHex
import electionguard.core.Base64.fromSafeBase64
import kotlin.test.*

class Sha256Tests {
    val inputs: List<ByteArray> =
        arrayListOf(
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

    val keys: List<UInt256> =
        arrayListOf(
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
        ).map { UInt256(it.fromSafeBase64()) }

    val hashes: List<UInt256> =
        arrayListOf(
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
        ).map { UInt256(it.fromSafeBase64()) }

    val hmacs: List<UInt256> =
        arrayListOf(
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
        ).map { UInt256(it.fromSafeBase64()) }

    @Test
    fun testSha256() {
        (0..9)
            .forEach { i ->
                assertEquals(hashes[i], inputs[i].sha256())
                assertEquals(hashes[i], internalSha256(inputs[i]))
            }
    }

    @Test
    fun testHmacSha256() {
        (0..9)
            .forEach { i ->
                assertEquals(hmacs[i], inputs[i].hmacSha256(keys[i]))
                assertEquals(hmacs[i], internalHmacSha256(keys[i], inputs[i]))
            }
    }

    @Test
    fun testHmacSha256same() {
        (0..9)
            .forEach { i ->
                val h1 = inputs[i].hmacSha256(keys[i])
                val hmac = HmacSha256(keys[i].bytes)
                hmac.update(inputs[i])
                val h2 = hmac.finish()
                assertEquals(h1, h2)
            }
    }

    // RFC 4231 test vectors
    // https://datatracker.ietf.org/doc/html/rfc4231#section-4.1

    @Test
    fun testHmacSha256Rfc4231No1() {
        // Key =          0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b
        //                  0b0b0b0b                          (20 bytes)
        //   Data =         4869205468657265                  ("Hi There")
        //
        //   HMAC-SHA-256 = b0344c61d8db38535ca8afceaf0bf12b
        //                  881dc200c9833da726e9376c2e32cff7

        val key = "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b".fromSafeHex()
        val data = "4869205468657265".fromSafeHex()
        val hmac =
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7".fromSafeHex()
                .toUInt256()

        assertEquals(hmac, data.hmacSha256(key))
        assertEquals(hmac, internalHmacSha256(key, data))
    }

    @Test
    fun testHmacSha256Rfc4231No2() {
        //   Test with a key shorter than the length of the HMAC output.
        //
        //   Key =          4a656665                          ("Jefe")
        //   Data =         7768617420646f2079612077616e7420  ("what do ya want ")
        //                  666f72206e6f7468696e673f          ("for nothing?")
        //
        //   HMAC-SHA-256 = 5bdcc146bf60754e6a042426089575c7
        //                  5a003f089d2739839dec58b964ec3843
        //
        val key = "4a656665".fromSafeHex()
        val data = "7768617420646f2079612077616e7420666f72206e6f7468696e673f".fromSafeHex()
        val hmac =
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843".fromSafeHex()
                .toUInt256()

        assertEquals(hmac, data.hmacSha256(key))
        assertEquals(hmac, internalHmacSha256(key, data))
    }

    @Test
    fun testHmacSha256Rfc4231No3() {
        //   Test with a combined length of key and data that is larger than 64
        //   bytes (= block-size of SHA-224 and SHA-256).
        //
        //   Key            aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaa                          (20 bytes)
        //   Data =         dddddddddddddddddddddddddddddddd
        //                  dddddddddddddddddddddddddddddddd
        //                  dddddddddddddddddddddddddddddddd
        //                  dddd                              (50 bytes)
        //
        //   HMAC-SHA-256 = 773ea91e36800e46854db8ebd09181a7
        //                  2959098b3ef8c122d9635514ced565fe
        //
        val key = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".fromSafeHex()
        val data =
            "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
                .fromSafeHex()
        val hmac =
            "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe".fromSafeHex()
                .toUInt256()

        assertEquals(hmac, data.hmacSha256(key))
        assertEquals(hmac, internalHmacSha256(key, data))
    }

    @Test
    fun testHmacSha256Rfc4231No4() {
        //   Test with a combined length of key and data that is larger than 64
        //   bytes (= block-size of SHA-224 and SHA-256).
        //
        //   Key =          0102030405060708090a0b0c0d0e0f10
        //                  111213141516171819                (25 bytes)
        //   Data =         cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd
        //                  cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd
        //                  cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd
        //                  cdcd                              (50 bytes)
        //
        //   HMAC-SHA-256 = 82558a389a443c0ea4cc819899f2083a
        //                  85f0faa3e578f8077a2e3ff46729665b
        //
        val key = "0102030405060708090a0b0c0d0e0f10111213141516171819".fromSafeHex()
        val data =
            "cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd"
                .fromSafeHex()
        val hmac =
            "82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b".fromSafeHex()
                .toUInt256()

        assertEquals(hmac, data.hmacSha256(key))
        assertEquals(hmac, internalHmacSha256(key, data))
    }

    // we're skipping test 5, doesn't seem very interesting

    @Test
    fun testHmacSha256Rfc4231No6() {
        //   Test with a key larger than 128 bytes (= block-size of SHA-384 and
        //   SHA-512).
        //
        //   Key =          aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaa                            (131 bytes)
        //   Data =         54657374205573696e67204c61726765  ("Test Using Large")
        //                  72205468616e20426c6f636b2d53697a  ("r Than Block-Siz")
        //                  65204b6579202d2048617368204b6579  ("e Key - Hash Key")
        //                  204669727374                      (" First")
        //
        //   HMAC-SHA-256 = 60e431591ee0b67f0d8a26aacbf5b77f
        //                  8e0bc6213728c5140546040f0ee37f54
        //
        val key =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                .fromSafeHex()
        val data =
            "54657374205573696e67204c6172676572205468616e20426c6f636b2d53697a65204b6579202d2048617368204b6579204669727374"
                .fromSafeHex()
        val hmac =
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54".fromSafeHex()
                .toUInt256()

        assertEquals(hmac, data.hmacSha256(key))
        assertEquals(hmac, internalHmacSha256(key, data))
    }

    @Test
    fun testHmacSha256Rfc4231No7() {
        //   Test with a key and data that is larger than 128 bytes (= block-size
        //   of SHA-384 and SHA-512).
        //
        //   Key =          aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //                  aaaaaa                            (131 bytes)
        //   Data =         54686973206973206120746573742075  ("This is a test u")
        //                  73696e672061206c6172676572207468  ("sing a larger th")
        //                  616e20626c6f636b2d73697a65206b65  ("an block-size ke")
        //                  7920616e642061206c61726765722074  ("y and a larger t")
        //                  68616e20626c6f636b2d73697a652064  ("han block-size d")
        //                  6174612e20546865206b6579206e6565  ("ata. The key nee")
        //                  647320746f2062652068617368656420  ("ds to be hashed ")
        //                  6265666f7265206265696e6720757365  ("before being use")
        //                  642062792074686520484d414320616c  ("d by the HMAC al")
        //                  676f726974686d2e                  ("gorithm.")
        //
        //   HMAC-SHA-256 = 9b09ffa71b942fcb27635fbcd5b0e944
        //                  bfdc63644f0713938a7f51535c3a35e2
        val key =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                .fromSafeHex()
        val data =
            "5468697320697320612074657374207573696e672061206c6172676572207468616e20626c6f636b2d73697a65206b657920616e642061206c6172676572207468616e20626c6f636b2d73697a6520646174612e20546865206b6579206e6565647320746f20626520686173686564206265666f7265206265696e6720757365642062792074686520484d414320616c676f726974686d2e"
                .fromSafeHex()
        val hmac =
            "9b09ffa71b942fcb27635fbcd5b0e944bfdc63644f0713938a7f51535c3a35e2".fromSafeHex()
                .toUInt256()

        assertEquals(hmac, data.hmacSha256(key))
        assertEquals(hmac, internalHmacSha256(key, data))
    }
}