@file:OptIn(ExperimentalCli::class)

package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.Base16.toHex
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.UInt256
import electionguard.core.elGamalKeyPairFromRandom
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.schnorrProof
import electionguard.decrypt.DecryptingTrustee
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ExperimentalCli
import kotlin.test.Test

/** Run a fake KeyCeremony to generate an ElectionInitialized for workflow testing. */

@Test
fun runFakeKeyCeremonyTest() {
    val group = productionGroup()
    val configDir = "src/commonTest/data/runWorkflow"
    val outputDir = "testOut/runFakeKeyCeremonyTest"
    val trusteeDir = "testOut/runFakeKeyCeremonyTest/private_data"

    runFakeKeyCeremony(group, configDir, outputDir, trusteeDir)
}

fun runFakeKeyCeremony(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
): ElectionInitialized {
    val electionRecordIn = ElectionRecord(configDir, group)
    val config: ElectionConfig = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    // The hashing is order dependent, use the x coordinate to sort.
    val polynomials: List<ElectionPolynomial> = List(config.numberOfGuardians) {
        generatePolynomial(it, group, config.quorum)
    }.sortedBy { it.guardianXCoordinate }

    val guardians: List<Guardian> = polynomials.map { electionPolynomial ->
        makeGuardian(electionPolynomial)
    }
    val trustees: List<DecryptingTrustee> = polynomials.map { electionPolynomial ->
        makeTrustee(electionPolynomial)
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    polynomials.forEach { commitments.addAll( it.coefficientCommitments )}
    val commitmentsHash = hashElements(commitments)

    val primes = config.constants
    val crypto_base_hash: UInt256 = hashElements(
        primes.largePrime.toHex(), // LOOK is this the same as converting to ElementMod ??
        primes.smallPrime.toHex(),
        primes.generator.toHex(),
        config.numberOfGuardians,
        config.quorum,
        config.manifest.cryptoHash,
    )

    val cryptoExtendedBaseHash: UInt256 = hashElements(crypto_base_hash, commitmentsHash)
    val jointPublicKey: ElementModP = guardians.map { it.publicKey() }.reduce { a, b -> a * b }

    val init = ElectionInitialized(
        config,
        jointPublicKey,
        config.manifest.cryptoHash,
        cryptoExtendedBaseHash,
        guardians,
    )

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionInitialized(init)

    val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    return init
}

// must generate the public guardians and the private trustees
private fun makeGuardian(poly: ElectionPolynomial): Guardian {
    return Guardian(
        "guardian${poly.guardianXCoordinate}",
        poly.guardianXCoordinate,
        poly.coefficientCommitments,
        poly.coefficientProofs,
    )
}

private fun makeTrustee(poly: ElectionPolynomial): DecryptingTrustee {
    return DecryptingTrustee(
        "guardian${poly.guardianXCoordinate}",
        poly.guardianXCoordinate,
        ElGamalKeypair(
            ElGamalSecretKey(poly.coefficients[0]),
            ElGamalPublicKey(poly.coefficientCommitments[0])
        )
    )
}

/**
 * The polynomial that each Guardian defines to solve for their private key.
 * A different point associated with the polynomial is shared with each of the other guardians so that the guardians
 * can come together to derive the polynomial function and solve for the private key.
 * <p>
 * The 0-index coefficient is used for a secret key which can be discovered by a quorum of guardians.
 */
private data class ElectionPolynomial(
    val guardianXCoordinate: Int,

    /** The secret coefficients `a_ij`.  */
    val coefficients: List<ElementModQ>,

    /** The public keys `K_ij` generated from secret coefficients. (not secret)  */
    val coefficientCommitments: List<ElementModP>,

    /** A proof of possession of the private key for each secret coefficient. (not secret)  */
    val coefficientProofs: List<SchnorrProof>,
)

private fun generatePolynomial(
    seq: Int,
    context: GroupContext,
    quorum: Int,
): ElectionPolynomial {
    val coefficients = mutableListOf<ElementModQ>()
    val commitments = mutableListOf<ElementModP>()
    val proofs = mutableListOf<SchnorrProof>()

    for (coeff in 1..quorum) {
        val keypair: ElGamalKeypair = elGamalKeyPairFromRandom(context)
        coefficients.add(keypair.secretKey.key)
        commitments.add(keypair.publicKey.key)
        proofs.add(keypair.schnorrProof())
    }
    return ElectionPolynomial(seq + 1, coefficients, commitments, proofs)
}

