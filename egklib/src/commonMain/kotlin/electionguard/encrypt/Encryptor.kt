package electionguard.encrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.ManifestIF
import electionguard.ballot.PlaintextBallot
import electionguard.core.*

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The manifest is expected to have passed manifest validation (see ManifestInputValidation).
 * The input ballots are expected to have passed ballot validation [TODO missing contests added? overvotes checked?]
 * See RunBatchEncryption and BallotInputValidation to validate ballots before passing them to this class.
 */
class Encryptor(
    val group: GroupContext,
    val manifest: ManifestIF,
    val jointPublicKey: ElGamalPublicKey, // aka K
    val extendedBaseHash: UInt256, // aka He
    val votingDevice: String,
) {
    private val extendedBaseHashB = extendedBaseHash.bytes

    /** Encrypt ballots in a chain with starting codeSeed, and random ballotNonce */
    fun encryptChain(ballots: Iterable<PlaintextBallot>, codeSeed: UInt256): List<CiphertextBallot> {
        var previousTrackingHash = codeSeed
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            val encryptedBallot = ballot.encryptBallot(UInt256.random(), null, null, previousTrackingHash.bytes)
            encryptedBallots.add(encryptedBallot)
            previousTrackingHash = encryptedBallot.confirmationCode
        }
        return encryptedBallots
    }

    fun encrypt(ballot: PlaintextBallot, ballotNonce: UInt256? = null, timestampOverride: Long? = null, codeBaux : ByteArray = ByteArray(0)): CiphertextBallot {
        return ballot.encryptBallot( ballotNonce ?: UInt256.random(), null, timestampOverride, codeBaux)
    }

    private fun PlaintextBallot.encryptBallot(
        ballotNonce: UInt256,
        codeOverride: UInt256? = null, // non-null for preencrypt; if null, calculate from spec
        timestampOverride: Long? = null,
        codeBaux : ByteArray = ByteArray(0),
    ): CiphertextBallot {
        val plaintextContests = this.contests.associateBy { it.contestId }

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        for (mcontest in manifest.contestsForBallotStyle(this.ballotStyle)) {
            // If no contest on the ballot, create a well formed contest with all zeroes
            val pcontest = plaintextContests[mcontest.contestId] ?: makeZeroContest(mcontest)
            encryptedContests.add( pcontest.encryptContest(mcontest, ballotNonce))
        }
        val sortedContests = encryptedContests.sortedBy { it.sequenceOrder }

        // H(B) = H(HE ; 24, χ1 , χ2 , . . . , χmB , Baux ).  (59)
        val contestHashes = sortedContests.map { it.contestHash }
        val confirmationCode = codeOverride ?: hashFunction(extendedBaseHash.bytes, 0x24.toByte(), contestHashes, codeBaux)
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000) // secs since epoch

        return CiphertextBallot(
            ballotId,
            ballotStyle,
            votingDevice,
            timestamp,
            codeBaux,
            confirmationCode,
            sortedContests,
            ballotNonce,
            codeOverride != null,
        )
    }

    private fun makeZeroContest(mcontest: ManifestIF.Contest): PlaintextBallot.Contest {
        val selections = mcontest.selections.map { makeZeroSelection(it.selectionId, it.sequenceOrder) }
        return PlaintextBallot.Contest(mcontest.contestId, mcontest.sequenceOrder, selections)
    }

    private fun PlaintextBallot.Contest.encryptContest(
        mcontest: ManifestIF.Contest,
        ballotNonce: UInt256,
    ): CiphertextBallot.Contest {
        val ballotSelections = this.selections.associateBy { it.selectionId }

        val votedFor = mutableListOf<Int>()
        for (mselection: ManifestIF.Selection in mcontest.selections) {
            // Find the ballot selection matching the contest description.
            val plaintextSelection = ballotSelections[mselection.selectionId]
            if (plaintextSelection != null && plaintextSelection.vote > 0) {
                votedFor.add(plaintextSelection.sequenceOrder)
            }
        }

        val totalVotedFor = votedFor.size + this.writeIns.size
        val status = if (totalVotedFor == 0) ContestDataStatus.null_vote
            else if (totalVotedFor < mcontest.votesAllowed)  ContestDataStatus.under_vote
            else if (totalVotedFor > mcontest.votesAllowed)  ContestDataStatus.over_vote
            else ContestDataStatus.normal

        val encryptedSelections = mutableListOf<CiphertextBallot.Selection>()
        for (mselection: ManifestIF.Selection in mcontest.selections) {
            var plaintextSelection = ballotSelections[mselection.selectionId]

            // Set vote to zero if not in manifest or this contest is overvoted
            if (plaintextSelection == null || (status == ContestDataStatus.over_vote)) {
                plaintextSelection = makeZeroSelection(mselection.selectionId, mselection.sequenceOrder)
            }
            encryptedSelections.add( plaintextSelection.encryptSelection(
                ballotNonce,
                this.contestId,
            ))
        }

        val contestData = ContestData(
            if (status == ContestDataStatus.over_vote) votedFor else emptyList(),
            this.writeIns,
            status
        )

        val contestDataEncrypted = contestData.encrypt(jointPublicKey, extendedBaseHash, mcontest.contestId, ballotNonce, mcontest.votesAllowed)

        return this.encryptContest(
            group,
            jointPublicKey,
            extendedBaseHash,
            mcontest.votesAllowed,
            if (status == ContestDataStatus.over_vote) 0 else totalVotedFor,
            encryptedSelections.sortedBy { it.sequenceOrder },
            contestDataEncrypted,
        )
    }

    private fun makeZeroSelection(selectionId: String, sequenceOrder: Int): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selectionId,
            sequenceOrder,
            0,
        )
    }

    private fun PlaintextBallot.Selection.encryptSelection(
        ballotNonce: UInt256,
        contestLabel: String,
    ): CiphertextBallot.Selection {

        // ξi,j = H(HE ; 20, ξB , Λi , λj ). eq 22
        val selectionNonce = hashFunction(extendedBaseHashB, 0x20.toByte(), ballotNonce, contestLabel, this.selectionId)

        return this.encryptSelection(
            this.vote,
            jointPublicKey,
            extendedBaseHash,
            selectionNonce.toElementModQ(group),
        )
    }
}

