@file:OptIn(ExperimentalCli::class)

package electionguard.keyceremony

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.Base16.toHex
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required

/**
 * Run KeyCeremony CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunKeyCeremony")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input election record"
    ).required()
    val trusteeDir by parser.option(
        ArgType.String,
        shortName = "trustees",
        description = "Directory to write private trustees"
    ).required()
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output election record"
    ).required()
    parser.parse(args)
    println("RunKeyCeremony starting\n   input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")

    val group = productionGroup()
    runKeyCeremony(group, inputDir, outputDir, trusteeDir)
}

fun runKeyCeremony(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(configDir, group)
    val config: ElectionConfig = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    // class KeyCeremonyTrustee(
    //    val group: GroupContext,
    //    val id: String,
    //    val xCoordinate: UInt,
    //    val quorum: Int,
    val trustees: List<KeyCeremonyTrustee> = List(config.numberOfGuardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "trustee$seq", seq.toUInt(), config.quorum)
    }.sortedBy { it.xCoordinate }

    // exchange PublicKeys
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.sharePublicKeys())
        }
    }

    // exchange SecretKeyShares
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t2.receiveSecretKeyShare(t1.sendSecretKeyShare(t2.id))
        }
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    trustees.forEach { commitments.addAll(it.polynomial.coefficientCommitments) }
    val commitmentsHash = hashElements(commitments)

    val primes = config.constants
    val cryptoBaseHash: UInt256 = hashElements(
        primes.largePrime.toHex(),
        primes.smallPrime.toHex(),
        primes.generator.toHex(),
        config.numberOfGuardians,
        config.quorum,
        config.manifest.cryptoHash,
    )

    val cryptoExtendedBaseHash: UInt256 = hashElements(cryptoBaseHash, commitmentsHash)
    val jointPublicKey: ElementModP =
        trustees.map { it.polynomial.coefficientCommitments[0] }.reduce { a, b -> a * b }
    val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
    val init = ElectionInitialized(
        config,
        jointPublicKey,
        config.manifest.cryptoHash,
        cryptoBaseHash,
        cryptoExtendedBaseHash,
        guardians,
    )
    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionInitialized(init)

    val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    val took = getSystemTimeInMillis() - starting
    println("RunKeyCeremony took $took millisecs")
}

private fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
    val publicKeys = trustee.sharePublicKeys()
    return Guardian(
        "guardian${trustee.xCoordinate}",
        trustee.xCoordinate,
        publicKeys.coefficientCommitments,
        publicKeys.coefficientProofs,
    )
}
