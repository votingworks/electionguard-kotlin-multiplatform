package electionguard.preencrypt

import electionguard.ballot.ManifestIF
import electionguard.core.*

/**
 * The crypto part of the "The Ballot Encrypting Tool"
 * The encrypting/decrypting primaryNonce is done external to this.
 */
class PreEncryptor(
    val group: GroupContext,
    val manifest: ManifestIF,
    val publicKey: ElementModP,
    val extendedBaseHash: UInt256,
    val sigma : (UInt256) -> String, // hash trimming function Ω
) {
    private val publicKeyEG = ElGamalPublicKey(publicKey)

    /* The encrypting tool for pre-encrypted ballots takes as input parameters
        • an election manifest,
        • a ballot id,
        • a ballot style,
        • a primary nonce
      The tool produces the following outputs – which can be used to construct a single pre-encrypted ballot:
        • for each selection, a selection vector, a selection hash, and user-visible short code
        • for each contest, votesAllowed additional null selections vectors, and a contest hash
        • a confirmation code for the ballot
     */
    internal fun preencrypt(ballotId: String, ballotStyleId: String, primaryNonce: UInt256,
                            codeBaux : ByteArray = ByteArray(0)
    ): PreEncryptedBallot {

        val mcontests = manifest.contestsForBallotStyle(ballotStyleId)
        val preeContests = mcontests.sortedBy { it.sequenceOrder }.map {
            it.preencryptContest(manifest.contestLimit(it.contestId), primaryNonce)
        }
        val contestHashes = preeContests.map { it.preencryptionHash }

        // H(B) = H(HE ; 0x42, χ1 , χ2 , . . . , χmB , Baux ). (95)
        val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x42.toByte(), contestHashes, codeBaux)

        return PreEncryptedBallot(
            ballotId,
            ballotStyleId,
            primaryNonce,
            preeContests,
            confirmationCode,
        )
    }

    private fun ManifestIF.Contest.preencryptContest(contestLimit: Int, primaryNonce: UInt256): PreEncryptedContest {
        val preeSelections = mutableListOf<PreEncryptedSelection>()

        // make sure selections are sorted by sequence number
        val sortedSelections = this.selections.sortedBy { it.sequenceOrder }
        val sortedSelectionIndices = sortedSelections.map { it.sequenceOrder }
        sortedSelections.map {
            preeSelections.add( preencryptSelection(primaryNonce, this.sequenceOrder, it.selectionId, it.sequenceOrder, sortedSelectionIndices))
        }

        // In a contest with a selection limit of L, an additional L null vectors are added
        var sequence = this.selections.size
        for (nullVectorIdx in (1..contestLimit)) {
            // TODO null labels may be in manifest, see 4.2.1
            preeSelections.add( preencryptSelection(primaryNonce, this.sequenceOrder, "null${nullVectorIdx}", sequence, sortedSelectionIndices))
            sequence++
        }

        // numerically sorted selectionHashes
        val selectionHashes = preeSelections.sortedBy { it.selectionHash }.map { it.selectionHash.toUInt256safe() }

        // χl = H(HE ; 0x41, indc (Λl ), K, ψσ(1) , ψσ(2) , . . . , ψσ(m+L) ) ; 94
        val preencryptionHash = hashFunction(extendedBaseHash.bytes, 0x41.toByte(), this.sequenceOrder, publicKey, selectionHashes)

        return PreEncryptedContest(
            this.contestId,
            this.sequenceOrder,
            contestLimit,
            preeSelections,
            preencryptionHash,
        )
    }

    // A PreEncryptedSelection for thisSelectionId being voted for
    private fun preencryptSelection(primaryNonce: UInt256, contestIndex : Int, thisSelectionId : String,
                                    thisSelectionIndex: Int, allSelectionIndices: List<Int>): PreEncryptedSelection {

        val encryptionVector = mutableListOf<ElGamalCiphertext>()
        val encryptionNonces = mutableListOf<ElementModQ>()
        val hashElements = mutableListOf<ElementModP>()
        allSelectionIndices.forEach{
            // ξi,j,k = H(HE ; 0x43, ξ, indc (Λi ), indo (λj ), indo (λk )) eq 96
            val nonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), primaryNonce, contestIndex, thisSelectionIndex, it).toElementModQ(group)
            val encoding = if (thisSelectionIndex == it) 1.encrypt(publicKeyEG, nonce) else 0.encrypt(publicKeyEG, nonce)
            encryptionVector.add(encoding)
            encryptionNonces.add(nonce)
            hashElements.add(encoding.pad)
            hashElements.add(encoding.data)
        }

        // here is the selection order dependency
        // ψi = H(HE ; 0x40, K, α1 , β1 , α2 , β2 . . . , αm , βm ), (eq 92)
        val selectionHash = hashFunction(extendedBaseHash.bytes, 0x40.toByte(), publicKey, hashElements)

        return PreEncryptedSelection(
            thisSelectionId,
            thisSelectionIndex,
            selectionHash.toElementModQ(group),
            sigma(selectionHash),
            encryptionVector,
            encryptionNonces,
        )
    }
}