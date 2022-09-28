package electionguard.protoconvert

import electionguard.core.*
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals

import electionguard.core.HashedElGamalCiphertext
import electionguard.core.productionGroup

class ContestDataConvertTest {

    @Test
    fun convertContestData() {
        runTest {
            checkAll(
                validElementsModP(productionGroup()),
                byteArrays(21),
                uint256s(),
            ) { p, c1, u ->
                val context = productionGroup()
                val target = HashedElGamalCiphertext(p, c1, u, 21)
                assertEquals(target.numBytes, target.c1.size)

                val proto = target.publishHashedCiphertext()
                val roundtrip = context.importHashedCiphertext(proto)
                assertEquals(target, roundtrip)
            }
        }
    }
}