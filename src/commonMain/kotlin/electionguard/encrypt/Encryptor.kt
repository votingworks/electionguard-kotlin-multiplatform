package electionguard.encrypt

import electionguard.ballot.CiphertextBallot
import electionguard.ballot.ElectionContext
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
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
import electionguard.core.isValid
import electionguard.core.randomElementModQ
import electionguard.core.toElementModQ
import electionguard.core.toUInt256
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Encryptor")
private val validate = true

/**
 * Encrypt Plaintext Ballots into Ciphertext Ballots.
 * The input Ballots must be well-formed and consistent.
 * See RunBatchEncryption and BallotInputValidation to validate ballots before passing them to this class.
 */
class Encryptor(
    val group: GroupContext,
    val manifest: Manifest,
    val context : ElectionContext,
) {
    val publicKey = context.jointPublicKey
    val elgamalPublicKey : ElGamalPublicKey = ElGamalPublicKey(publicKey)
    val cryptoExtendedBaseHash = context.cryptoExtendedBaseHash
    val cryptoExtendedBaseHashQ = cryptoExtendedBaseHash.toElementModQ(group)

    // encryptionSeed; see issue 272 in python repo python uses device.crypto_hash,
    // spec says  "extended base hash" :  6.B H0 = H(Qbar)
    fun encrypt(ballots: Iterable<PlaintextBallot>, encryptionSeed: ElementModQ): List<CiphertextBallot> {
        var previousTrackingHash = encryptionSeed
        val encryptedBallots = mutableListOf<CiphertextBallot>()
        for (ballot in ballots) {
            val encryptedBallot = ballot.encryptBallot(previousTrackingHash, group.randomElementModQ())
            encryptedBallots.add(encryptedBallot)
            previousTrackingHash = encryptedBallot.code.toElementModQ(group)
        }
        return encryptedBallots
    }

    fun encrypt(ballot: PlaintextBallot, encryptionSeed: ElementModQ, randomMasterNonce: ElementModQ): CiphertextBallot {
        return ballot.encryptBallot(encryptionSeed, randomMasterNonce)
    }

    /**
     * Encrypt a PlaintextBallot into a CiphertextBallot.
     *
     * This method accepts a ballot representation that only includes `True` selections.
     * It will fill missing selections for a contest with `False` values, and generate `placeholder`
     * selections to represent the number of seats available for a given contest.
     *
     * This method also allows for ballots to exclude passing contests for which the voter made no selections.
     * It will fill missing contests with `False` selections and generate `placeholder` selections that are marked `True`.
     *
     * @param encryptionSeed:  Hash from previous ballot or starting hash from device. python: seed_hash
     * @param randomMasterNonce: the nonce used to encrypt this contest
     */
    fun PlaintextBallot.encryptBallot(
        encryptionSeed: ElementModQ,
        randomMasterNonce: ElementModQ,
    ): CiphertextBallot {
        val ballotNonce: UInt256 = hashElements(manifest.cryptoHashUInt256(), this.ballotId, randomMasterNonce)
        val pcontests = this.contests.associateBy { it.contestId }

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        for (mcontest in manifest.contests) {
            val pcontest : PlaintextBallot.Contest = pcontests[mcontest.contestId]  ?:
                // No contest on the ballot, so create a placeholder
                contestFrom(mcontest)

            encryptedContests.add(pcontest.encryptContest(this.ballotId, mcontest, ballotNonce))
        }

        // Ticks are defined here as number of seconds since the unix epoch (00:00:00 UTC on 1 January 1970)
        val timestamp: Long = getSystemTimeInMillis() / 1000
        val cryptoHash = hashElements(ballotId, manifest.cryptoHashUInt256(), encryptedContests)
        val ballotCode = hashElements(encryptionSeed, timestamp, cryptoHash)

        val encryptedBallot = CiphertextBallot(
            this.ballotId,
            this.ballotStyleId,
            manifest.cryptoHashUInt256(),
            encryptionSeed.toUInt256(),
            ballotCode,
            encryptedContests,
            timestamp,
            cryptoHash,
            randomMasterNonce,
        )
        return encryptedBallot
    }

    fun contestFrom(mcontest: Manifest.ContestDescription): PlaintextBallot.Contest {
        val selections = mcontest.selections.map {selectionFrom(it.selectionId, it.sequenceOrder, false, false)}
        return PlaintextBallot.Contest(mcontest.contestId, mcontest.sequenceOrder, selections)
    }

    /**
     * Encrypt a PlaintextBallotContest into CiphertextBallot.Contest.
     *
     * It will fill missing selections for a contest with `False` values, and generate `placeholder`
     * selections to represent the number of seats available for a given contest.  By adding `placeholder`
     * votes
     *
     * @param mcontest:   the corresponding Manifest.ContestDescription
     * @param ballotNonce:          the seed for this contest.
     */
    fun PlaintextBallot.Contest.encryptContest(
        ballotId: String,
        mcontest: Manifest.ContestDescription,
        ballotNonce: UInt256,
    ): CiphertextBallot.Contest {

        val contestDescriptionHash = mcontest.cryptoHash
        val contestDescriptionHashQ = contestDescriptionHash.toElementModQ(group)
        val nonceSequence = Nonces(contestDescriptionHashQ, ballotNonce)
        val contestNonce = nonceSequence[mcontest.sequenceOrder]
        val chaumPedersenNonce = nonceSequence[0]

        val encryptedSelections = mutableListOf<CiphertextBallot.Selection>()
        val plaintextSelections: Map<String, PlaintextBallot.Selection> =
            this.selections.associateBy { it.selectionId }

        // only use selections that match the manifest.
        var votes = 0
        for (mselection: Manifest.SelectionDescription in mcontest.selections) {

            // Find the actual selection matching the contest description.
            val plaintextSelection = plaintextSelections[mselection.selectionId] ?:
                // No selection was made for this possible value so we explicitly set it to false
                selectionFrom(mselection.selectionId, mselection.sequenceOrder, false, false)

            // track the votes so we can append the appropriate number of true placeholder votes
            votes += plaintextSelection.vote
            val encrypted_selection = plaintextSelection.encryptSelection(
                mselection,
                contestNonce,
                false,
            )
            encryptedSelections.add(encrypted_selection)
        }

        // Add a placeholder selection for each possible vote in the contest
        val limit = mcontest.votesAllowed
        val selectionSequenceOrderMax = mcontest.selections.maxOf { it.sequenceOrder }
        for (placeholder in 1..limit) {
            val sequenceNo = selectionSequenceOrderMax + placeholder
            val plaintextSelection = selectionFrom(
                "${mcontest.contestId}-$sequenceNo", sequenceNo, true,
                votes < limit)
            votes++

            encryptedSelections.add(plaintextSelection.encryptPlaceholder(contestNonce))
        }

        val cryptoHash = hashElements(contestId, mcontest.cryptoHash, encryptedSelections)
        val texts: List<ElGamalCiphertext> = encryptedSelections.map {it.ciphertext}
        val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
        val nonces: Iterable<ElementModQ> = encryptedSelections.map {it.selectionNonce}
        val aggNonce: ElementModQ = with (group) { nonces.addQ() }

        val proof : ConstantChaumPedersenProofKnownNonce = ciphertextAccumulation.constantChaumPedersenProofKnownNonce(
            limit,
            aggNonce,
            elgamalPublicKey,
            chaumPedersenNonce,
            cryptoExtendedBaseHashQ,
        )

        if (validate && !proof.isValid(
                ciphertextAccumulation,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
                limit
        )) {
            logger.warn {"Ballot $ballotId contest $contestId proof does not validate"}
        }

        val encrypted_contest: CiphertextBallot.Contest = CiphertextBallot.Contest(
            this.contestId,
            this.sequenceOrder,
            mcontest.cryptoHash,
            encryptedSelections,
            ciphertextAccumulation,
            cryptoHash,
            proof,
            contestNonce,
        )

        return encrypted_contest
    }

    private fun selectionFrom(selectionId: String, sequenceOrder : Int, is_placeholder: Boolean, is_affirmative: Boolean
    ): PlaintextBallot.Selection {
        return PlaintextBallot.Selection(
            selectionId,
            sequenceOrder,
            if (is_affirmative) 1 else 0,
            is_placeholder,
            null
        )
    }

    /**
     * Encrypt a PlaintextBallot.Selection into a CiphertextBallot.Selection
     *
     * @param selectionDescription:         the Manifest selection
     * @param contestNonce:                 aka "nonce seed"
     * @param isPlaceholder:                if this is a placeholder selection
     */
    fun PlaintextBallot.Selection.encryptSelection(
        selectionDescription: Manifest.SelectionDescription,
        contestNonce: ElementModQ,
        isPlaceholder: Boolean = false,
    ): CiphertextBallot.Selection {
        val nonceSequence = Nonces(selectionDescription.cryptoHash.toElementModQ(group), contestNonce)
        val disjunctiveChaumPedersenNonce: ElementModQ = nonceSequence.get(0)
        val selectionNonce: ElementModQ = nonceSequence.get(selectionDescription.sequenceOrder)

        // Generate the encryption
        val elgamalEncryption: ElGamalCiphertext = this.vote.encrypt(elgamalPublicKey, selectionNonce)

        val proof = elgamalEncryption.disjunctiveChaumPedersenProofKnownNonce(
            this.vote,
            selectionNonce,
            elgamalPublicKey,
            disjunctiveChaumPedersenNonce,
            cryptoExtendedBaseHashQ
        )

        if (validate && !proof.isValid(
                elgamalEncryption,
                elgamalPublicKey,
                cryptoExtendedBaseHashQ,
            )) {
            logger.warn {"Selection $selectionId proof does not validate"}
        }

        val elgamalCrypto = elgamalEncryption.cryptoHashUInt256()
        val cryptoHash = hashElements(selectionId, selectionDescription.cryptoHash, elgamalCrypto)

        val extendedDataCiphertext =
            if (extendedData != null) {
                val extendedDataBytes = extendedData.value.encodeToByteArray()
                val extendedDataNonce = Nonces(selectionNonce, "extended-data")[0]
                extendedDataBytes.hashedElGamalEncrypt(elgamalPublicKey, extendedDataNonce)
            } else null

        val encryptedSelection: CiphertextBallot.Selection = CiphertextBallot.Selection(
            this.selectionId,
            this.sequenceOrder,
            selectionDescription.cryptoHash,
            elgamalEncryption,
            cryptoHash,
            isPlaceholder,
            proof,
            extendedDataCiphertext,
            selectionNonce,
        )
        return encryptedSelection
    }

    fun PlaintextBallot.Selection.encryptPlaceholder(
        contestNonce: ElementModQ,
    ): CiphertextBallot.Selection {

        val cryptoHash = group.randomElementModQ() // random
        val nonceSequence = Nonces(cryptoHash, contestNonce)
        val disjunctiveChaumPedersenNonce: ElementModQ = nonceSequence.get(0)
        val selectionNonce: ElementModQ = nonceSequence.get(this.sequenceOrder)

        // Generate the encryption
        val elgamalEncryption: ElGamalCiphertext = this.vote.encrypt(elgamalPublicKey, selectionNonce)

        val proof = elgamalEncryption.disjunctiveChaumPedersenProofKnownNonce(
            this.vote,
            selectionNonce,
            elgamalPublicKey,
            disjunctiveChaumPedersenNonce,
            cryptoExtendedBaseHashQ
        )

        val encryptedSelection: CiphertextBallot.Selection = CiphertextBallot.Selection(
            this.selectionId,
            this.sequenceOrder,
            cryptoHash.toUInt256(),
            elgamalEncryption,
            cryptoExtendedBaseHash,
            true,
            proof,
            null,
            selectionNonce,
        )
        return encryptedSelection
    }
}