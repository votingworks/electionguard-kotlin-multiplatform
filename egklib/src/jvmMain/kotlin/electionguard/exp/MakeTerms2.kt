package electionguard.exp

// The job is to find an addition chain for all the column vectors.
// I think we can just use BitVectors.
// Do we have to compare the vectors, not the cb integer?
class MakeTerms2(val k: Int, val BitVectors: List<BitVector>, val show: Boolean = false) {
    val working = mutableMapOf<Int, BitVector>() // key = value
    val done = mutableListOf<BitVector>()
    val doneSet = mutableSetOf<ColVector>()

    private val showDetails = true

    init {
        makeWorkingNoDuplicates()

        while (working.size > 0) {
            var changed = removeEmptyAndBaseValues()
            changed = changed || removeSums()
            changed = changed || removeSingleBits()
            changed = changed || splitLargest()
            if (!changed) break // no progress
        }

        if (show) {
            println("done=${done.size}")
            println(buildString {
                getTerms().forEach { appendLine(it) }
            })
        }
        if (show || (working.size > 0)) {
            println(" working=${working.size}")
            println(buildString {
                working.forEach { appendLine(" ${it.value}") }
            })
        }
        if (working.size > 0) {
            throw RuntimeException("working not empty")
        }
    }

    fun getTerms(): List<BitVector> {
        val allsorted = done.sorted()
        allsorted.forEach {
            require(it.sum2 != null)
            if ((it.sum2!!.first + it.sum2!!.second) != it.cv)
                println("HAY")
            require((it.sum2!!.first + it.sum2!!.second) == it.cv)
        }
        return allsorted
    }

    fun makeWorkingNoDuplicates() {
        BitVectors.forEach {
            val org = working[it.cv]
            if (org == null) {
                working[it.cv] = it
            }
        }
        if (show) println(" removeDuplicates ${BitVectors.size - working.size}")
    }

    fun removeEmptyAndBaseValues(): Boolean {
        val removeThese = mutableListOf<BitVector>()
        working.values.forEach {
            if (it.cv.countOneBits() < 2) { // ignore zeros and ones
                removeThese.add(it)
            } else if (it.cv.countOneBits() == 2) { // can use sum of bases
                val bits = it.cv.toNList()
                addToDone(it.setSum(1 shl bits[0], 1 shl bits[1]))
                removeThese.add(it)
            }
        }
        removeThese.forEach { working.remove(it.cv) }
        if (show) println(" removeEmptyAndBaseValues ${removeThese.size}")
        return (removeThese.size > 0)
    }

    fun removeSums(): Boolean {
        if (working.size < 2) return false
        val sorted = working.values.sorted()
        val last = sorted.last().cv
        var count = 0

        for (first in 0 until sorted.size - 1) {
            if (sorted[first].cv + sorted[first + 1].cv > last) break // dont bother continuing
            val firstCV = sorted[first]

            for (second in first + 1 until sorted.size) {
                val secondCV = sorted[second]
                val test = firstCV.cv + secondCV.cv
                val candidate = working[test]
                if (candidate != null) {
                    // that cvs are sums are necessary but not sufficient. must test the bits
                    if (candidate.isSumOf(firstCV, secondCV)) {
                        candidate.sum2 = Pair(firstCV.cv, secondCV.cv)
                        addToDone(candidate)
                        working.remove(candidate.cv)
                        if (show && showDetails) println("   removeSum= $candidate")
                        count++
                    }
                }
                if (test > last) break // dont bother continuing
            }
        }
        if (show) println(" removeSums= $count")
        return (count > 0)
    }

    // look for terms that are sum of existing and one base
    fun removeSingleBits(): Boolean {
        if (working.isEmpty()) return false
        if (done.isEmpty()) return false
        val removeThese = mutableListOf<BitVector>()

        working.values.forEach { candidate ->
            for (doneIdx in 0 until done.size) {
                val test = done[doneIdx]
                if (candidate.cv > test.cv) { // only test smaller values
                    val bitPos = candidate.isOneBitDifferent(test)
                    if (bitPos != null) {
                        candidate.sum2 = Pair(test.cv, 1 shl bitPos)
                        addToDone(candidate)
                        removeThese.add(candidate)
                        if (show) println(" removeSingleBits $candidate succeeded with ${candidate.sum2}")
                        break
                    }
                }
            }
        }
        removeThese.forEach { working.remove(it.cv) }
        if (show) println(" removeSums= ${removeThese.size}")
        return (removeThese.size > 0)
    }

    fun splitLargest(): Boolean {
        if (working.isEmpty()) return false
        val sorted = working.toSortedMap()

        // split the last one, ie with largest value
        val splitt = sorted.values.last()
        val (left, right) = splitt.divide()

        val addleft = addToWorkingOrDone(left)
        val addRight = addToWorkingOrDone(right)

        splitt.setSum(left.cv, right.cv)
        working.remove(splitt.cv)
        addToDone(splitt)

        if (show) println(" splitLargest $splitt succeeded with '$addleft' and '$addRight'")
        return true
    }

    fun addToWorkingOrDone(cv: BitVector): String {
        return if (cv.cv.countOneBits() == 2) { // can use sum of bases
            val bits = cv.cv.toNList()
            addToDone(cv.setSum(1 shl bits[0], 1 shl bits[1]))
            "add ${cv.cv} 2bit to done"
        } else {
            if (working[cv.cv] == null) {
                working[cv.cv] = cv
                "add ${cv.cv} to working"
            } else {
                "already had ${cv.cv} in working"
            }
        }
    }

    fun addToDone(cv: BitVector) {
        if (doneSet.contains(cv.cv)) return
        done.add(cv)
        doneSet.add(cv.cv)
    }
}