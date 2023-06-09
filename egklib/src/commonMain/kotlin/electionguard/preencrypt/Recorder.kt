package electionguard.preencrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.encrypt.CiphertextBallot
import electionguard.encrypt.encryptContest

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
    val publicKeyEG = ElGamalPublicKey(publicKey)
    val extendedBaseHashQ = extendedBaseHash.toElementModQ(group)
    val preEncryptor = PreEncryptor( group, manifest, publicKey, extendedBaseHash, sigma)

    /*
    The ballot recording tool receives an election manifest, an identifier for a ballot style, the decrypted
    primary nonce ξ and, for a cast ballot, all the selections made by the voter.
     */
    internal fun MarkedPreEncryptedBallot.record(ballotNonce: UInt256, codeBaux : ByteArray = ByteArray(0)): Pair<RecordedPreBallot, CiphertextBallot> {
        // uses the primary nonce ξ to regenerate all of the encryptions on the ballot
        val preEncryptedBallot = preEncryptor.preencrypt(this.ballotId, this.ballotStyleId, ballotNonce)
        val recordPreBallot = this.makeRecordedPreBallot(preEncryptedBallot)
        val timestamp = (getSystemTimeInMillis() / 1000) // secs since epoch

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

        val contests = mutableListOf<CiphertextBallot.Contest>()
        for (preeContest in preEncryptedBallot.contests) {
            val markedContest = markedContests[preeContest.contestId]
            if (markedContest != null) { // ok to skip contests, Encryptor will deal
                contests.add( markedContest.makeContest(ballotNonce, preeContest) )
            }
        }

        //     val ballotId: String,
        //    val ballotStyleId: String,
        //    val confirmationCode: UInt256, // tracking code, H(B), eq 59
        //    val codeBaux: ByteArray, // Baux in eq 59
        //    val contests: List<Contest>,
        //    val timestamp: Long,
        //    val ballotNonce: UInt256,
        //    val isPreEncrypt: Boolean = false,
        val ciphertextBallot =  CiphertextBallot(
            ballotId,
            ballotStyleId,
            preEncryptedBallot.confirmationCode,
            codeBaux,
            contests,
            timestamp,
            ballotNonce,
            true,
        )

        return Pair(recordPreBallot, ciphertextBallot)
    }

    private fun MarkedPreEncryptedContest.makeContest(ballotNonce: UInt256, preeContest: PreEncryptedContest): CiphertextBallot.Contest {
        val selectionLabels = preeContest.selections.map { it.selectionId }

        // assume for the moment only one selected
        val selectionCode = this.selectedCodes[0]
        val preeSelection = matchSelection(selectionCode, preeContest) ?:
            throw IllegalArgumentException("Unknown selectionCode ${selectionCode}")
        println("    makeContest vote for preeSelection ${preeSelection.selectionId} seq=${preeSelection.sequenceOrder}")
        val selections = makeSelectionsLimit1(ballotNonce, preeContest.contestId, preeSelection, selectionLabels)
        val votedFor = if (preeSelection.selectionId.startsWith("null")) 0 else 1

        val texts: List<ElGamalCiphertext> = selections.map { it.ciphertext }
        val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
        val nonces: Iterable<ElementModQ> = selections.map { it.selectionNonce }
        val aggNonce: ElementModQ = with(group) { nonces.addQ() }

        val proof = ciphertextAccumulation.makeChaumPedersen(
            votedFor,      // (ℓ in the spec)
            preeContest.votesAllowed,  // (L in the spec)
            aggNonce,
            publicKeyEG,
            extendedBaseHashQ,
        )

        val contestData = ContestData(
            emptyList(),
            emptyList(),
            ContestDataStatus.normal
        )
        // ξ = H(HE ; 20, ξB , Λ, ”contest data”) (47)
        val contestDataNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, preeContest.contestId, "contest data")
        val contestDataEncrypted = contestData.encrypt(publicKeyEG, preeContest.votesAllowed, contestDataNonce)

        // χl = H(HE ; 23, Λl , K, α1 , β1 , α2 , β2 . . . , αm , βm ). (58)
        val ciphers = mutableListOf<ElementModP>()
        texts.forEach {
            ciphers.add(it.pad)
            ciphers.add(it.data)
        }
        val contestHash = hashFunction(extendedBaseHashQ.byteArray(), 0x23.toByte(), this.contestId, publicKey, ciphers)

        //     data class Contest(
        //        val contestId: String, // matches ContestDescription.contestIdd
        //        val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        //        val contestHash: UInt256, // eq 58
        //        val selections: List<Selection>,
        //        val proof: RangeChaumPedersenProofKnownNonce,
        //        val contestData: HashedElGamalCiphertext,
        //    )
        return CiphertextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, contestHash,
            selections, proof, contestDataEncrypted)
    }

    fun matchSelection(selectionCode: String, preeContest: PreEncryptedContest): PreEncryptedSelection? {
        return preeContest.selections.find { sigma(it.selectionHash.toUInt256()) == selectionCode }
    }

    // assume for the moment only one selected
    private fun makeSelectionsLimit1(ballotNonce : UInt256, contestLabel : String, preeSelection: PreEncryptedSelection,
                               selectionLabels : List<String>): List<CiphertextBallot.Selection> {

        val selections = mutableListOf<CiphertextBallot.Selection>()
        preeSelection.selectionVector.forEachIndexed { idx, encryption ->
            val vote = if (preeSelection.sequenceOrder == idx) 1 else 0 // TODO cant trust sequence order i think
            println("      makeSelections $idx vote $vote")

            // ξi,j,k = H(HE ; 43, ξ, Λi , λj , λk ) eq 97
            val nonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, contestLabel, preeSelection.selectionId, selectionLabels[idx]).toElementModQ(group)

            val proof = encryption.makeChaumPedersen(
                vote,
                1,
                nonce,
                publicKeyEG,
                extendedBaseHashQ
            )
            selections.add( CiphertextBallot.Selection(selectionLabels[idx], idx, encryption, proof, nonce))
        }
        return selections
    }

}