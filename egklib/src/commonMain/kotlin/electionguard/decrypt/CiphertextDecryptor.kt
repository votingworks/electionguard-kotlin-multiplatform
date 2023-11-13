package electionguard.decrypt

import com.github.michaelbull.result.*
import electionguard.core.*
import electionguard.publish.Consumer
import electionguard.publish.makeConsumer
import electionguard.publish.makeTrusteeSource
import electionguard.util.ErrorMessages
import electionguard.util.mergeErrorMessages

// uses ElectionInitialized.guardians to read DecryptingTrustee's from trusteeDir
class CiphertextDecryptor(
    group: GroupContext,
    inputDir: String,
    trusteeDir: String,
    missing: String? = null
) {
    val trustees : List<DecryptingTrusteeIF>
    val lagrangeCoeff : List<ElementModQ>
    val secretKey : ElementModQ
    val keyPair : ElGamalKeypair

    init {
        val consumerIn = makeConsumer(group, inputDir)
        val initResult = consumerIn.readElectionInitialized()
        if (initResult is Err) {
            throw RuntimeException(initResult.error.toString())
        }
        val init = initResult.unwrap()
        val trusteeSource: Consumer = makeTrusteeSource(trusteeDir, group, consumerIn.isJson())
        val readTrusteeResults: List<Result<DecryptingTrusteeIF, ErrorMessages>> =
            init.guardians.map { trusteeSource.readTrustee(trusteeDir, it.guardianId) }
        val (allTrustees, allErrors) = readTrusteeResults.partition()
        if (allErrors.isNotEmpty()) {
            throw RuntimeException(mergeErrorMessages("readDecryptingTrustees", allErrors).toString())
        }
        trustees = if (missing.isNullOrEmpty()) {
            allTrustees
        } else {
            // remove missing guardians
            val missingX = missing.split(",").map { it.toInt() }
            allTrustees.filter { !missingX.contains(it.xCoordinate()) }
        }

        // build the lagrangeCoordinates once and for all
        val coeffs = mutableListOf<ElementModQ>()
        for (trustee in trustees) {
            // available trustees minus me
            val present: List<Int> = trustees.filter { it.id() != trustee.id() }.map { it.xCoordinate() }
            coeffs.add( group.computeLagrangeCoefficient(trustee.xCoordinate(), present))
        }
        this.lagrangeCoeff = coeffs

        // The decryption M = A^s mod p can be computed as shown in Equation (68) because
        // s = Sum( wi * P(i)) mod q,  i∈U
        secretKey = with (group) {
            trustees.mapIndexed { idx, it -> (it as DecryptingTrusteeDoerre).keyShare * lagrangeCoeff[idx] }.addQ()
        }
        require(init.jointPublicKey == group.gPowP(secretKey))
        keyPair = ElGamalKeypair(ElGamalSecretKey(secretKey), ElGamalPublicKey(init.jointPublicKey))
    }

    fun decrypt(ciphertext : ElGamalCiphertext) : Int? {
        return ciphertext.decrypt(keyPair)
    }
}
