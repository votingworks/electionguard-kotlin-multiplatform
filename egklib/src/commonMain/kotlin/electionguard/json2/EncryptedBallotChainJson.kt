package electionguard.json2

import electionguard.ballot.EncryptedBallotChain
import electionguard.core.Base16.fromHexSafe
import electionguard.core.Base16.toHex
import electionguard.core.UInt256
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class EncryptedBallotChainJson(
    val encrypting_device: String,
    val baux0: String,
    val ballot_ids: List<String>,
    val last_confirmation_code: UInt256Json,
    val chaining: Boolean,
    val closing_hash: UInt256Json?,
    val metadata: Map<String, String> = emptyMap(),
)

fun EncryptedBallotChain.publishJson() = EncryptedBallotChainJson(
    this.encryptingDevice,
    this.baux0.toHex(),
    this.ballotIds,
    this.lastConfirmationCode.publishJson(),
    this.chaining,
    this.closingHash?.publishJson(),
    this.metadata,
)

fun EncryptedBallotChainJson.import(errs : ErrorMessages): EncryptedBallotChain? {
    val last = last_confirmation_code.import() ?: errs.addNull("malformed last_confirmation_code") as UInt256?
    val closing_hash = if (closing_hash == null) null else closing_hash.import() ?: errs.addNull("malformed closing_hash") as UInt256?

    return if (errs.hasErrors()) null
    else EncryptedBallotChain(
        this.encrypting_device,
        this.baux0.fromHexSafe(),
        this.ballot_ids,
        last!!,
        this.chaining,
        closing_hash,
        this.metadata,
    )
}