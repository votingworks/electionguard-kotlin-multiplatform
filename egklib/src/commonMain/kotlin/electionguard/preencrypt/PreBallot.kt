package electionguard.preencrypt

import electionguard.core.*

/**
 * Intermediate working ballot to transform pre encrypted ballot to an Encrypted ballot.
 * Not externally visable
 */
internal data class PreBallot(
    val ballotId: String,
    val contests: List<PreContest>,
) {
    fun show() {
        println("\nRecordPreBallot '$ballotId' ")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} = ${contest.selectedCodes()}")
            println("   contestHash = ${contest.contestHash.toHex()}")
            println("   selectionHashes (${contest.allSelectionHashes.size}) = ${contest.allSelectionHashes}")
            println("   selectedVectors (${contest.selectedVectors.size}) =")
            contest.selectedVectors.forEach { println("    $it")}
        }
    }
}

internal data class PreContest(
    val contestId: String,
    val contestHash: UInt256,  // (95)
    val allSelectionHashes: List<UInt256>, // nselections + limit, numerically sorted
    val selectedVectors: List<PreSelectionVector>, // limit number of them, sorted by selectionHash
    val votedFor: List<Boolean> // nselections, in order by sequence_order
) {
    init {
        require(votedFor.size == allSelectionHashes.size - selectedVectors.size)
    }
    fun selectedCodes() : List<String> = selectedVectors.map { it.shortCode }
    fun nselections() = votedFor.size
}

internal data class PreSelectionVector(
    val selectionId: String, // do not serialize
    val selectionHash: ElementModQ, // ψi (93)
    val shortCode: String,
    val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
    val nonces: List<ElementModQ>, // size = nselections, in order by sequence_order, do not serialize
) {
    override fun toString() =
        buildString {
            append(" shortCode=$shortCode")
            append(" selectionHash=$selectionHash\n")
            encryptions.forEach { append("       encryption $it\n") }
        }
}

internal fun MarkedPreEncryptedBallot.makePreBallot(preeBallot : PreEncryptedBallot): PreBallot {
    val contests = mutableListOf<PreContest>()
    preeBallot.contests.forEach { preeContest ->
        val markedContest = this.contests.find { it.contestId == preeContest.contestId }
            ?: throw IllegalArgumentException("Cant find ${preeContest.contestId}")

        // find the selected selections
        val selected = mutableListOf<PreEncryptedSelection>()
        markedContest.selectedCodes.map { selectedShortCode ->
            val selection = preeContest.selections.find { it.shortCode == selectedShortCode } ?: throw RuntimeException()
            if (selection != null) selected.add(selection)
        }

        val nselections = preeContest.selections.size - preeContest.votesAllowed
        val votedFor = mutableListOf<Boolean>()
        repeat(nselections) { idx ->
            val selection = preeContest.selections[idx]
            votedFor.add( selected.find { it.selectionId == selection.selectionId } != null)
        }

        // add null vector on undervote
        val votesMissing = preeContest.votesAllowed - selected.size
        repeat (votesMissing) {
            val nullVector = findNullVectorNotSelected(preeContest.selections, selected)
            selected.add(nullVector)
        }
        require (selected.size == preeContest.votesAllowed)

        // The selectionVectors are sorted numerically by selectionHash, so cant be associated with a selection
        val sortedSelectedVectors = selected.sortedBy { it.selectionHash }
        val sortedRecordedVectors = sortedSelectedVectors.map { preeSelection ->
            PreSelectionVector(preeSelection.selectionId, preeSelection.selectionHash, preeSelection.shortCode,
                preeSelection.selectionVector, preeSelection.selectionNonces)
        }
        val allSortedSelectedHashes = preeContest.selections.sortedBy { it.selectionHash }.map { it.selectionHash.toUInt256() }

        contests.add(
             PreContest(
                 preeContest.contestId,
                 preeContest.preencryptionHash,
                 allSortedSelectedHashes,
                 sortedRecordedVectors,
                 votedFor,
            )
        )
    }

    return PreBallot(
        this.ballotId,
        contests,
    )
}

// find a null vector not already in selections
private fun findNullVectorNotSelected(allSelections : List<PreEncryptedSelection>, selections : List<PreEncryptedSelection>) : PreEncryptedSelection {
    allSelections.forEach { it ->
        if (it.selectionId.startsWith("null")) {
            if (null == selections.find{ have -> have.selectionId == it.selectionId }) {
                return it
            }
        }
    }
    throw RuntimeException()
}