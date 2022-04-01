package electionguard.ballot

import electionguard.core.*

data class CiphertextBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val manifestHash: UInt256, // matches Manifest.cryptoHash
//    val codeSeed: UInt256,
//    val code: UInt256,
    val contests: List<Contest>,
    val timestamp: Long?,
    val cryptoHash: UInt256,
) {
    data class Contest(
        val contestId: String, // matches ContestDescription.contestIdd
        val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val contestHash: UInt256, // matches ContestDescription.cryptoHash
        val selections: List<Selection>,
        val ciphertextAccumulation: ElGamalCiphertext, // TODO: we could potentially remove this, yield smaller contest structure
        val cryptoHash: UInt256,
        val proof: ConstantChaumPedersenProofKnownNonce,
    )  : CryptoHashableUInt256 {
        override fun cryptoHashUInt256() = cryptoHash
    }

    data class Selection(
        val selectionId: String, // matches SelectionDescription.selectionId
        val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        val selectionHash: UInt256, // matches SelectionDescription.cryptoHash
        val ciphertext: ElGamalCiphertext,
        val cryptoHash: UInt256,
        val isPlaceholderSelection: Boolean,
        val proof: DisjunctiveChaumPedersenProofKnownNonce,
        val extendedData: HashedElGamalCiphertext?,
        val selectionNonce: ElementModQ, // TODO: validate that this nonce is never serialized
    )  : CryptoHashableUInt256 {
        override fun cryptoHashUInt256() = cryptoHash
    }
}

fun PlaintextBallot.Selection.encrypt(
    descriptionHash: UInt256,
    key: ElGamalPublicKey,
    cryptoExtendedBaseHash: ElementModQ,
    proofSeed: ElementModQ,
    nonce: ElementModQ? = null,
): CiphertextBallot.Selection {
    if (vote < 0 || vote > 1) {
        throw IllegalArgumentException("ballot encryption only currently supported for 0-or-1 counters")
    }

    val context = compatibleContextOrFail(key.key, cryptoExtendedBaseHash, proofSeed, nonce)

    val encryptionNonce = nonce ?: context.randomElementModQ()
    val ciphertext = vote.encrypt(key, encryptionNonce)

    val extendedDataCiphertext =
        if (extendedData != null) {
            val extendedDataBytes = extendedData.value.encodeToByteArray()
            val extendedDataNonce = Nonces(encryptionNonce, "extended-data")[0]
            extendedDataBytes.hashedElGamalEncrypt(key, extendedDataNonce)
        } else null


    return CiphertextBallot.Selection(
        selectionId = selectionId,
        sequenceOrder = sequenceOrder,
        selectionHash = descriptionHash,
        ciphertext = ciphertext,
        cryptoHash = hashElements(selectionId, descriptionHash, ciphertext),
        isPlaceholderSelection = isPlaceholderSelection,
        proof = ciphertext.disjunctiveChaumPedersenProofKnownNonce(vote, encryptionNonce, key, proofSeed, cryptoExtendedBaseHash),
        extendedData = extendedDataCiphertext,
        selectionNonce = encryptionNonce
    )
}