fun PlaintextBallot.Contest.encryptContest(
    group: GroupContext,
    jointPublicKey: ElGamalPublicKey,
    extendedBaseHash: UInt256,
    votesAllowed: Int, // The number of allowed votes for this contest
    totalVotedFor: Int, // The actual number of selections voted for, for the range proof
    encryptedSelections: List<CiphertextBallot.Selection>,
    extendedDataCiphertext: HashedElGamalCiphertext,
): CiphertextBallot.Contest {

    val ciphertexts: List<ElGamalCiphertext> = encryptedSelections.map { it.ciphertext }
    val ciphertextAccumulation: ElGamalCiphertext = ciphertexts.encryptedSum()
    val nonces: Iterable<ElementModQ> = encryptedSelections.map { it.selectionNonce }
    val aggNonce: ElementModQ = with(group) { nonces.addQ() }

    val proof = ciphertextAccumulation.makeChaumPedersen(
        totalVotedFor,      // (ℓ in the spec)
        votesAllowed,  // (L in the spec)
        aggNonce,
        jointPublicKey,
        extendedBaseHash,
    )

    // χl = H(HE ; 23, Λl , K, α1 , β1 , α2 , β2 . . . , αm , βm ). (58)
    val ciphers = mutableListOf<ElementModP>()
    ciphertexts.forEach {
        ciphers.add(it.pad)
        ciphers.add(it.data)
    }
    val contestHash = hashFunction(extendedBaseHash.bytes, 0x23.toByte(), this.contestId, jointPublicKey.key, ciphers)

    return CiphertextBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        votesAllowed,
        contestHash,
        encryptedSelections,
        proof,
        extendedDataCiphertext,
    )
}

fun PlaintextBallot.Selection.encryptSelection(
    vote: Int,
    jointPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHash: UInt256,
    selectionNonce: ElementModQ,
): CiphertextBallot.Selection {
    val elgamalEncryption: ElGamalCiphertext = vote.encrypt(jointPublicKey, selectionNonce)

    val proof = elgamalEncryption.makeChaumPedersen(
        vote,
        1,
        selectionNonce,
        jointPublicKey,
        cryptoExtendedBaseHash
    )

    return CiphertextBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        elgamalEncryption,
        proof,
        selectionNonce,
    )
}