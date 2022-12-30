package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.SchnorrProof
import electionguard.core.merge

data class PublicKeys(
    val guardianId: String,
    val guardianXCoordinate: Int,
    val coefficientProofs: List<SchnorrProof>,
) {
    init {
        require(guardianId.isNotEmpty())
        require(guardianXCoordinate > 0)
        require(coefficientProofs.isNotEmpty())
    }

    fun publicKey(): ElGamalPublicKey {
        return ElGamalPublicKey(coefficientProofs[0].publicKey)
    }

    fun coefficientCommitments(): List<ElementModP> {
        return coefficientProofs.map { it.publicKey }
    }

    fun validate(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for ((idx, proof) in this.coefficientProofs.withIndex()) {
            val result = proof.validate()
            if (result is Err) {
                checkProofs.add(
                    Err("  Guardian $guardianId has invalid proof for coefficient $idx " +
                                result.unwrapError()
                    )
                )
            }
        }
        return checkProofs.merge()
    }
}