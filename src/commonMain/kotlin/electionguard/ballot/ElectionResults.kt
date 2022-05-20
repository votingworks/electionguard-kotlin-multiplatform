package electionguard.ballot

import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.toElementModQ

data class TallyResult(
    val group: GroupContext,
    val electionIntialized: ElectionInitialized,
    val ciphertextTally: CiphertextTally,
    val ballotIds: List<String>,
    val tallyIds: List<String>,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun jointPublicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(electionIntialized.jointPublicKey)
    }
    fun cryptoExtendedBaseHash(): ElementModQ {
        return electionIntialized.cryptoExtendedBaseHash.toElementModQ(group)
    }
    fun numberOfGuardians(): Int {
        return electionIntialized.config.numberOfGuardians
    }
    fun quorum(): Int {
        return electionIntialized.config.quorum
    }
}

data class DecryptionResult(
    val tallyResult: TallyResult,
    val decryptedTally: PlaintextTally,
    val availableGuardians: List<DecryptingGuardian>,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun numberOfGuardians(): Int {
        return tallyResult.numberOfGuardians()
    }
    fun quorum(): Int {
        return tallyResult.quorum()
    }
}

data class DecryptingGuardian(
    var guardianId: String,
    var xCoordinate: Int,
    var lagrangeCoordinate: ElementModQ,
)