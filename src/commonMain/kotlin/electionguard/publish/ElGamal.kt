package electionguard.publish

import electionguard.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** External representation of an ElGamal secret key */
@Serializable
@SerialName("ElGamalSecretKey")
data class ElGamalSecretKeyPub(val key: ElementModQPub)

/** External representation of an ElGamal public key */
@Serializable
@SerialName("ElGamalPublicKey")
data class ElGamalPublicKeyPub(val key: ElementModPPub)

/** External representation of an ElGamal keypair */
@Serializable
@SerialName("ElGamalKeypair")
data class ElGamalKeypairPub(val secretKey: ElGamalSecretKeyPub, val publicKey: ElGamalPublicKeyPub)

/** External representation of an ElGamal ciphertext */
@Serializable
@SerialName("ElGamalCiphertext")
data class ElGamalCiphertextPub(val pad: ElementModPPub, val data: ElementModPPub)

/**
 * Publishes an [ElGamalPublicKey] to its external, serializable form. The method name is
 * `publishKey` rather than `publish`, to distinguish this from the extension methods that on
 * `ElementModP` (which is a type alias for [ElGamalPublicKey]).
 */
fun ElGamalPublicKey.publishKey(): ElGamalPublicKeyPub = ElGamalPublicKeyPub(this.publish())

/** Publishes an [ElGamalSecretKey] to its external, serializable form. */
fun ElGamalSecretKey.publishKey(): ElGamalSecretKeyPub = ElGamalSecretKeyPub(this.e.publish())

/** Publishes an [ElGamalKeypair] to its external, serializable form. */
fun ElGamalKeypair.publish(): ElGamalKeypairPub =
    ElGamalKeypairPub(secretKey.publishKey(), publicKey.publishKey())

/** Publishes an [ElGamalCiphertext] to its external, serializable form. */
fun ElGamalCiphertext.publish(): ElGamalCiphertextPub =
    ElGamalCiphertextPub(pad.publish(), data.publish())

/** Imports from a published [ElGamalPublicKey]. Returns `null` if it's malformed. */
fun GroupContext.importKey(element: ElGamalPublicKeyPub): ElGamalPublicKey? =
    this.import(element.key)

/** Imports from a published [ElGamalSecretKey]. Returns `null` if it's malformed. */
fun GroupContext.importKey(element: ElGamalSecretKeyPub): ElGamalSecretKey? =
    this.import(element.key)?.let { ElGamalSecretKey(it) }

/** Imports from a published [ElGamalKeypair]. Returns `null` if it's malformed. */
fun GroupContext.import(keypair: ElGamalKeypairPub): ElGamalKeypair? {
    val secretKey = importKey(keypair.secretKey)
    val publicKey = importKey(keypair.publicKey)
    if (secretKey == null || publicKey == null) return null
    return ElGamalKeypair(secretKey, publicKey)
}

/** Imports from a published [ElGamalCiphertext]. Returns `null` if it's malformed. */
fun GroupContext.import(ciphertext: ElGamalCiphertextPub): ElGamalCiphertext? {
    val pad = import(ciphertext.pad)
    val data = import(ciphertext.data)
    if (pad == null || data == null) return null
    return ElGamalCiphertext(pad, data)
}
