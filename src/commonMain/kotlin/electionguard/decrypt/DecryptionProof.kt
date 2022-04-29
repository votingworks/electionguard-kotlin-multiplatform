package electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.GenericChaumPedersenProof

/** Direct decryption from the Decrypting Trustee */
data class PartialDecryptionAndProof(
    val partialDecryption: ElementModP,
    val proof: GenericChaumPedersenProof)

/** Compensated decryption from the Decrypting Trustee */
data class CompensatedPartialDecryptionAndProof(
    val partialDecryption: ElementModP,
    val proof: GenericChaumPedersenProof?,
    val recoveredPublicKey: ElementModP)