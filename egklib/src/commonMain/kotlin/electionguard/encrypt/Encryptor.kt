package electionguard.encrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.encrypt
import electionguard.core.encryptedSum
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.randomElementModQ
import electionguard.core.rangeChaumPedersenProofKnownNonce
import electionguard.core.take
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The manifest is expected to have passed manifest validation (see ManifestInputValidation).
 * The input ballots are expected to have passed ballot validation.
 * See RunBatchEncryption and BallotInputValidation to validate ballots before passing them to this class.
 */
class Encryptor(
    val group: GroupContext,
    val manifest: Manifest,
    private val elgamalPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: UInt256,
) {
    private val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)

    /** Encrypt ballots in a chain with starting codeSeed, and random primaryNonce */
    fun encrypt(ballots: Iterable<PlaintextBallot>, codeSeed: ElementModQ): List<CiphertextBallot> {
        var previousTrackingHash = codeSeed
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            val encryptedBallot = ballot.encryptBallot(previousTrackingHash, group.randomElementModQ())
            encryptedBallots.add(encryptedBallot)
            previousTrackingHash = encryptedBallot.code.toElementModQ(group)
        }
        return encryptedBallots
    }

    /** Encrypt ballots with fixed codeSeed, primaryNonce, and timestamp. */
    fun encryptWithFixedNonces(
        ballots: Iterable<PlaintextBallot>,
        codeSeed: ElementModQ,
        primaryNonce: ElementModQ
    ): List<CiphertextBallot> {
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            encryptedBallots.add(ballot.encryptBallot(codeSeed, primaryNonce, 0))
        }
        return encryptedBallots
    }

    /** Encrypt with this codeSeed and primary nonce and optional timestamp and confirmationCode overrides. */
    fun encrypt(
        ballot: PlaintextBallot,
        codeSeed: ElementModQ, // should be UInt256
        primaryNonce: ElementModQ, // should be UInt256
        timestampOverride: Long? = null, // if null, use getSystemTimeInMillis()
        confirmationCode: UInt256? = null // non-null for preencrypt; if null, calculate from spec
    ): CiphertextBallot {
        return ballot.encryptBallot(codeSeed, primaryNonce, timestampOverride, confirmationCode)
    }

    private fun PlaintextBallot.encryptBallot(
        codeSeed: ElementModQ,
        primaryNonce: ElementModQ, // usually random
        timestampOverride: Long? = null,
        confirmationCode: UInt256? = null,
    ): CiphertextBallot {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHashUInt256(), this.ballotId, primaryNonce)
        val plaintextContests = this.contests.associateBy { it.contestId }

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        for (mcontest in manifest.contests) {
            // If no contest on the ballot, create a well formed contest with all zeroes
            val pcontest: PlaintextBallot.Contest = plaintextContests[mcontest.contestId] ?: makeZeroContest(mcontest)
            encryptedContests.add(pcontest.encryptContest(mcontest, ballotNonce))
        }
        val sortedContests = encryptedContests.sortedBy { it.sequenceOrder }
        val cryptoHash = hashElements(ballotId, manifest.cryptoHashUInt256(), sortedContests) // B_i

        // TODO make this simpler, need spec to be clearer.
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000)
        // see spec 1.52, section 3.3.6
        val trackingCode = confirmationCode ?: hashElements(codeSeed, timestamp, cryptoHash)

        return CiphertextBallot(
            ballotId,
            ballotStyleId,
            manifest.cryptoHashUInt256(),
            codeSeed.toUInt256(),
            trackingCode,
            sortedContests,
            timestamp,
            cryptoHash,
            primaryNonce,
            confirmationCode != null, // LOOK lame, is there something better?
        )
    }

    private fun makeZeroContest(mcontest: Manifest.ContestDescription): PlaintextBallot.Contest {
        val selections = mcontest.selections.map { makeZeroSelection(it.selectionId, it.sequenceOrder, false) }
        return PlaintextBallot.Contest(mcontest.contestId, mcontest.sequenceOrder, selections)
    }

    /**
     * Encrypt a PlaintextBallotContest into CiphertextBallot.Contest.
     * @param mcontest:    the corresponding Manifest.ContestDescription
     * @param ballotNonce: the seed for this contest.
     */
    private fun PlaintextBallot.Contest.encryptContest(
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
    ): CiphertextBallot.Contest {
        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val (contestNonce, chaumPedersenNonce, contestDataNonce) = Nonces(contestDescriptionHashQ, ballotNonce).take(3)

        val ballotSelections = this.selections.associateBy { it.selectionId }

        val votedFor = mutableListOf<Int>()
        for (mselection: Manifest.SelectionDescription in mcontest.selections) {
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
        for (mselection: Manifest.SelectionDescription in mcontest.selections) {
            var plaintextSelection = ballotSelections[mselection.selectionId]

            // Set vote to zero if not in manifest or this contest is overvoted
            if (plaintextSelection == null || (status == ContestDataStatus.over_vote)) {
                plaintextSelection = makeZeroSelection(mselection.selectionId, mselection.sequenceOrder, false)
            }
            encryptedSelections.add(plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                false,
            ))
        }

        val contestData = ContestData(
            if (status == ContestDataStatus.over_vote) votedFor else emptyList(),
            this.writeIns,
            status
        )

        return mcontest.encryptContest(
            group,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            if (status == ContestDataStatus.over_vote) 0 else totalVotedFor,
            contestNonce,
            chaumPedersenNonce,
            encryptedSelections.sortedBy { it.sequenceOrder },
            contestData.encrypt(elgamalPublicKey, mcontest.votesAllowed, contestDataNonce),
        )
    }

    private fun makeZeroSelection(selectionId: String, sequenceOrder: Int, voteFor : Boolean): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selectionId,
            sequenceOrder,
            if (voteFor) 1 else 0,
        )
    }

    /**
     * Encrypt a PlaintextBallot.Selection into a CiphertextBallot.Selection
     *
     * @param selectionDescription:         the Manifest selection
     * @param contestNonce:                 aka "nonce seed"
     * @param isPlaceholder:                if this is a placeholder selection
     */
    private fun PlaintextBallot.Selection.encryptSelection(
        selectionDescription: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        isPlaceholder: Boolean = false,
    ): CiphertextBallot.Selection {
        val cryptoHashQ = selectionDescription.cryptoHash.toElementModQ(group)
        val (disjunctiveChaumPedersenNonce, selectionNonce) = Nonces(cryptoHashQ, contestNonce).take(2)

        return selectionDescription.encryptSelection(
            this.vote,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            disjunctiveChaumPedersenNonce,
            selectionNonce,
            isPlaceholder,
        )
    }
}

