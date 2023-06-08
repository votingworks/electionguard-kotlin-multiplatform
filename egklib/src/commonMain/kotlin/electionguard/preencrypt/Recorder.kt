package electionguard.preencrypt

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.Encryptor

/**
 * The crypto part of the "The Recording Tool".
 * The encrypting/decrypting primaryNonce is done external.
 */
class Recorder(
    val group: GroupContext,
    val manifest: Manifest,
    val publicKey: ElementModP,
    val extendedBaseHash: UInt256,
    val sigma : (UInt256) -> String, // hash trimming function Ω
) {
    val preEncryptor = PreEncryptor( group, manifest, publicKey, extendedBaseHash, sigma)
    val encryptor = Encryptor( group, manifest, ElGamalPublicKey(publicKey), extendedBaseHash)

    /*
    The ballot recording tool receives an election manifest, an identifier for a ballot style, the decrypted
    primary nonce ξ and, for a cast ballot, all the selections made by the voter.
     */
    internal fun MarkedPreEncryptedBallot.record(primaryNonce: UInt256): Pair<RecordedPreBallot, CiphertextBallot> {
        // uses the primary nonce ξ to regenerate all of the encryptions on the ballot
        val preEncryptedBallot = preEncryptor.preencrypt(this.ballotId, this.ballotStyleId, primaryNonce)
        val recordPreBallot = this.makeRecordedPreBallot(preEncryptedBallot)

        // match against the choices in MarkedPreEncryptedBallot
        val markedContests = this.contests.associateBy { it.contestId }

        // Find the pre-encryptions corresponding to the selections made by the voter and, using
        // the encryption nonces derived from the primary nonce, generates proofs of ballot-correctness as in
        // standard ElectionGuard section 3.3.5.
        //
        // If a contest selection limit is greater than one, then homomorphically
        // combine the selected pre-encryption vectors corresponding to the selections made to produce a
        // single vector of encrypted selections. The selected pre-encryption vectors are combined by com-
        // ponentwise multiplication (modulo p), and the derived encryption nonces are added (modulo q)
        // to create suitable nonces for this combined pre-encryption vector. These derived nonces will be
        // necessary to form zero-knowledge proofs that the associated encryption vectors are well-formed.

        val plaintextContests = mutableListOf<PlaintextBallot.Contest>()
        for (preeContest in preEncryptedBallot.contests) {
            val markedContest = markedContests[preeContest.contestId]
            if (markedContest != null) { // ok to skip contests, Encryptor will deal
                plaintextContests.add( markedContest.recordContest(preeContest) )
            }
        }

        val plaintextBallot = PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintextContests,
        )

        // TODO need special form of encryptPre??
        val ciphertextBallot = encryptor.encryptPre(
            plaintextBallot,
            primaryNonce,
            preEncryptedBallot.confirmationCode,
        )

        return Pair(recordPreBallot, ciphertextBallot)
    }

    private fun MarkedPreEncryptedContest.recordContest(preeContest: PreEncryptedContest): PlaintextBallot.Contest {

        val plainSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selectionCode in this.selectedCodes) { // counting on order
            val preeSelection = matchSelection(selectionCode, preeContest) ?:
                throw IllegalArgumentException("Unknown selectionCode ${selectionCode}")
            plainSelections.add( PlaintextBallot.Selection(preeSelection.selectionId, preeSelection.sequenceOrder, 1))
            println("recordContest ${selectionCode} -> vote for selection ${preeSelection.selectionId}")
        }

        return PlaintextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, plainSelections)
    }

    fun matchSelection(selectionCode: String, preeContest: PreEncryptedContest): PreEncryptedSelection? {
        // just brute search, could make faster
        return preeContest.selectionsSorted.find { sigma(it.selectionHash.toUInt256()) == selectionCode }
    }
}