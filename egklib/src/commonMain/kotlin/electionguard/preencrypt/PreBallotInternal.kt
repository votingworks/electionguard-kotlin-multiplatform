package electionguard.preencrypt

import electionguard.core.ElGamalCiphertext
import electionguard.core.UInt256
import electionguard.core.hashElements

/** An internal record used by PreEncryptor and Recorder. */
internal data class PreBallotInternal(
    val ballotId: String,
    val ballotStyleId: String,
    val contests: List<PreContestInternal>,
) {
    fun show() {
        println("\nPreBallotInternal $ballotId = $ballotStyleId")
        for (contest in this.contests) {
            println(" contest ${contest.contestId}")
            for (selection in contest.selections) {
                println("  selection ${selection.selectionId} = ${selection.encrypt0.cryptoHashUInt256()} ${selection.encrypt1.cryptoHashUInt256()}")
            }
        }
    }

    fun makeExternal(
    ) : PreEncryptedBallot {
        // the contest hashes are themselves hashed sequentially to form the ballot’s confirmation code.
        val contestsExternal = this.contests.map { it.makeExternal() }
        return PreEncryptedBallot(
            this.ballotId,
            this.ballotStyleId,
            contestsExternal, // in order
            hashElements(contestsExternal.map { it.preencryptionHash }) // sort
        )
    }
}

internal data class PreContestInternal(
    val contestId: String,
    val sequenceOrder: Int,
    val contestCryptoHash: UInt256,
    val selections: List<PreSelectionInternal>, // must be in sequence order
    val limit: Int,
) {
    fun makeExternal() : PreEncryptedContest {
        val noneVector = this.selections.map { it.encrypt0 }
        val noneHashes = (1..limit).map { hashElements(it, noneVector) }
        val selectionsExternal = this.selections.map { it.makeExternal(this) }
        val selectionHashes = selectionsExternal.map {it.preencryptionHash}

        // The selection hashes and noneHashes are then sorted numerically and hashed together (in sorted order)
        // to produce the contest hash.
        val sortedHashes = (selectionHashes + noneHashes).sortedBy { h -> h.cryptoHashString() }

        return PreEncryptedContest(
            this.contestId,
            this.sequenceOrder,
            selectionsExternal,
            hashElements(sortedHashes), // contest hash
        )
    }
}

internal data class PreSelectionInternal(
    val selectionId: String,
    val sequenceOrder: Int,
    val selectionCryptoHash: UInt256,
    val encrypt0: ElGamalCiphertext, // pre-encryption E(0)
    val encrypt1: ElGamalCiphertext, // pre-encryption E(1)
    val selectionCode : String? = null // could use hash ?
) {

    // the selection vector Vj, for example (E1(0), E2(1), E3(0), E4(0)) when j = 2, n = 4
    fun selectionVector(contest: PreContestInternal): List<ElGamalCiphertext> {
        return contest.selections.map { if (it == this) it.encrypt1 else it.encrypt0 }
    }

    // the selection hash H(Vj)
    fun selectionHash(contest: PreContestInternal): UInt256 {
        return hashElements(selectionVector(contest))
    }

    fun makeExternal(contest: PreContestInternal) : PreEncryptedSelection {
        return PreEncryptedSelection(
            this.selectionId,
            this.sequenceOrder,
            selectionHash(contest), // H(Vj)
        )
    }
}

// we dont know the election code until we create the PreBallotInternal, so now add it
internal fun PreBallotInternal.addSelectionCodes(preEncrypted : PreEncryptedBallot, codeLen : Int) : PreBallotInternal {
    val contestsChanged = this.contests.map {
        val precontest = preEncrypted.contests.find { pre -> pre.contestId == it.contestId}
            ?: throw IllegalArgumentException("Cant find contest ${it.contestId}")
        it.addSelectionCodes(precontest, codeLen)
    }
    return this.copy(contests = contestsChanged)
}

internal fun PreContestInternal.addSelectionCodes(preEncrypted : PreEncryptedContest, codeLen : Int) : PreContestInternal {
    val selectionsChanged = this.selections.map {
        val presection = preEncrypted.selections.find { pre -> pre.selectionId == it.selectionId}
            ?: throw IllegalArgumentException("Cant find selection ${it.selectionId}")
        val match = presection.preencryptionHash.cryptoHashString()
        val selectionCode = match.substring(match.length - codeLen) // get the lase codeLen chars
        it.copy(selectionCode = selectionCode)
    }
    return this.copy(selections = selectionsChanged)
}
