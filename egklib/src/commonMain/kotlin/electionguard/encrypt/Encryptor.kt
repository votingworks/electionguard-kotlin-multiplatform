package electionguard.encrypt

import electionguard.ballot.ContestData
import electionguard.ballot.ContestDataStatus
import electionguard.ballot.ManifestIF
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.util.ErrorMessages

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
    val encryptingDevice: String,
) {
    private val extendedBaseHashB = extendedBaseHash.bytes

    fun encrypt(
        ballot: PlaintextBallot,
        codeBaux : ByteArray,
        errs: ErrorMessages,
        ballotNonce: UInt256? = null,
        timestampOverride: Long? = null
    ): CiphertextBallot? {
        return ballot.encryptBallot(codeBaux, errs, ballotNonce ?: UInt256.random(), timestampOverride)
    }

    private fun PlaintextBallot.encryptBallot(
        codeBaux : ByteArray,
        errs: ErrorMessages,
        ballotNonce: UInt256,
        timestampOverride: Long? = null,
    ): CiphertextBallot? {
        val plaintextContests = this.contests.associateBy { it.contestId }

        val encryptedContests = mutableListOf<CiphertextBallot.Contest>()
        val manifestContests = manifest.contestsForBallotStyle(this.ballotStyle)
        if (manifestContests == null || manifestContests.isEmpty()) {
            errs.add("Manifest does not have ballotStyle ${this.ballotStyle} or it has no contests for that ballotStyle")
            return null
        }
        for (mcontest in manifestContests) {
            // If no contest on the ballot, create a well formed contest with all zeroes
            val pcontest = plaintextContests[mcontest.contestId] ?: makeZeroContest(mcontest)
            encryptedContests.add(
                pcontest.encryptContest(mcontest,
                    manifest.contestLimit(mcontest.contestId),
                    manifest.optionLimit(mcontest.contestId),
                    ballotNonce)
            )
        }
        val sortedContests = encryptedContests.sortedBy { it.sequenceOrder }

        // H(B) = H(HE ; 0x24, χ1 , χ2 , . . . , χmB , Baux ) ;  spec 2.0.0 eq 58
        val contestHashes = sortedContests.map { it.contestHash }
        val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x24.toByte(), contestHashes, codeBaux)
        val timestamp = timestampOverride ?: (getSystemTimeInMillis() / 1000) // secs since epoch

        return CiphertextBallot(
            ballotId,
            ballotStyle,
            encryptingDevice,
            timestamp,
            codeBaux,
            confirmationCode,
            extendedBaseHash,
            sortedContests,
            ballotNonce,
            false,
        )
    }

    private fun makeZeroContest(mcontest: ManifestIF.Contest): PlaintextBallot.Contest {
        val selections = mcontest.selections.map { makeZeroSelection(it.selectionId, it.sequenceOrder) }
        return PlaintextBallot.Contest(mcontest.contestId, mcontest.sequenceOrder, selections)
    }

    private fun PlaintextBallot.Contest.encryptContest(
        mcontest: ManifestIF.Contest,
        contestLimit: Int,
        optionLimit: Int,
        ballotNonce: UInt256,
    ): CiphertextBallot.Contest {
        val ballotSelections = this.selections.associateBy { it.selectionId }

        val votedFor = mutableListOf<Int>()
        var selectionOvervote = false
        for (mselection: ManifestIF.Selection in mcontest.selections) {
            // Find the ballot selection matching the contest description.
            val plaintextSelection = ballotSelections[mselection.selectionId]
            if (plaintextSelection != null && plaintextSelection.vote > 0) {
                votedFor.add(plaintextSelection.sequenceOrder)
                if (plaintextSelection.vote > optionLimit) {
                    selectionOvervote = true
                }
            }
        }

        val totalVotedFor = votedFor.size + this.writeIns.size
        val status = if (totalVotedFor == 0) ContestDataStatus.null_vote
            else if (selectionOvervote || totalVotedFor > contestLimit)  ContestDataStatus.over_vote
            else if (totalVotedFor < contestLimit)  ContestDataStatus.under_vote
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
                this.sequenceOrder,
                optionLimit,
            ))
        }

        val contestData = ContestData(
            if (status == ContestDataStatus.over_vote) votedFor else emptyList(),
            this.writeIns,
            status
        )

        val contestDataEncrypted = contestData.encrypt(jointPublicKey, extendedBaseHash, mcontest.contestId,
            mcontest.sequenceOrder, ballotNonce, contestLimit)

        return this.encryptContest(
            group,
            jointPublicKey,
            extendedBaseHash,
            contestLimit,
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
        contestIndex: Int,
        optionLimit : Int,
        ): CiphertextBallot.Selection {

        // ξi,j = H(HE ; 0x20, ξB , indc (Λi ), indo (λj )) ; spec 2.0.0 eq 25
        val selectionNonce = hashFunction(extendedBaseHashB, 0x20.toByte(), ballotNonce, contestIndex, this.sequenceOrder)

        return this.encryptSelection(
            this.vote,
            jointPublicKey,
            extendedBaseHash,
            selectionNonce.toElementModQ(group),
            optionLimit,
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
    val ciphertextAccumulation: ElGamalCiphertext = ciphertexts.encryptedSum()?: 0.encrypt(jointPublicKey)
    val nonces: Iterable<ElementModQ> = encryptedSelections.map { it.selectionNonce }
    val aggNonce: ElementModQ = with(group) { nonces.addQ() }

    val proof = ciphertextAccumulation.makeChaumPedersen(
        totalVotedFor, // (ℓ in the spec)
        votesAllowed,  // (L in the spec)
        aggNonce,
        jointPublicKey,
        extendedBaseHash,
    )

    // χl = H(HE ; 0x23, indc (Λl ), K, α1 , β1 , α2 , β2 . . . , αm , βm ) ; spec 2.0.0 eq 57
    val ciphers = mutableListOf<ElementModP>()
    ciphertexts.forEach {
        ciphers.add(it.pad)
        ciphers.add(it.data)
    }
    val contestHash = hashFunction(extendedBaseHash.bytes, 0x23.toByte(), this.sequenceOrder, jointPublicKey.key, ciphers)

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
    optionLimit : Int,
): CiphertextBallot.Selection {
    val elgamalEncryption: ElGamalCiphertext = vote.encrypt(jointPublicKey, selectionNonce) // eq 24

    val proof = elgamalEncryption.makeChaumPedersen(
        vote,
        optionLimit,
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