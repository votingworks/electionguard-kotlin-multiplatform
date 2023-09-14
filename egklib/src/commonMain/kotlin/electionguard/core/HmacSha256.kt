package electionguard.core

/**
 * The function HMAC( , ) shall be used to denote the HMAC-SHA-256 keyed Hash Message
 * Authentication Code (as defined in NIST PUB FIPS 198-1 (2) instantiated with SHA-256 (as
 * defined in NIST PUB FIPS 180-4 (3). HMAC takes as input a key k and a message m of
 * arbitrary length and returns a bit string HMAC(k, m) of length 256 bits.

 * (4) NIST (2008) The Keyed-Hash Message Authentication Code (HMAC). In: FIPS 198-1. https://csrc.nist.
 * gov/publications/detail/fips/198/1/final
 * (5) NIST (2015) Secure Hash Standard (SHS). In: FIPS 180-4. https://csrc.nist.gov/publications/detail/
 * fips/180/4/final
 *
 * spec 2.0.0, p.9.
 */
expect class HmacSha256(key : ByteArray) {
    fun update(ba : ByteArray)
    fun finish() : UInt256
}