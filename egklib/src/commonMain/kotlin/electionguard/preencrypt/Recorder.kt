package electionguard.preencrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.encrypt.CiphertextBallot

/**
 * The crypto part of the "The Recording Tool".
 * The encrypting/decrypting primaryNonce is done external.
 * TODO :  uncast (implicitly or explicitly challenged) aka SPOILED
 */
class Recorder(
    val group: GroupContext,
    val manifest: ManifestIF,
    val publicKey: ElementModP,
    val extendedBaseHash: UInt256,
    sigma : (UInt256) -> String, // hash trimming function Ω
) {
    val publicKeyEG = ElGamalPublicKey(publicKey)
    val extendedBaseHashQ = extendedBaseHash.toElementModQ(group)
    val preEncryptor = PreEncryptor( group, manifest, publicKey, extendedBaseHash, sigma)

    /*
    The ballot recording tool receives an election manifest, an identifier for a ballot style, the decrypted
    primary nonce ξ and, for a cast ballot, all the selections made by the voter.

    For each uncast (implicitly or explicitly challenged) ballot, the recording tool returns the primary
nonce that enables the encryptions to be opened and checked.
     */
    internal fun MarkedPreEncryptedBallot.record(ballotNonce: UInt256, codeBaux : ByteArray = ByteArray(0)): Pair<RecordedPreBallot, CiphertextBallot> {
        // uses the primary nonce ξ to regenerate all of the encryptions on the ballot
        val preEncryptedBallot = preEncryptor.preencrypt(this.ballotId, this.ballotStyleId, ballotNonce)
        val preBallot = this.makePreBallot(preEncryptedBallot)
        val timestamp = (getSystemTimeInMillis() / 1000) // secs since epoch

        // match against the choices in MarkedPreEncryptedBallot
        val preContests = preBallot.contests.associateBy { it.contestId }

        val contests = mutableListOf<CiphertextBallot.Contest>()
        for (preeContest in preEncryptedBallot.contests) {
            val preContest = preContests[preeContest.contestId]!!
            val cipherContest = preContest.makeContest(ballotNonce, preeContest)
            contests.add( cipherContest)
        }

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

        val recordPreBallot = makeRecordedPreBallot(preBallot)
        return Pair(recordPreBallot, ciphertextBallot)
    }

    private fun PreContest.makeContest(ballotNonce: UInt256, preeContest: PreEncryptedContest): CiphertextBallot.Contest {

        // Find the pre-encryptions corresponding to the selections made by the voter and, using
        // the encryption nonces derived from the primary nonce, generate proofs of ballot correctness as in
        // standard ElectionGuard section 3.3.5.
        //
        // If a contest selection limit is greater than one, then homomorphically
        // combine the selected pre-encryption vectors corresponding to the selections made to produce a
        // single vector of encrypted selections. The selected pre-encryption vectors are combined by component-wise
        // multiplication (modulo p), and the derived encryption nonces are added (modulo q)
        // to create suitable nonces for this combined pre-encryption vector. These derived nonces will be
        // necessary to form zero-knowledge proofs that the associated encryption vectors are well-formed.

        val selections = this.makeSelections(preeContest)

        val texts: List<ElGamalCiphertext> = selections.map { it.ciphertext }
        val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
        val nonces: Iterable<ElementModQ> = selections.map { it.selectionNonce }
        val aggNonce: ElementModQ = with(group) { nonces.addQ() }
        val totalVotes = votedFor.map{ if (it) 1 else 0 }.sum()

        val proof = ciphertextAccumulation.makeChaumPedersen(
            totalVotes,      // (ℓ in the spec)
            preeContest.votesAllowed,  // (L in the spec)
            aggNonce,
            publicKeyEG,
            extendedBaseHash,
        )

        val contestData = ContestData(
            emptyList(),
            emptyList(),
            ContestDataStatus.normal
        )

        val contestDataEncrypted = contestData.encrypt(publicKeyEG, extendedBaseHash, preeContest.contestId, ballotNonce, preeContest.votesAllowed)

        // we are going to substitute preencryptionHash (eq 95) instead of eq 58 when we validate
        // χl = H(HE ; 23, Λl , K, α1 , β1 , α2 , β2 . . . , αm , βm ). (58)
        val ciphers = mutableListOf<ElementModP>()
        texts.forEach {
            ciphers.add(it.pad)
            ciphers.add(it.data)
        }
        val contestHash = hashFunction(extendedBaseHashQ.byteArray(), 0x23.toByte(), this.contestId, publicKey, ciphers)

        return CiphertextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, contestHash,
            selections, proof, contestDataEncrypted)
    }

    private fun PreContest.makeSelections(preeContest: PreEncryptedContest): List<CiphertextBallot.Selection> {

        val nselections = preeContest.selections.size - preeContest.votesAllowed
        val nvectors = this.selectedVectors.size
        require (nvectors == preeContest.votesAllowed)

        // homomorphically combine the selected pre-encryption vectors by component wise multiplication
        val combinedEncryption = mutableListOf<ElGamalCiphertext>()
        repeat(nselections) { idx ->
            val componentEncryptions : List<ElGamalCiphertext> = this.selectedVectors.map { it.encryptions[idx] }
            combinedEncryption.add( componentEncryptions.encryptedSum() )
        }

        // the encryption nonces are added to create suitable nonces
        val combinedNonces = mutableListOf<ElementModQ>()
        repeat(nselections) { idx ->
            val componentNonces : List<ElementModQ> = this.selectedVectors.map { it.nonces[idx] }
            val aggNonce: ElementModQ = with(group) { componentNonces.addQ() }
            combinedNonces.add( aggNonce )
        }

        if (preeContest.votesAllowed == 1) {
            require(combinedEncryption.size == nselections)
            val selectedEncryption = this.selectedVectors[0].encryptions
            repeat(nselections) { idx ->
                require(combinedEncryption[idx] == selectedEncryption[idx])
            }
        }

        val result = mutableListOf<CiphertextBallot.Selection>()
        combinedEncryption.forEachIndexed { idx, encryption ->
            val selection = preeContest.selections[idx]

            val proof = encryption.makeChaumPedersen(
                if (this.votedFor[idx]) 1 else 0,
                1,
                combinedNonces[idx],
                publicKeyEG,
                extendedBaseHash
            )
            result.add( CiphertextBallot.Selection(selection.selectionId, selection.sequenceOrder, encryption, proof, combinedNonces[idx]))
        }
        return result
    }

}