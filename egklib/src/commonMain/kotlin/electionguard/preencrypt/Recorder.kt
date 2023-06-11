package electionguard.preencrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.encrypt.CiphertextBallot

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
        val recordContests = recordPreBallot.contests.associateBy { it.contestId }

        val contests = mutableListOf<CiphertextBallot.Contest>()
        for (preeContest in preEncryptedBallot.contests) {
            val recordContest = recordContests[preeContest.contestId]!!

            val cipherContest1 = recordContest.makeContest1(ballotNonce, preeContest)
            val cipherContest = if (preeContest.votesAllowed == 1) recordContest.makeContest1p(ballotNonce, preeContest)
                                else recordContest.makeContest(ballotNonce, preeContest)
            contests.add( cipherContest)
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

    private fun RecordedPreEncryption.makeContest1(ballotNonce: UInt256, preeContest: PreEncryptedContest): CiphertextBallot.Contest {
        val selectionLabels = preeContest.selections.map { it.selectionId } // n + l

        val selectionCode = this.selectedCodes()[0]
        val preeSelection = matchSelection(selectionCode, preeContest)
            ?: throw IllegalArgumentException("Unknown selectionCode ${selectionCode}")

        // println("    makeContest vote for preeSelection ${preeSelection.selectionId} seq=${preeSelection.sequenceOrder}")
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

        return CiphertextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, contestHash,
            selections, proof, contestDataEncrypted)
    }

    fun matchSelection(selectionCode: String, preeContest: PreEncryptedContest): PreEncryptedSelection? {
        return preeContest.selections.find { sigma(it.selectionHash.toUInt256()) == selectionCode }
    }

    private fun makeSelectionsLimit1(ballotNonce : UInt256, contestLabel : String, preeSelection: PreEncryptedSelection,
                                     selectionLabels : List<String>): List<CiphertextBallot.Selection> {

        val selections = mutableListOf<CiphertextBallot.Selection>()
        preeSelection.selectionVector.forEachIndexed { idx, encryption ->
            val vote = if (preeSelection.sequenceOrder == idx) 1 else 0 // TODO cant trust sequence order i think
            // println("      makeSelections $idx vote $vote")

            // ξi,j,k = H(HE ; 43, ξ, Λi , λj , λk ) eq 97
            val nonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, contestLabel, preeSelection.selectionId, selectionLabels[idx]).toElementModQ(group)

            val proof = encryption.makeChaumPedersen(
                vote,
                1,
                nonce,
                publicKeyEG,
                extendedBaseHashQ
            )
            selections.add( CiphertextBallot.Selection(selectionLabels[idx], idx, encryption, proof, nonce)) // LOOK idx
        }
        return selections
    }

    private fun RecordedPreEncryption.makeContest1p(ballotNonce: UInt256, preeContest: PreEncryptedContest): CiphertextBallot.Contest {
        // val selectionLabels = preeContest.selections.map { it.selectionId } // n + l

        val selectedVector = this.selectedVectors[0]

        // println("    makeContest vote for preeSelection ${preeSelection.selectionId} seq=${preeSelection.sequenceOrder}")
        val selections = makeSelectionsLimit1p(ballotNonce, preeContest.contestId, selectedVector, preeContest.selections)
        val votedFor = if (selectedVector.selectionId.startsWith("null")) 0 else 1

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

        return CiphertextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, contestHash,
            selections, proof, contestDataEncrypted)
    }

    private fun makeSelectionsLimit1p(ballotNonce : UInt256, contestLabel : String, selectedVector: RecordedSelectionVector,
                                     preeSelections : List<PreEncryptedSelection>): List<CiphertextBallot.Selection> {

        val result = mutableListOf<CiphertextBallot.Selection>()
        selectedVector.encryptions.forEachIndexed { idx, encryption ->
            val selectionk = preeSelections[idx].selectionId
            val votedFor = if (selectionk == selectedVector.selectionId) 1 else 0

            // ξi,j,k = H(HE ; 43, ξ, Λi , λj , λk ) eq 97
            val nonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, contestLabel, selectedVector.selectionId, selectionk).toElementModQ(group)

            val proof = encryption.makeChaumPedersen(
                votedFor,
                1,
                nonce,
                publicKeyEG,
                extendedBaseHashQ
            )
            result.add( CiphertextBallot.Selection(selectionk, preeSelections[idx].sequenceOrder, encryption, proof, nonce))
        }
        return result
    }

    // fake
    private fun RecordedPreEncryption.makeContest(ballotNonce: UInt256, preeContest: PreEncryptedContest): CiphertextBallot.Contest {

        // Find the pre-encryptions corresponding to the selections made by the voter and, using
        // the encryption nonces derived from the primary nonce, generate proofs of ballot-correctness as in
        // standard ElectionGuard section 3.3.5.
        //
        // If a contest selection limit is greater than one, then homomorphically
        // combine the selected pre-encryption vectors corresponding to the selections made to produce a
        // single vector of encrypted selections. The selected pre-encryption vectors are combined by com-
        // ponentwise multiplication (modulo p), and the derived encryption nonces are added (modulo q)
        // to create suitable nonces for this combined pre-encryption vector. These derived nonces will be
        // necessary to form zero-knowledge proofs that the associated encryption vectors are well-formed.

        // homomorphically combine the selected pre-encryption vectors by componentwise multiplication
        val nselections = preeContest.selections.size
        val nvectors = this.selectedVectors.size
        require (nvectors == preeContest.votesAllowed)

        var combinedEncryption = mutableListOf<ElGamalCiphertext>()
        var combinedEncryptionNonces = mutableListOf<ElementModQ>()
        repeat(nselections) { idx ->
            var componentEncryptions : List<ElGamalCiphertext> = this.selectedVectors.map { it.encryptions[idx] }
            combinedEncryption.add( componentEncryptions.encryptedSum() )
            var componentNonces : List<ElementModQ> = this.selectedVectors.map {
                hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, preeContest.contestId, selectedVector.selectionId, selectionk).toElementModQ(group)
            }
            val aggNonce: ElementModQ = with(group) { componentNonces.addQ() }
            combinedEncryptionNonces.add( aggNonce )
        }
        if (preeContest.votesAllowed == 1) {
            require(combinedEncryption.size == nselections)
            val selectedEncryption = this.selectedVectors[0].encryptions
            repeat(nselections) { idx ->
                require(combinedEncryption[idx] == selectedEncryption[idx])
            }
        }

        // println("    makeContest vote for preeSelection ${preeSelection.selectionId} seq=${preeSelection.sequenceOrder}")
        val selections = makeSelections(ballotNonce, preeContest.contestId, selectedVector, preeContest.selections)
        val votedFor = if (selectedVector.selectionId.startsWith("null")) 0 else 1

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

        return CiphertextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, contestHash,
            selections, proof, contestDataEncrypted)
    }

    private fun RecordedPreEncryption.makeSelections(ballotNonce : UInt256, preeContest: PreEncryptedContest): List<CiphertextBallot.Selection> {

        // homomorphically combine the selected pre-encryption vectors by componentwise multiplication
        val nselections = preeContest.selections.size
        val nvectors = this.selectedVectors.size
        require (nvectors == preeContest.votesAllowed)

        var combinedEncryption = mutableListOf<ElGamalCiphertext>()
        var combinedEncryptionNonces = mutableListOf<ElementModQ>()
        repeat(nselections) { idx ->
            var componentEncryptions : List<ElGamalCiphertext> = this.selectedVectors.map { it.encryptions[idx] }
            combinedEncryption.add( componentEncryptions.encryptedSum() )
            var componentNonces : List<ElementModQ> = this.selectedVectors.map {
                hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, preeContest.contestId, selectedVector.selectionId, selectionk).toElementModQ(group)
            }
            val aggNonce: ElementModQ = with(group) { componentNonces.addQ() }
            combinedEncryptionNonces.add( aggNonce )
        }

        if (preeContest.votesAllowed == 1) {
            require(combinedEncryption.size == nselections)
            val selectedEncryption = this.selectedVectors[0].encryptions
            repeat(nselections) { idx ->
                require(combinedEncryption[idx] == selectedEncryption[idx])
            }
        }

        val result = mutableListOf<CiphertextBallot.Selection>()
        selectedVector.encryptions.forEachIndexed { idx, encryption ->
            val selectionk = preeContest.selections[idx].selectionId
            val votedFor = if (selectionk == selectedVector.selectionId) 1 else 0

            // ξi,j,k = H(HE ; 43, ξ, Λi , λj , λk ) eq 97
            val nonce = hashFunction(extendedBaseHash.bytes, 0x43.toByte(), ballotNonce, preeContest.contestId, selectedVector.selectionId, selectionk).toElementModQ(group)

            val proof = encryption.makeChaumPedersen(
                votedFor,
                1,
                nonce,
                publicKeyEG,
                extendedBaseHashQ
            )
            result.add( CiphertextBallot.Selection(selectionk, preeContest.selections[idx].sequenceOrder, encryption, proof, nonce))
        }
        return result
    }

}