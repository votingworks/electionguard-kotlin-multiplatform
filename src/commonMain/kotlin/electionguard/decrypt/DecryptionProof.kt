package electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.GenericChaumPedersenProof

data class DecryptionProofRecovery(
    val decryption: ElementModP,
    val proof: GenericChaumPedersenProof,
    val recoveryPublicKey: ElementModP)

data class PartialDecryptionProof(
    val partialDecryption: ElementModP,
    val proof: GenericChaumPedersenProof)