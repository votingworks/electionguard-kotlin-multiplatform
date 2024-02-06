package electionguard.cli

import electionguard.core.*
import electionguard.encrypt.Encryptor
import electionguard.input.RandomBallotProvider
import electionguard.util.ErrorMessages
import electionguard.util.Stopwatch
import electionguard.util.sigfig
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

class RunEncryptBallotTiming {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunEncryptBallotTiming")
            val nballots by parser.option(
                ArgType.Int,
                shortName = "nballots",
                description = "Number of ballots to encrypt and measure time"
            ).default(100)
            val ncontests by parser.option(
                ArgType.Int,
                shortName = "ncontests",
                description = "Number of contests per ballot"
            ).default(12)
            val nselections by parser.option(
                ArgType.Int,
                shortName = "nselections",
                description = "Number of selections per contest"
            ).default(4)
            val warmup by parser.option(
                ArgType.Int,
                shortName = "warmup",
                description = "Number of ballots to encrypt as warmup (not timed)"
            ).default(11)
            val showOperations by parser.option(
                ArgType.Boolean,
                shortName = "ops",
                description = "Show operation count"
            ).default(false)
            parser.parse(args)

            println(
                "RunEncryptBallotTiming\n" +
                        "  nballots = '$nballots'\n" +
                        "  ncontests = '$ncontests'\n" +
                        "  nselections = '$nselections'\n" +
                        "  warmup = '$warmup'\n"
            )

            val group = productionGroup()
            val manifest =  buildTestManifest(ncontests, nselections)
            val keypair = elGamalKeyPairFromRandom(group)
            val encryptor = Encryptor(group, manifest, keypair.publicKey, UInt256.random(), "device")

            // warmup
            println("warming up with $warmup ballots")
            val warmupProvider = RandomBallotProvider(manifest, warmup)
            warmupProvider.ballots().forEach { ballot ->
                val encryptedBallot = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryption"))
                requireNotNull(encryptedBallot)
            }

            // time it
            val ballotProvider = RandomBallotProvider(manifest, nballots)
            var stopwatch = Stopwatch()
            group.getAndClearOpCounts()
            ballotProvider.ballots().forEach { ballot ->
                val encryptedBallot = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryption"))
                requireNotNull(encryptedBallot)
            }
            val opCounts = group.getAndClearOpCounts()
            var duration = stopwatch.stop()

            val perballot = duration.toDouble() / nballots / 1_000_000
            val nencryptions = ncontests + ncontests * nselections
            val perencryption = perballot / nencryptions
            println("Encryption took ${duration / 1_000_000_000 } secs for $nballots ballots")
            println("   ${perballot.sigfig(3)} msec per ballot")
            println("   ${perencryption.sigfig(3)} msec per encryption ($nencryptions encryptions/ballot)")
            println()
            if (showOperations) {
                println("operations:")
                println(buildString {
                    opCounts.forEach { key, value -> println("  $key = $value") }
                })
                println("expect: ${6 * nencryptions * nballots + 2 * nballots}")
            }
        }
    }
}