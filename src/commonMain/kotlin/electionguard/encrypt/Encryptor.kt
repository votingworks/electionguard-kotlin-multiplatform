package electionguard.encrypt

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import electionguard.core.Nonces
import electionguard.core.UInt256
import electionguard.core.constantChaumPedersenProofKnownNonce
import electionguard.core.disjunctiveChaumPedersenProofKnownNonce
import electionguard.core.encrypt
import electionguard.core.encryptedSum
import electionguard.core.get
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The input Ballots must be well-formed and consistent.
 * See RunBatchEncryption and BallotInputValidation to validate ballots before passing them to this class.
 */
class Encryptor(
    val group: GroupContext,
    val manifest: Manifest,
    private val elgamalPublicKey: ElGamalPublicKey,
    val cryptoExtendedBaseHash: UInt256,
) {
    private val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)

    /** Encrypt ballots in a chain with starting codeSeed, and random masterNonce */
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

    /** Encrypt ballots with fixed codeSeed, masterNonce, and timestamp. */
    fun encryptWithFixedNonces(
        ballots: Iterable<PlaintextBallot>,
        codeSeed: ElementModQ,
        masterNonce: ElementModQ
    ): List<CiphertextBallot> {
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            encryptedBallots.add(ballot.encryptBallot(codeSeed, masterNonce, 0))
        }
        return encryptedBallots
    }

    /** Encrypt the ballot with the given nonces and an optional timestamp override. */
    fun encrypt(
        ballot: PlaintextBallot,
        codeSeed: ElementModQ,
        masterNonce: ElementModQ,
        timestampOverride: Long? = null
    ): CiphertextBallot {
        return ballot.encryptBallot(codeSeed, masterNonce, timestampOverride)
    }

    private fun PlaintextBallot.encryptBallot(
        codeSeed: ElementModQ,
        masterNonce: ElementModQ,
        timestampOverride: Long? = null,
    ): CiphertextBallot {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHashUInt256(), this.ballotId, masterNonce)
        val plaintextContests = this.contests.associateBy { it.contestId }

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        for (mcontest in manifest.contests) {
            // If no contest on the ballot, create a placeholder
            val pcontest: PlaintextBallot.Contest = plaintextContests[mcontest.contestId] ?: contestFrom(mcontest)
            encryptedContests.add(pcontest.encryptContest(mcontest, ballotNonce))
        }
        val sortedContests = encryptedContests.sortedBy { it.sequenceOrder }

        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000)
        val cryptoHash = hashElements(ballotId, manifest.cryptoHashUInt256(), sortedContests)
        val trackingCode = hashElements(codeSeed, timestamp, cryptoHash)

        return CiphertextBallot(
            ballotId,
            ballotStyleId,
            manifest.cryptoHashUInt256(),
            codeSeed.toUInt256(),
            trackingCode,
            sortedContests,
            timestamp,
            cryptoHash,
            masterNonce,
        )
    }

    private fun contestFrom(mcontest: Manifest.ContestDescription): PlaintextBallot.Contest {
        val selections = mcontest.selections.map { selectionFrom(it.selectionId, it.sequenceOrder, false) }
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
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group) // LOOK why? because Nonces wants a Q
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[mcontest.sequenceOrder]
        val chaumPedersenNonce = nonceSequence[0]

        val encryptedSelections = mutableListOf<CiphertextBallot.Selection>()
        val plaintextSelections = this.selections.associateBy { it.selectionId }

        // only use selections that match the manifest.
        var votes = 0
        for (mselection: Manifest.SelectionDescription in mcontest.selections) {

            // Find the actual selection matching the contest description.
            val plaintextSelection = plaintextSelections[mselection.selectionId] ?:
            // No selection was made for this possible value so we explicitly set it to false
            selectionFrom(mselection.selectionId, mselection.sequenceOrder, false)

            // track the votes so we can append the appropriate number of true placeholder votes
            votes += plaintextSelection.vote
            val encryptedSelection = plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                false,
            )
            encryptedSelections.add(encryptedSelection)
        }

        // Add a placeholder selection for each possible vote in the contest
        val limit = mcontest.votesAllowed
        val selectionSequenceOrderMax = mcontest.selections.maxOf { it.sequenceOrder }
        for (placeholder in 1..limit) {
            val sequenceNo = selectionSequenceOrderMax + placeholder
            val plaintextSelection = selectionFrom(
                "${mcontest.contestId}-$sequenceNo", sequenceNo, votes < limit
            )
            val mselection = Manifest.SelectionDescription(plaintextSelection.selectionId, plaintextSelection.sequenceOrder, "placeholder")
            val encryptedPlaceholder= plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                true,
            )
            encryptedSelections.add(encryptedPlaceholder)
            votes++
        }

        return mcontest.encryptContest(
            group,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            contestNonce,
            chaumPedersenNonce,
            encryptedSelections.sortedBy { it.sequenceOrder },
        )
    }

    private fun selectionFrom(selectionId: String, sequenceOrder: Int, is_affirmative: Boolean
    ): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selectionId,
            sequenceOrder,
            if (is_affirmative) 1 else 0,
            null,
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
        val nonceSequence = Nonces(selectionDescription.cryptoHash.toElementModQ(group), contestNonce)
        val disjunctiveChaumPedersenNonce: ElementModQ = nonceSequence[0]
        val selectionNonce: ElementModQ = nonceSequence[selectionDescription.sequenceOrder]

        // TODO: test
        val extendedDataCiphertext =
            if (extendedData != null) {
                val extendedDataBytes = extendedData.encodeToByteArray()
                val extendedDataNonce = Nonces(selectionNonce, "extended-data")[0]
                extendedDataBytes.hashedElGamalEncrypt(elgamalPublicKey, extendedDataNonce)
            } else null

        return selectionDescription.encryptSelection(
            this.vote,
            elgamalPublicKey,
            cryptoExtendedBaseHashQ,
            disjunctiveChaumPedersenNonce,
            selectionNonce,
            isPlaceholder,
            extendedDataCiphertext,
        )
    }
}

