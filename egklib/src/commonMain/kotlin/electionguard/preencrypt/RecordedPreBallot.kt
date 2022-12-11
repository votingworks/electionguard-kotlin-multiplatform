package electionguard.preencrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.UInt256
import electionguard.core.hashElements

/**
 * The result of RecordPreBallot.record(), for use by the "Recording Tool" processing a marked pre-encrypted ballot.
 * TODO unfinished
 */
data class RecordedPreBallot(
    val ballotId: String,
    val contests: List<RecordedPreContest>,
) {
    fun show() {
        println("\nRecordPreBallot $ballotId")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} = ${contest.selectedCodes}")
            println("   preencryptionHash = ${contest.contestHash.cryptoHashString()}")
            println("   selectionHashes = ${contest.selectionVectors.map {it.selectionHash}}")
        }
    }
}

data class RecordedPreContest(
    val contestId: String,

    val selectionVectors: List<RecordedSelectionVector> = emptyList(), // sorted by selectionHash
    val selectedCodes: List<String>, // limit number of them; LOOK are we padding?
) {
    val contestHash by lazy { hashElements(selectionVectors.map { it.selectionHash }) }
}

data class RecordedSelectionVector(
    val selectionVector: List<ElGamalCiphertext>, // Vj, size = nselections, in order by sequenceOrder
    val selectionHash: UInt256, // H(Vj)
) {
    val code: String = makeCode(selectionHash, 4)
}

internal fun makeRecordedPreBallot(marked: MarkedPreEncryptedBallot, preInternal: PreBallotInternal): RecordedPreBallot {
    val contests = mutableListOf<RecordedPreContest>()
    preInternal.contests.forEach { preContest ->
        val mcontest = marked.contests.find { it.contestId == preContest.contestId }
            ?: throw IllegalArgumentException("Cant find ${preContest.contestId}")

        // LOOK all the noneVector are the same. must be wrong.
        val noneVector = preContest.selections.map { it.encrypt0 }
        val noneVectors = (1..preContest.limit)
            .map { RecordedSelectionVector(noneVector, hashElements(it, noneVector)) }
        val selectionVectors= preContest.selections.map {
            val sv = it.selectionVector(preContest)
            RecordedSelectionVector(sv, hashElements(sv))
        }

        // The selectionVectors and noneVectors are sorted numerically by selectionHash, so cant be associated with a selection
        val sortedSelectionVectors = (selectionVectors + noneVectors).sortedBy { h -> h.selectionHash.cryptoHashString() }

        contests.add(
            RecordedPreContest(
                preContest.contestId,
                sortedSelectionVectors,
                mcontest.selectedCodes,
            )
        )
    }

    return RecordedPreBallot(
        marked.ballotId,
        contests,
    )
}

private fun makeCode(selectionHash: UInt256, codeLen : Int) : String {
    val match = selectionHash.cryptoHashString()
    return match.substring(match.length - codeLen) // get the last codeLen chars
}