////  share with Encryptor, BallotPrecompute, ContestPrecompute
fun Manifest.ContestDescription.encryptContest(
    group: GroupContext,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    plaintext: Int, // The actual number of votes=1, for the range proof
    contestNonce: ElementModQ,
    chaumPedersenNonce: ElementModQ,
    encryptedSelections: List<CiphertextBallot.Selection>,
    extendedDataCiphertext: HashedElGamalCiphertext,
): CiphertextBallot.Contest {

    val cryptoHash = hashElements(this.contestId, this.cryptoHash, encryptedSelections)
    val texts: List<ElGamalCiphertext> = encryptedSelections.map { it.ciphertext }
    val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
    val nonces: Iterable<ElementModQ> = encryptedSelections.map { it.selectionNonce }
    val aggNonce: ElementModQ = with(group) { nonces.addQ() }

    val proof = ciphertextAccumulation.rangeChaumPedersenProofKnownNonce(
        plaintext, // The actual plaintext constant value used to make the ElGamal ciphertext (ℓ in the spec)
        this.votesAllowed, // The maximum possible value for the plaintext (inclusive), (L in the spec)
        aggNonce,
        elgamalPublicKey,
        chaumPedersenNonce,
        cryptoExtendedBaseHashQ,
    )

    return CiphertextBallot.Contest(
        this.contestId,
        this.sequenceOrder,
        this.cryptoHash, // manifest contest cryptohash
        encryptedSelections,
        cryptoHash,      // CiphertextBallot.Contest cryptohash
        proof,
        contestNonce,
        extendedDataCiphertext,
    )
}

fun Manifest.SelectionDescription.encryptSelection(
    vote: Int,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    disjunctiveChaumPedersenNonce: ElementModQ,
    selectionNonce: ElementModQ,
    isPlaceholder: Boolean = false,
): CiphertextBallot.Selection {
    val elgamalEncryption: ElGamalCiphertext = vote.encrypt(elgamalPublicKey, selectionNonce)

    val proof = elgamalEncryption.rangeChaumPedersenProofKnownNonce(
        vote,
        1,
        selectionNonce,
        elgamalPublicKey,
        disjunctiveChaumPedersenNonce,
        cryptoExtendedBaseHashQ
    )

    val cryptoHash = hashElements(this.selectionId, this.cryptoHash, elgamalEncryption.cryptoHashUInt256())

    return CiphertextBallot.Selection(
        this.selectionId,
        this.sequenceOrder,
        this.cryptoHash,
        elgamalEncryption,
        cryptoHash,
        isPlaceholder,
        proof,
        selectionNonce,
    )
}