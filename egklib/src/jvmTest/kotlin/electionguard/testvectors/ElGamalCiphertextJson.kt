package electionguard.testvectors

import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import kotlinx.serialization.Serializable


@Serializable
data class ElGamalCiphertextJson(
    val pad: ElementModPJson,
    val data: ElementModPJson
)

fun ElGamalCiphertext.publishJson() = ElGamalCiphertextJson(this.pad.publishJson(), this.data.publishJson())
fun ElGamalCiphertextJson.import(group: GroupContext) = ElGamalCiphertext(this.pad.import(group), this.data.import(group))