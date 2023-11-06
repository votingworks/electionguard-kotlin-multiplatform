package electionguard.preencrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.encrypt.CiphertextBallot
import electionguard.util.ErrorMessages

/**
 * The crypto part of the "The Recording Tool".
 * The encrypting/decrypting primaryNonce is done external.
 *
 * TODO :  uncast (implicitly or explicitly challenged) aka SPOILED
 *    "For each uncast (implicitly or explicitly challenged) ballot, the recording tool returns the primary
 *  nonce that enables the encryptions to be opened and checked."
 *    "For an uncast ballot, the wrapper computes the short codes for all possible selections and posts
 *  in the election record the full set of pre-encryption vectors, selection hashes, and short codes for
 *  each possible selection."
 *    "The decryptions of all pre-encryptions correspond to the plaintext values indicated in the
 *  election manifest."
 */
class Recorder(
    val group: GroupContext,
    val manifest: ManifestIF,
    val publicKey: ElementModP,
    val extendedBaseHash: UInt256,
    val votingDevice: String,
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
    internal fun MarkedPreEncryptedBallot.record(
        ballotNonce: UInt256,
        errs: ErrorMessages,
        codeBaux: ByteArray = ByteArray(0),
    ): Pair<RecordedPreBallot, CiphertextBallot>? {

        // uses the primary nonce ξ to regenerate all of the encryptions on the ballot
        val preEncryptedBallot = preEncryptor.preencrypt(this.ballotId, this.ballotStyleId, ballotNonce)
        val preBallot = this.makePreBallot(preEncryptedBallot, errs)
        if (errs.hasErrors()) return null

        // match against the choices in MarkedPreEncryptedBallot
        val preContests = preBallot!!.contests.associateBy { it.contestId }

        val contests = preEncryptedBallot.contests.map {
            val preContest = preContests[it.contestId] ?: errs.addNull("Cant find contest ${it.contestId}") as PreContest?
            preContest?.makeContest(ballotNonce, it, errs.nested("PreContest ${it.contestId}"))
        }
        if (errs.hasErrors()) return null

        val ciphertextBallot =  CiphertextBallot(
            ballotId,
            ballotStyleId,
            votingDevice,
            (getSystemTimeInMillis() / 1000), // secs since epoch
            codeBaux,
            preEncryptedBallot.confirmationCode,
            extendedBaseHash,
            contests.filterNotNull(),
            ballotNonce,
            true,
        )

        val recordPreBallot = makeRecordedPreBallot(preBallot)
        return Pair(recordPreBallot, ciphertextBallot)
    }

    private fun PreContest.makeContest(ballotNonce: UInt256, preeContest: PreEncryptedContest, errs: ErrorMessages): CiphertextBallot.Contest {

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

        val selections = this.makeSelections(preeContest, errs)

        val texts: List<ElGamalCiphertext> = selections.map { it.ciphertext }
        val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()?: 0.encrypt(publicKeyEG)
        val nonces: Iterable<ElementModQ> = selections.map { it.selectionNonce }
        val aggNonce: ElementModQ = with(group) { nonces.addQ() }
        val totalVotes = votedFor.map{ if (it) 1 else 0 }.sum()

        val proof = ciphertextAccumulation.makeChaumPedersen(
            totalVotes,      // (ℓ in the spec)
            preeContest.votesAllowed,  // (L in the spec) TODO remove ?
            aggNonce,
            publicKeyEG,
            extendedBaseHash,
        )

        val contestData = ContestData(
            emptyList(),
            emptyList(),
            ContestDataStatus.normal
        )

        val contestDataEncrypted = contestData.encrypt(publicKeyEG, extendedBaseHash, preeContest.contestId,
            preeContest.sequenceOrder, ballotNonce, preeContest.votesAllowed)

        // we are going to substitute preencryptionHash (eq 94) instead of eq 57 when we validate TODO WTF?
        // χl = H(HE ; 0x23, indc (Λl ), K, α1 , β1 , α2 , β2 . . . , αm , βm ) ; spec 2.0.0 eq 57

        val ciphers = mutableListOf<ElementModP>()
        texts.forEach {
            ciphers.add(it.pad)
            ciphers.add(it.data)
        }
        val contestHash = hashFunction(extendedBaseHashQ.byteArray(), 0x23.toByte(), this.contestIndex, publicKey, ciphers)

        return CiphertextBallot.Contest(preeContest.contestId, preeContest.sequenceOrder, preeContest.votesAllowed,
            contestHash, selections, proof, contestDataEncrypted)
    }

    private fun PreContest.makeSelections(preeContest: PreEncryptedContest, errs: ErrorMessages): List<CiphertextBallot.Selection> {

        val nselections = preeContest.selections.size - preeContest.votesAllowed
        val nvectors = this.selectedVectors.size
        if (nvectors != preeContest.votesAllowed) {
            errs.add("nvectors $nvectors != ${preeContest.votesAllowed} preeContest.votesAllowed")
        }

        // homomorphically combine the selected pre-encryption vectors by component wise multiplication
        val combinedEncryption = mutableListOf<ElGamalCiphertext>()
        repeat(nselections) { idx ->
            val componentEncryptions : List<ElGamalCiphertext> = this.selectedVectors.map { it.encryptions[idx] }
            combinedEncryption.add( componentEncryptions.encryptedSum()?: 0.encrypt(publicKeyEG) )
        }

        // the encryption nonces are added to create suitable nonces
        val combinedNonces = mutableListOf<ElementModQ>()
        repeat(nselections) { idx ->
            val componentNonces : List<ElementModQ> = this.selectedVectors.map { it.nonces[idx] }
            val aggNonce: ElementModQ = with(group) { componentNonces.addQ() }
            combinedNonces.add( aggNonce )
        }

        if (preeContest.votesAllowed == 1) {
            if (nselections != combinedEncryption.size) {
                errs.add("nselections $nselections != ${combinedEncryption.size} combinedEncryption.size")
            }

            val selectedEncryption = this.selectedVectors[0].encryptions
            repeat(nselections) { idx ->
                if (combinedEncryption[idx] != selectedEncryption[idx]) {
                    errs.add("$idx combinedEncryption != selectedEncryption")
                }
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
