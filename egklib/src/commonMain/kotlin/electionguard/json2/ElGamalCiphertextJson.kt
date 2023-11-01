package electionguard.json2

import electionguard.core.ElGamalCiphertext
import electionguard.core.GroupContext
import kotlinx.serialization.Serializable


@Serializable
data class ElGamalCiphertextJson(
    val pad: ElementModPJson,
    val data: ElementModPJson
)

fun ElGamalCiphertext.publishJson() = ElGamalCiphertextJson(this.pad.publishJson(), this.data.publishJson())

fun ElGamalCiphertextJson.import(group: GroupContext) : ElGamalCiphertext? {
    val pad = this.pad.import(group)
    val data = this.data.import(group)
    return if (pad == null || data == null) null else ElGamalCiphertext(pad, data)
}