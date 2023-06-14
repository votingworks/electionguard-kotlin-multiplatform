package electionguard.preencrypt

import electionguard.core.*

/**
 * The result of RecordPreBallot.record(), for use by the "Recording Tool" processing a marked pre-encrypted ballot.
 * Used to serialize a pre-encrypted ballot after its been voted.
 * Has the extra Preencryption info for the EncryptedBallot.
 */
data class RecordedPreBallot(
    val ballotId: String,
    val contests: List<RecordedPreEncryption>,
) {
    fun show() {
        println("\nRecordPreBallot '$ballotId' ")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} = ${contest.selectedCodes()}")
            println("   selectionHashes (${contest.allSelectionHashes.size}) = ${contest.allSelectionHashes}")
            println("   selectedVectors (${contest.selectedVectors.size}) =")
            contest.selectedVectors.forEach { println("    $it")}
        }
    }
}

data class RecordedPreEncryption(
    val contestId: String,
    val preencryptionHash: UInt256,  // (95)
    val allSelectionHashes: List<UInt256>, // nselections + limit, numerically sorted
    val selectedVectors: List<RecordedSelectionVector>, // limit number of them, sorted by selectionHash
) {
    fun selectedCodes() : List<String> = selectedVectors.map { it.shortCode }
}

data class RecordedSelectionVector(
    val selectionId: String, // do not serialize
    val selectionHash: ElementModQ, // ψi (93)
    val shortCode: String,
    val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
) {
    override fun toString() =
        buildString {
            append(" shortCode=$shortCode")
            append(" selectionHash=$selectionHash\n")
            encryptions.forEach { append("       encryption $it\n") }
        }
}

internal fun makeRecordedPreBallot(preeBallot : PreBallot): RecordedPreBallot {
    val contests = mutableListOf<RecordedPreEncryption>()
    preeBallot.contests.forEach { preeContest ->

        // make the selections
        val selectedVectors = preeContest.selectedVectors.map {
            RecordedSelectionVector(it.selectionId, it.selectionHash, it.shortCode, it.encryptions)
        }

        contests.add(
            RecordedPreEncryption(
                preeContest.contestId,
                preeContest.preencryptionHash,
                preeContest.allSelectionHashes,
                selectedVectors,
            )
        )
    }

    return RecordedPreBallot(
        preeBallot.ballotId,
        contests,
    )
}