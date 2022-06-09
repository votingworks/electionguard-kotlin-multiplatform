@file:OptIn(ExperimentalCli::class)

package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Guardian
import electionguard.core.Base16.toHex
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required

/**
 * Run KeyCeremony CLI.
 * Read election record from inputDir, write to outputDir.
 * This has access to all the trustees, so is only used for testing, or in a use case of trust.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunTrustedKeyCeremony")
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
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created"
    )
    parser.parse(args)
    println("RunTrustedKeyCeremony starting\n   input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")

    val group = productionGroup()
    runKeyCeremony(group, inputDir, outputDir, trusteeDir, createdBy)
}

fun runKeyCeremony(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    createdBy: String?
): Boolean {
    val starting = getSystemTimeInMillis()

    val consumerIn = Consumer(configDir, group)
    val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    // Generate numberOfGuardians KeyCeremonyTrustees, which means this is a trusted situation.
    val trustees: List<KeyCeremonyTrustee> = List(config.numberOfGuardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "trustee$seq", seq, config.quorum)
    }.sortedBy { it.xCoordinate }

    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        println(exchangeResult.error)
        return false
    }

    val commitments: MutableList<ElementModP> = mutableListOf()
    trustees.forEach { commitments.addAll(it.coefficientCommitments()) }
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
        trustees.map { it.electionPublicKey() }.reduce { a, b -> a * b }
    val guardians: List<Guardian> = trustees.map { makeGuardian(it) }
    val init = ElectionInitialized(
        config,
        jointPublicKey,
        config.manifest.cryptoHash,
        cryptoBaseHash,
        cryptoExtendedBaseHash,
        guardians,
        mapOf(
            Pair("CreatedBy", createdBy ?: "RunTrustedKeyCeremony"),
            Pair("CreatedOn", getSystemDate().toString()),
            Pair("CreatedFromDir", configDir))
    )
    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionInitialized(init)

    // store the trustees in some private place.
    val trusteePublisher = Publisher(trusteeDir, PublisherMode.createIfMissing)
    trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }

    val took = getSystemTimeInMillis() - starting
    println("RunTrustedKeyCeremony took $took millisecs")
    return true
}

private fun makeGuardian(trustee: KeyCeremonyTrusteeIF): Guardian {
    val publicKeys = trustee.sendPublicKeys().unwrap()
    return Guardian(
        trustee.id(),
        trustee.xCoordinate(),
        publicKeys.coefficientCommitments,
        publicKeys.coefficientProofs,
    )
}
