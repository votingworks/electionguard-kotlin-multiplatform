package electionguard.preencrypt

import electionguard.core.UInt256

/**
 * The result of PreEncryptor.preencrypt(), for use by the "Encrypting Tool" to make a pre-encrypted ballot.
 * One must have the primary nonce in order to generate this.
 */
data class PreEncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val contests: List<PreEncryptedContest>,
    val confirmationCode: UInt256,
) {
    fun show() {
        println("\nPreEncryptedBallot $ballotId = $ballotStyleId")
        for (contest in this.contests) {
            println(" contest ${contest.contestId} = ${contest.preencryptionHash.cryptoHashString()}")
            for (selection in contest.selections) {
                println("  selection ${selection.selectionId} = ${selection.preencryptionHash.cryptoHashString()}")
            }
        }
    }
}

data class PreEncryptedContest(
    val contestId: String,
    val sequenceOrder: Int,
    val selections: List<PreEncryptedSelection>,
    val preencryptionHash: UInt256, // could compute
)

data class PreEncryptedSelection(
    val selectionId: String,
    val sequenceOrder: Int,
    val preencryptionHash: UInt256, // H(Vj)
)