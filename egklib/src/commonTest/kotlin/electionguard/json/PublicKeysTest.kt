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
            val context = productionGroup()
            checkAll(
                iterations = 33,
                Arb.string(minSize = 3),
                Arb.int(min = 1, max = 10),
                Arb.int(min = 1, max = 10),
                ) { id, xcoord, quota,  ->

                val proofs = mutableListOf<SchnorrProof>()
                repeat(quota) {
                    val kp = elGamalKeypairs(context).single()
                    val nonce = elementsModQ(context).single()
                    proofs.add(kp.schnorrProof(nonce))
                }
                val publicKey = PublicKeys(id, xcoord, proofs)
                assertEquals(publicKey, context.importPublicKeys(publicKey.publish()).unwrap())
                assertEquals(publicKey, context.importPublicKeys(jsonRoundTrip(publicKey.publish())).unwrap())
            }
        }
    }
}