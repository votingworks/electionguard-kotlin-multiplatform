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
) {
    fun jointPublicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(electionIntialized.jointPublicKey)
    }
    fun cryptoExtendedBaseHash(): ElementModQ {
        return electionIntialized.cryptoExtendedBaseHash.toElementModQ(group)
    }
    fun quorum(): Int {
        return electionIntialized.config.quorum
    }
    fun numberOfGuardians(): Int {
        return electionIntialized.config.numberOfGuardians
    }
}

data class DecryptionResult(
    val tallyResult: TallyResult,
    val decryptedTally: PlaintextTally,
    val availableGuardians: List<AvailableGuardian>,
    val metadata: Map<String, String> = emptyMap(),
)

data class AvailableGuardian(
    var guardianId: String,
    var xCoordinate: Int,
    var lagrangeCoordinate: ElementModQ,
)