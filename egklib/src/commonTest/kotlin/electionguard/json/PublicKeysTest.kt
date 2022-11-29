package electionguard.json

import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.core.schnorrProof
import electionguard.keyceremony.PublicKeys
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlin.test.*

class PublicKeysTest {
    @Test
    fun testRoundtrip() {
        runTest {
            val group = productionGroup()
            checkAll(
                iterations = 33,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 10),
                Arb.int(min = 1, max = 10),
                ) { id, xcoord, quota,  ->

                val proofs = mutableListOf<SchnorrProof>()
                repeat(quota) {
                    val kp = elGamalKeypairs(group).single()
                    val nonce = elementsModQ(group).single()
                    proofs.add(kp.schnorrProof(nonce))
                }
                val publicKey = PublicKeys(id, xcoord, proofs)
                assertEquals(publicKey, publicKey.publish().import(group).unwrap())
                assertEquals(publicKey, jsonRoundTrip(publicKey.publish()).import(group).unwrap())

            }
        }
    }
}