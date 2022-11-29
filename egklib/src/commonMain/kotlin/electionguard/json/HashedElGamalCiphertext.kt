package electionguard.json

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("HashedElGamalCiphertext")
data class HashedElGamalCiphertextJson(
    val pad: ElementModPJson,
    val data: ByteArray,
    val mac: UInt256Json,
    val numBytes: Int
)

fun HashedElGamalCiphertext.publish() = HashedElGamalCiphertextJson(
        this.c0.publish(),
        this.c1,
        this.c2.publish(),
        this.numBytes,
    )

fun HashedElGamalCiphertextJson.import(group: GroupContext): HashedElGamalCiphertext? {
    val mac = this.mac.import()

    return if (mac == null) null else
        HashedElGamalCiphertext(
            this.pad.import(group)!!,
            this.data,
            mac,
            this.numBytes,
        )
}