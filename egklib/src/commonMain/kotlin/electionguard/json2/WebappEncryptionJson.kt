package electionguard.json2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// stuff used by the server webapp

@Serializable
@SerialName("EncryptionResponse")
data class EncryptionResponseJson(
    val confirmationCode : String
)