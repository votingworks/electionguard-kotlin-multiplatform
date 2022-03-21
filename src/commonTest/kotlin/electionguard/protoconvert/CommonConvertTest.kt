package electionguard.protoconvert

import electionguard.core.*
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonConvertTest {

    @Test
    fun convertElementMod() {
        runTest {
            checkAll(validElementsModP(productionGroup()), elementsModQ(productionGroup()),)
                { fakeElementModP, fakeElementModQ ->
                    val context = productionGroup()

                    val protoP = fakeElementModP.publishElementModP()
                    val roundtripP = context.importElementModP(protoP)
                    assertEquals(roundtripP, fakeElementModP)

                    val protoQ = fakeElementModQ.publishElementModQ()
                    val roundtripQ = context.importElementModQ(protoQ)
                    assertEquals(roundtripQ, fakeElementModQ)
                }
        }
    }

    @Test
    fun convertCiphertext() {
        runTest {
            checkAll(validElementsModP(productionGroup()), validElementsModP(productionGroup()),)
                { fakePad, fakeData ->
                    val context = productionGroup()
                    val ciphertext = ElGamalCiphertext(fakePad, fakeData)

                    val proto = ciphertext.publishCiphertext()
                    val roundtrip = context.importCiphertext(proto)
                    assertEquals(roundtrip, ciphertext)
                }
        }
    }

    @Test
    fun convertChaumPedersenProof() {
        runTest {
            checkAll(elementsModQ(productionGroup()), elementsModQ(productionGroup()),) { c, r ->
                val context = productionGroup()
                val proof = GenericChaumPedersenProof(c, r)

                val proto = proof.publishChaumPedersenProof()
                val roundtrip = context.importChaumPedersenProof(proto)
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
                val proto = proof.publishSchnorrProof()
                val roundtrip = context.importSchnorrProof(proto)
                assertEquals(roundtrip, proof)
            }
        }
    }

    @Test
    fun convertElGamalPublicKey() {
        runTest {
            checkAll(validElementsModP(productionGroup()),) { p ->
                val context = productionGroup()
                val publicKey = ElGamalPublicKey(p)
                val proto = publicKey.publishElGamalPublicKey()
                val roundtrip = context.importElGamalPublicKey(proto)
                assertEquals(roundtrip, publicKey)
            }
        }
    }
}