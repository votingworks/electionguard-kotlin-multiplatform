package electionguard.json2

import electionguard.ballot.EncryptedBallotChain
import electionguard.core.Base16.fromSafeHex
import electionguard.core.Base16.toHex
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedBallotChainJson(
    val encrypting_device: String,
    val baux0: String,
    val ballot_ids: List<String>,
    val confirmation_codes: List<UInt256Json>,
    val chaining: Boolean,
    val closing_hash: UInt256Json?,
    val metadata: Map<String, String> = emptyMap(),
)

fun EncryptedBallotChain.publishJson() = EncryptedBallotChainJson(
    this.encryptingDevice,
    this.baux0.toHex(),
    this.ballotIds,
    this.confirmationCodes.map { it.publishJson() },
    this.chaining,
    this.closingHash?.publishJson(),
    this.metadata,
)

fun EncryptedBallotChainJson.import() = EncryptedBallotChain(
    this.encrypting_device,
    this.baux0.fromSafeHex(),
    this.ballot_ids,
    this.confirmation_codes.map { it.import() },
    this.chaining,
    this.closing_hash?.import(),
    this.metadata,
)