// TODO: work out a variant on this function where the ballot selections aren't available at once.
//   Maybe do it as an object that holds all the necessary state and has methods that mutate the
//   internal state of the object?
fun PlaintextBallot.Contest.encrypt(
    descriptionHash: UInt256,
    key: ElGamalPublicKey,
    cryptoExtendedBaseHash: ElementModQ,
    proofSeed: ElementModQ,
    nonce: ElementModQ? = null,
): CiphertextBallot.Contest {
    var contestSelections = this.selections
    if (maxSelections < 1 || maxSelections > contestSelections.size) {
        throw IllegalArgumentException("maxSelections ($maxSelections) must be from 1 to the size of the contest (${contestSelections.size})")
    }

    var numberSelected = contestSelections.sumOf { it.vote }
    val numNormalSelections = contestSelections.size

    if (numberSelected > maxSelections) {
        // We have an overvoted ballot, so we'll "interpret" it to an undervote, since
        // we have no way of representing an overvote where the proofs will work.
        numberSelected = 0
        contestSelections = contestSelections.map { it.copy(vote = 0) }
    }

    val context = compatibleContextOrFail(key.key, cryptoExtendedBaseHash, proofSeed, nonce)

    val encryptionSeedNonce = nonce ?: context.randomElementModQ()
    val encryptionNonces = Nonces(encryptionSeedNonce, "contest-encrypt")
    val proofNonces = Nonces(proofSeed, "contest-encryption-proofs")
    val constantProofNonce = proofNonces[contestSelections.size + maxSelections]

    val encryptedSelections = contestSelections.mapIndexed {index, selection ->
        selection.encrypt(
            descriptionHash = descriptionHash,
            key = key,
            cryptoExtendedBaseHash = cryptoExtendedBaseHash,
            proofSeed = proofNonces[index],
            nonce = encryptionNonces[index],
        )
    }

    val numPlaceholderOnes = maxSelections - numberSelected

    val encryptedPlaceholders = (0..maxSelections - 1).map {
        PlaintextBallot.Selection(
            selectionId = "$contestId-placeholder-$it",
            sequenceOrder = numNormalSelections + it,
            vote = if (it < numPlaceholderOnes) 1 else 0,
            isPlaceholderSelection = true,
            extendedData = null
        )
    }.mapIndexed { index, plaintextSelection ->
        plaintextSelection.encrypt(
            descriptionHash = descriptionHash,
            key = key,
            cryptoExtendedBaseHash = cryptoExtendedBaseHash,
            proofSeed = proofNonces[index + numNormalSelections],
            nonce = encryptionNonces[index + numNormalSelections]
        )
    }

    val selectionNonceSum = encryptedSelections.map { it.selectionNonce }.reduce { a, b -> a + b }

    val ciphertextAccumulation = numNormalSelections.encrypt(key, selectionNonceSum)
    val constantProof = ciphertextAccumulation.constantChaumPedersenProofKnownNonce(numNormalSelections, selectionNonceSum, key, constantProofNonce, cryptoExtendedBaseHash)

    val allSelections = encryptedSelections + encryptedPlaceholders

    val cryptoHash = hashElements(contestId, allSelections, descriptionHash)

    return CiphertextBallot.Contest(
        contestId = contestId,
        sequenceOrder = sequenceOrder,
        contestHash = descriptionHash,
        selections = allSelections,
        ciphertextAccumulation = ciphertextAccumulation,
        cryptoHash = cryptoHash,
        proof = constantProof);
}

fun PlaintextBallot.encrypt(
    objectId: String,
    styleId: String,
    manifestHash: UInt256,
    contests: List<PlaintextBallot.Contest>,
    key: ElGamalPublicKey,
    cryptoExtendedBaseHash: ElementModQ,
    nonce: ElementModQ?,
    timestamp: Long? = null, // TODO: should this be a date object of some kind?
): CiphertextBallot {
    val context = compatibleContextOrFail(key.key, cryptoExtendedBaseHash, nonce)

    val ballotNonce = nonce ?: context.randomElementModQ()
    val encryptionNonces = Nonces(ballotNonce, "contest-nonces")
    val proofNonces = Nonces(ballotNonce, "proof-nonces")

    val encryptedContests = contests.mapIndexed { index, contest ->
        contest.encrypt(
            descriptionHash = manifestHash,
            key = key,
            cryptoExtendedBaseHash = cryptoExtendedBaseHash,
            proofSeed = proofNonces[index],
            nonce = encryptionNonces[index]
        )
    }

    val cryptoHash = hashElements(objectId, encryptedContests, manifestHash)

    return CiphertextBallot(
        ballotId = objectId,
        ballotStyleId = styleId,
        manifestHash = manifestHash,
        contests = encryptedContests,
        timestamp = timestamp,
        cryptoHash = cryptoHash)
}

// Notes:

// This is a vague attempt at writing up the ballot encryption functions, based in part on the
// Python code, and in part on trying to *simplify* the Python code. An essential goal here
// is to reduce the complexity, relative to the Python code. If it's not necessary, it's gone.

// TODO: revisit all the hashElements() computations. Which ones are actually necessary?
//   What should be hashed?
// TODO: what's the purpose of the extended data? Does it belong at all?
// TODO: what's the way to encode a write-in?
// TODO: what's the proper way to handle an over-vote?
// TODO: for every
// TODO: what's the right way to get a contest's maxSelections? Should that come from the
//   contest object or from the ballot definition?
// TODO: what's the codeSeed stuff for? That was added somewhere much later in the EG design.