package electionguard.ballot

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.UInt256

// one for each encryption device
data class EncryptedBallotChain(
    val encryptingDevice: String,
    val baux0: ByteArray,
    val ballotIds: List<String>,
    val lastConfirmationCode: UInt256,
    val chaining: Boolean,
    val closingHash: UInt256?, // only if chaining == true
    val metadata: Map<String, String> = emptyMap(),
)

/** Results of tallying some collection of ballots, namely an EncryptedTally. */
data class TallyResult(
    val electionInitialized: ElectionInitialized,
    val encryptedTally: EncryptedTally,
    val tallyIds: List<String>,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun jointPublicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(electionInitialized.jointPublicKey)
    }
    fun extendedBaseHash(): UInt256 {
        return electionInitialized.extendedBaseHash
    }
    fun numberOfGuardians(): Int {
        return electionInitialized.config.numberOfGuardians
    }
    fun quorum(): Int {
        return electionInitialized.config.quorum
    }
}

/** Results of decrypting an EncryptedTally. */
data class DecryptionResult(
    val tallyResult: TallyResult,
    val decryptedTally: DecryptedTallyOrBallot,
    val metadata: Map<String, String> = emptyMap(),
)

data class LagrangeCoordinate(
    var guardianId: String,
    var xCoordinate: Int,
    var lagrangeCoefficient: ElementModQ, // wℓ, spec 2.0.0 eq 67
) {
    init {
        require(guardianId.isNotEmpty())
        require(xCoordinate > 0)
    }
}