////  share with Encryptor, BallotPrecompute, ContestPrecompute
fun Manifest.SelectionDescription.encryptSelection(
    vote: Int,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    disjunctiveChaumPedersenNonce: ElementModQ,
    selectionNonce: ElementModQ,
    isPlaceholder: Boolean = false,
    extendedDataCiphertext: HashedElGamalCiphertext?,
): CiphertextBallot.Selection {
    val elgamalEncryption: ElGamalCiphertext = vote.encrypt(elgamalPublicKey, selectionNonce)

    val proof = elgamalEncryption.disjunctiveChaumPedersenProofKnownNonce(
        vote,
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
        extendedDataCiphertext,
        selectionNonce,
    )
}

////  share with Encryptor, BallotPrecompute, ContestPrecompute
fun Manifest.ContestDescription.encryptContest(
    group: GroupContext,
    elgamalPublicKey: ElGamalPublicKey,
    cryptoExtendedBaseHashQ: ElementModQ,
    contestNonce: ElementModQ,
    chaumPedersenNonce: ElementModQ,
    encryptedSelections: List<CiphertextBallot.Selection>,
): CiphertextBallot.Contest {

    val cryptoHash = hashElements(this.contestId, this.cryptoHash, encryptedSelections)
    val texts: List<ElGamalCiphertext> = encryptedSelections.map { it.ciphertext }
    val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
    val nonces: Iterable<ElementModQ> = encryptedSelections.map { it.selectionNonce }
    val aggNonce: ElementModQ = with(group) { nonces.addQ() }

    val proof = ciphertextAccumulation.constantChaumPedersenProofKnownNonce(
        this.votesAllowed,
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
    )
}