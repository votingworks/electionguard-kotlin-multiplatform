package electionguard.keyceremony

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext

class KeyCeremonyTrustee(
    val group: GroupContext,
    val id: String,
    val xCoordinate: UInt,
    val quorum: Int,
) {
    val polynomial: ElectionPolynomial

    // All of the guardians' public keys (including this one), keyed by guardian id.
    val guardianPublicKeys: MutableMap<String, PublicKeys> = mutableMapOf()

    // This guardian's share of other guardians' secret keys, keyed by designated guardian id.
    val mySecretKeyShares: MutableMap<String, SecretKeyShare> = mutableMapOf()

    // Other guardians' shares of this guardian's secret key, keyed by generating guardian id.
    val guardianSecretKeyShares: MutableMap<String, SecretKeyShare> = mutableMapOf()

    init {
        polynomial = group.generatePolynomial(id, quorum)

        // allGuardianPublicKeys include itself.
        guardianPublicKeys[id] = this.sharePublicKeys()
    }

    fun electionPublicKey(): ElementModP = polynomial.coefficientCommitments[0]

    fun sharePublicKeys(): PublicKeys {
        return PublicKeys(
            id,
            xCoordinate,
            polynomial.coefficientCommitments,
            polynomial.coefficientProofs,
        )
    }

    /** Receive publicKeys from another guardian. Return error message or empty string on success.  */
    // LOOK how do we know it came from the real guardian?
    fun receivePublicKeys(publicKeys: PublicKeys): String {
        if (publicKeys.guardianXCoordinate < 1U) {
            return "${publicKeys.guardianId}: guardianXCoordinate must be >= 1"
        }
        if (publicKeys.coefficientCommitments.size != quorum) {
            return "${publicKeys.guardianId}: must have quorum ($quorum) coefficientCommitments"
        }
        if (publicKeys.coefficientProofs.size != quorum) {
            return "${publicKeys.guardianId}: must have quorum ($quorum) coefficientProofs"
        }
        guardianPublicKeys[publicKeys.guardianId] = publicKeys
        return ""
    }

    // LOOK this needs to be encrypted, see p10 spec 1.0
    fun sendSecretKeyShare(otherGuardian: String): SecretKeyShare {
        if (mySecretKeyShares.containsKey(otherGuardian)) {
            return mySecretKeyShares[otherGuardian]!!
        }
        val backup = generateSecretKeyShare(otherGuardian)
        if (backup.errors.isEmpty()) {
            mySecretKeyShares[otherGuardian] = backup
        }
        return backup
    }

    private fun generateSecretKeyShare(otherGuardian: String): SecretKeyShare {
        val other = guardianPublicKeys[otherGuardian]
            ?: return SecretKeyShare(
                id,
                otherGuardian,
                0U,
                group.ZERO_MOD_Q,
                "Trustee '$id', does not have public key for '$otherGuardian'",
            )

        // Compute my polynomial's y value at the other's x coordinate.
        val value: ElementModQ = polynomial.valueAt(group, other.guardianXCoordinate)
        return SecretKeyShare(
            id,
            otherGuardian,
            other.guardianXCoordinate,
            value,
        )
    }

    fun receiveSecretKeyShare(backup: SecretKeyShare): String {
        guardianSecretKeyShares[backup.generatingGuardianId] = backup
        return ""
    }

}