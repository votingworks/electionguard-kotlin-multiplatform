package electionguard.json

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an SecretKeyShare */
@Serializable
@SerialName("HashedElGamalCiphertext")
data class HashedElGamalCiphertextJson(
    val pad: ElementModPJson,
    val data: ByteArray,
    val mac: UInt256Json,
    val numBytes: Int
)

/** Publishes a [HashedElGamalCiphertext] to its external, serializable form. */
fun HashedElGamalCiphertext.publish() = HashedElGamalCiphertextJson(
    this.c0.publishModP(),
    this.c1,
    this.c2.publish(),
    this.numBytes,
)

/** Imports from a published [HashedElGamalCiphertext]. Returns `null` if it's malformed. */
fun GroupContext.importHashedElGamalCiphertext(pub: HashedElGamalCiphertextJson): HashedElGamalCiphertext? {
    return HashedElGamalCiphertext(
        this.importModP(pub.pad)!!,
        pub.data,
        pub.mac.import(),
        pub.numBytes,
    )
}