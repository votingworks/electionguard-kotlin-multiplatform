package electionguard.exp


// there are 256 cvints when using ElementModQ
class MakeTerms(val k: Int, val cvints: List<CVInt>, val show: Boolean = false) {
    val working = mutableMapOf<Int, CVInt>() // key = value
    val done = mutableListOf<CVInt>()
    val doneSet = mutableSetOf<ColVector>()

    init {
        makeWorkingNoDuplicates()

        while (working.size > 0) {
            var changed = removeEmptyAndBaseValues()
            changed = changed || removeSums()
            changed = changed || removeSingleBits()
            // changed = changed || splitLargest()
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

    fun getTerms(): List<CVInt> {
        val allsorted = done.sorted()
        allsorted.forEach {
            require(it.sum2 != null)
            require((it.sum2!!.first + it.sum2!!.second) == it.cv)
        }
        return allsorted
    }

    fun makeWorkingNoDuplicates() {
        cvints.forEach {
            val org = working[it.cv]
            if (org == null) {
                working[it.cv] = it
            }
        }
        if (show) println(" removeDuplicates ${cvints.size - working.size}")
    }

    fun removeEmptyAndBaseValues(): Boolean {
        val removeThese = mutableListOf<CVInt>()
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
                val theSum = working[test]
                if (theSum != null && !theSum.done()) {
                    theSum.sum2 = Pair(firstCV.cv, secondCV.cv)
                    addToDone(theSum)
                    working.remove(theSum.cv)
                    count++
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
        val removeThese = mutableListOf<CVInt>()
        working.values.forEach { trial ->
            for (doneIdx in 0 until done.size) {
                val test = trial.cv - done[doneIdx].cv
                if (test > 0 && test.countOneBits() == 1) {
                    trial.sum2 = Pair(test, done[doneIdx].cv) // not orgIdx
                    addToDone(trial)
                    removeThese.add(trial)
                    if (show) println(" removeSingleBits $trial succeeded with ${done[doneIdx]} and ${test}")
                    break
                }
            }
        }
        removeThese.forEach { working.remove(it.cv) }
        if (show) println(" removeSums= ${removeThese.size}")
        return (removeThese.size > 0)
    }

    /*
    fun splitLargest(): Boolean {
        if (working.isEmpty()) return false
        val sorted = working.toSortedMap()

        // split the last one, ie with largest value
        val splitt = sorted.values.last()

        val nonzero = splitt.cv.toNList()
        val size2 = nonzero.size / 2
        val left = binary(nonzero.subList(0, size2), k)
        val right = binary(nonzero.subList(size2, nonzero.size), k)

        //val leftCV = CVInt(nonbinary(left))
        //val rightCV = CVInt(nonbinary(right))

        addToWorkingOrDone(leftCV)
        addToWorkingOrDone(rightCV)

        splitt.setSum(leftCV.cv, rightCV.cv)
        working.remove(splitt.cv)
        addToDone(splitt)

        if (show) println(" splitLargest $splitt succeeded with ${leftCV} and ${rightCV}")
        return true
    }

     */

    fun addToWorkingOrDone(cv: CVInt) {
        if (cv.cv.countOneBits() == 2) { // can use sum of bases
            val bits = cv.cv.toNList()
            addToDone(cv.setSum(1 shl bits[0], 1 shl bits[1]))
        } else {
            if (working[cv.cv] == null) working[cv.cv] = cv
        }
    }

    fun addToDone(cv: CVInt) {
        if (doneSet.contains(cv.cv)) return
        done.add(cv)
        doneSet.add(cv.cv)
    }


}