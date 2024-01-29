package electionguard.exp

import org.junit.jupiter.api.Test
import kotlin.test.*

class VACUtilTest {

    @Test
    fun testNList() {
        val k = 5
        val cv = 26
        val nlist = cv.toNList()
        assertEquals(cv, toIntN(nlist))
    }

    @Test
    fun testBList() {
        val k = 5
        val cv = 26
        val blist = cv.toVector(k)
        assertEquals(cv, blist.toCV())
    }

    @Test
    fun testNlistConvertBinary() {
        val k = 5
        val cv = 26
        val nlist = cv.toNList()
        val blist = binary(k, nlist)
        assertEquals(nlist, nonzero(blist))
    }

    @Test
    fun testBlistConvertNonbinary() {
        val k = 5
        val cv = 26
        val bvec = cv.toVector(k)
        val nlist = cv.toNList()
        assertTrue(bvec.contentEquals(binary(k, nlist)))
    }

    @Test
    fun testBlistToVector() {
        val k = 5
        val cv = 26
        val bvec = cv.toVector(k)
        val blist = bvec.toBlist()
        assertTrue(bvec.contentEquals(blist.toVector()))
        assertEquals(cv, bvec.toCV())
        assertEquals(cv, blist.toCV())
    }

    @Test
    fun testBitVectorisSumOf() {
        val k = 5
        val v26 = BitVector(k, 26)

        assertFalse(v26.isSumOf(BitVector(k, 26), BitVector(k, 26)))

        val (a, b) = v26.divide()
        println("26 = ${a.cv} + ${b.cv}")
        assertTrue(v26.isSumOf(a, b))
    }

    @Test
    fun testBitVectorisOneBitDifferent() {
        val k = 5
        val v26 = BitVector(k, 26)

        // so only test against values that are smaller
        val bitno = v26.isOneBitDifferent(BitVector(k, 27))
        assertNotNull(bitno)
    }
}