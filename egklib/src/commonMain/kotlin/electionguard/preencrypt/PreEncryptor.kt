package electionguard.preencrypt

import electionguard.ballot.Manifest
import electionguard.core.*

/**
 * The crypto part of the "The Ballot Encrypting Tool"
 * The encrypting/decrypting primaryNonce is done external to this.
 */
class PreEncryptor(
    val group: GroupContext,
    val manifest: Manifest,
    val publicKey: ElementModP,
    val extendedBaseHash: UInt256,
    val sigma : (UInt256) -> String, // hash trimming function Ω
) {
    val publicKeyEG = ElGamalPublicKey(publicKey)

    /* The encrypting tool for pre-encrypted ballots takes as input parameters
        • an election manifest,
        • a ballot id,
        • a ballot style,
        • a primary nonce
      The tool produces the following outputs – which can be used to construct a single pre-encrypted ballot.
        • for each selection, a selection vector, a selection hash, and user-visible short code
        • for each contest, votesAllowed additional null selections vectors, and a contest hash
        • a confirmation code for the ballot
     */
    internal fun preencrypt(
        ballotId: String,
        ballotStyleId: String,
        primaryNonce: UInt256,
        codeBaux : ByteArray = ByteArray(0),
    ): PreEncryptedBallot {
        val mcontests = manifest.styleToContestsMap[ballotStyleId]
            ?: throw IllegalArgumentException("Unknown ballotStyleId $ballotStyleId")

        val preeContests = mcontests.sortedBy { it.sequenceOrder }.map {
            it.preencryptContest(primaryNonce)
        }
        val contestHashes = preeContests.map { it.contestHash }

        // H(B) = H(HE ; 42, χ1 , χ2 , . . . , χmB , Baux ). (96)
        val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x42.toByte(), contestHashes, codeBaux)

        return PreEncryptedBallot(
            ballotId,
            ballotStyleId,
            primaryNonce,
            preeContests,
            confirmationCode,
        )
    }

    private fun Manifest.ContestDescription.preencryptContest(primaryNonce: UInt256): PreEncryptedContest {
        val preeSelections = mutableListOf<PreEncryptedSelection>()

        val sortedSelections = this.selections.sortedBy { it.sequenceOrder }
        val selectionLabels = sortedSelections.map { it.selectionId }
        sortedSelections.map {
            preeSelections.add( preencryptSelection(primaryNonce, this.contestId, it.selectionId, it.sequenceOrder, selectionLabels))
        }

        // In a contest with a selection limit of L, an additional L null vectors are hashed to obtain
        var sequence = this.selections.size
        for (nullVectorIdx in (1..this.votesAllowed)) {
            preeSelections.add( preencryptSelection(primaryNonce, this.contestId, "null${nullVectorIdx}", sequence, selectionLabels))
            sequence++
        }

        // now sort those by selectionHash
        val selectionVectorsSorted = preeSelections.sortedBy { it.selectionHash }
        val selectionHashes = selectionVectorsSorted.map { it.selectionHash.toUInt256() }

        // χl = H(HE ; 41, Λl , K, ψσ(1) , ψσ(2) , . . . , ψσ(m+L) ), (95)
        val contestHash = hashFunction(extendedBaseHash.bytes, 0x41.toByte(), this.contestId, publicKey, selectionHashes)

        return PreEncryptedContest(
            this.contestId,
            this.sequenceOrder,
            this.votesAllowed,
            selectionVectorsSorted,
            contestHash,
        )
    }

    // depends only on the labels.
    private fun preencryptSelection(primaryNonce: UInt256, contestLabel : String, selectionId : String,
                                    sequenceOrder: Int, selectionLabels: List<String>): PreEncryptedSelection {
        val encryptionVector = mutableListOf<ElGamalCiphertext>()
        val hashElements = mutableListOf<ElementModP>()
        selectionLabels.forEach{
            // ξi,j,k = H(HE ; 43, ξ, Λi , λj , λk ) eq 97
            val nonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), primaryNonce, contestLabel, selectionId, it).toElementModQ(group)
            val encoding = if (selectionId == it) 1.encrypt(publicKeyEG, nonce) else 0.encrypt(publicKeyEG, nonce)
            encryptionVector.add(encoding)
            hashElements.add(encoding.pad)
            hashElements.add(encoding.data)
        }

        // here is the selection order dependency
        // ψi = H(HE ; 40, λi , K, α1 , β1 , α2 , β2 . . . , αm , βm ), (eq 93) LOOK selectionId
        val selectionHash = hashFunction(extendedBaseHash.bytes, 0x40.toByte(), /* selectionId, */ publicKey, hashElements)

        return PreEncryptedSelection(
            selectionId,
            sequenceOrder,
            selectionHash.toElementModQ(group),
            sigma(selectionHash),
            encryptionVector,
        )
    }
}