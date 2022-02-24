package electionguard.protoconvert

import electionguard.core.*
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonConvertTest {

    @Test
    fun convertElementMod() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                elementsModQ(productionGroup()),
            ) { fakeElementModP, fakeElementModQ ->
                val context = productionGroup()

                val protoP = convertElementModP(fakeElementModP)
                val roundtripP = convertElementModP(protoP, context)
                assertEquals(roundtripP, fakeElementModP)

                val protoQ = convertElementModQ(fakeElementModQ)
                val roundtripQ = convertElementModQ(protoQ, context)
                assertEquals(roundtripQ, fakeElementModQ)

            }
        }
    }

    @Test
    fun convertCiphertext() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                validElementsModP(productionGroup()),
            ) { fakePad, fakeData ->
                val context = productionGroup()
                val ciphertext = ElGamalCiphertext(fakePad, fakeData)

                val proto = convertCiphertext(ciphertext)
                val roundtrip = convertCiphertext(proto, context)
                assertEquals(roundtrip, ciphertext)
            }
        }
    }

    @Test
    fun convertChaumPedersenProof() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                validElementsModP(productionGroup()),
                elementsModQ(productionGroup()),
                elementsModQ(productionGroup()),
            ) { a, b, c, r ->
                val context = productionGroup()
                val proof = GenericChaumPedersenProof(a, b, c, r)

                val proto = convertChaumPedersenProof(proof)
                val roundtrip = convertChaumPedersenProof(proto, context)
                assertEquals(roundtrip, proof)
            }
        }
    }

    @Test
    fun convertSchnorrProof() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                validElementsModP(productionGroup()),
                elementsModQ(productionGroup()),
                elementsModQ(productionGroup()),
            ) { p, commit, c, r ->
                val context = productionGroup()
                val publicKey = ElGamalPublicKey(p)
                val proof = SchnorrProof(publicKey, commit, c, r)
                val proto = convertSchnorrProof(proof)
                val roundtrip = convertSchnorrProof(proto, context)
                assertEquals(roundtrip, proof)
            }
        }
    }

    @Test
    fun convertElGamalPublicKey() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
            ) { p ->
                val context = productionGroup()
                val publicKey = ElGamalPublicKey(p)
                val proto = convertElGamalPublicKey(publicKey)
                val roundtrip = convertElGamalPublicKey(proto, context)
                assertEquals(roundtrip, publicKey)
            }
        }
    }
}