# 🗳 Pre-encryption Workflow

draft 7/17/2023

## 1. "The Ballot Encrypting Tool": generating Preencrypted ballots

 Prencrypt(manifest, sigma).preencrypt(ballotId, ballotStyle, primaryNonce, codeBaux) -> PreEncryptedBallot

````
data class PreEncryptedBallot(
    val ballotId: String,
    val ballotStyleId: String,
    val primaryNonce: UInt256,
    val contests: List<PreEncryptedContest>,
    val confirmationCode: UInt256, // eq 96
)

data class PreEncryptedContest(
    val contestId: String, // could just pass the manifest contest, in case other info is needed
    val sequenceOrder: Int,
    val votesAllowed: Int,
    val selections: List<PreEncryptedSelection>, // nselections + limit, in sequenceOrder, eq 93,94
    val preencryptionHash: UInt256, // eq 95
)

data class PreEncryptedSelection(
    val selectionId: String, // could just pass the manifest selection, in case other info is needed
    val sequenceOrder: Int,  // matches the Manifest
    val selectionHash: ElementModQ, // allow numerical sorting with ElementModQ, eq 93
    val shortCode: String, // first 5 digits of the hex representation of the selectionHash.
    val selectionVector: List<ElGamalCiphertext>, // nselections, in sequenceOrder, eq 92
    val selectionNonces: List<ElementModQ>, // nselections, in sequenceOrder (debugging)
)
````

In the following test vector, you are given the manifest, primaryNonce, etc. The short code (sigma function) is the 
first 5 digits of the hex representation of the selectionHash. From all those you can generate the expected fields in the
preencrypted_ballot:

[Example PreEncryptedBallot JSON serialization](../egklib/src/commonTest/data/testvectors/PreEncryptionTestVector.json)

## 2. Print ballot and vote

Then a printed ballot is made: for each contest: for each selection: show the selection label and short code.
Allow user to mark the ballot, return representation of the voter's choices.

## 3. "The Ballot Recording Tool": record Pre-encrypted ballot in the election record

Recorder(manifest, sigma).record(ballotId, ballotStyle, primaryNonce, codeBaux, markedBallot) -> EncryptedBallot

1. The recorder regenerates the PreEncryptedBallot from the primaryNonce and ballotStyleId.
2. With the regenerated PreEncryptedBallot and the voter's choices, generate an encrypted_ballot for the election record,
   which has the usual fields, plus for each contest, the PreEncryption data.
3. With these, a verifier can verify the Pre-encryption (spec 2.0, section 4.5).

````
data class EncryptedBallot(
    override val ballotId: String,
    val ballotStyleId: String,  // matches a Manifest.BallotStyle
    val encryptingDevice: String,
    val timestamp: Long,
    val codeBaux: ByteArray, // Baux in eq 59
    val confirmationCode: UInt256, // tracking code, H(B), eq 59
    override val contests: List<Contest>,
    override val state: BallotState,
    val isPreencrypt: Boolean = false,
)

    data class Contest(
        override val contestId: String, // matches ContestDescription.contestIdd
        override val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        val votesAllowed: Int, // matches ContestDescription.votesAllowed
        val contestHash: UInt256, // eq 58
        override val selections: List<Selection>,
        val proof: ChaumPedersenRangeProofKnownNonce,
        val contestData: HashedElGamalCiphertext,
        val preEncryption: PreEncryption? = null, // pre-encrypted ballots only
    ) 

    data class Selection(
        override val selectionId: String, // matches SelectionDescription.selectionId
        override val sequenceOrder: Int, // matches SelectionDescription.sequenceOrder
        override val encryptedVote: ElGamalCiphertext,
        val proof: ChaumPedersenRangeProofKnownNonce,
    ) 

    data class PreEncryption(
        val preencryptionHash: UInt256, // (eq 95)
        // the selection hashes for every option on the ballot
        val allSelectionHashes: List<UInt256>, // size = nselections + limit, sorted numerically
        // the short codes and selection vectors for all selections on the made by the voter.
        val selectedVectors: List<SelectionVector>, // size = limit, sorted numerically
    ) 

    // Ψi,m = ⟨E1 , E2 , . . . , Em ⟩  (eq 92)
    data class SelectionVector(
        val selectionHash: UInt256, // ψi (eq 93)
        val shortCode: String,
        val encryptions: List<ElGamalCiphertext>, // Ej, size = nselections, in order by sequence_order
    )
}
````

[Example EncryptedBallot JSON serialization](../egklib/src/commonTest/data/testvectors/PreEncryptionRecordedTestVector.json)
