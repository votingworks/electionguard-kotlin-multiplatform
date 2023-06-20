package electionguard.json2

import electionguard.core.Base16.fromHex
import electionguard.core.Base16.toHex
import electionguard.core.GroupContext
import electionguard.core.HashedElGamalCiphertext
import kotlinx.serialization.Serializable

@Serializable
data class HashedElGamalCiphertextJson(
    val c0: ElementModPJson, // ElementModP,
    val c1: String, // ByteArray,
    val c2: UInt256Json, // UInt256,
    val numBytes: Int // TODO needed?
)

fun HashedElGamalCiphertext.publishJson() =
    HashedElGamalCiphertextJson(this.c0.publishJson(), this.c1.toHex(), this.c2.publishJson(), this.numBytes)

fun HashedElGamalCiphertextJson.import(group : GroupContext) =
    HashedElGamalCiphertext(
        c0.import(group),
        c1.fromHex()!!,
        c2.import(),
        numBytes,
    )