package electionguard.publish

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.*
import electionguard.core.Base16.fromSafeHex
import electionguard.core.UInt256
import electionguard.core.hashElements
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.core.toUInt256
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test Manifest hash values against the ones that python and java generate. */
class ManifestHashValidateTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable/"

    @Test
    fun readElectionRecord() {
        runTest {
            val context = productionGroup()
            val consumerIn = makeConsumer(input, context)
            val init = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            validateManifestHash(init.config)
        }
    }

    fun showManifestHash(manifest: Manifest) {
        println()
        println("***manifest.electionScopeId '${manifest.electionScopeId}' ")
        println("***manifest.electionType '${manifest.electionType}' ")
        println("***manifest.startDate '${manifest.startDate}' ")
        println("***manifest.endDate '${manifest.endDate}' ")
        println("***manifest.name '${manifest.name}'")
        println("***manifest.contactInformation '${manifest.contactInformation}'")

        println("***manifest.geopoliticalUnits")
        manifest.geopoliticalUnits.forEach {
            println("  '$it'")
            println()
        }

        println("***manifest.parties")
        manifest.parties.forEach {
            println("  '$it'")
            println()
        }

        println("***manifest.ballotStyles")
        manifest.ballotStyles.forEach {
            val cryptoHash: UInt256 = hashElements(it.ballotStyleId, it.geopoliticalUnitIds, it.partyIds, it.imageUri)
            println("  '$it' = $cryptoHash")
            println()
        }

        println("***manifest.candidates")
        manifest.candidates.forEach {
            val cryptoHash: UInt256 = hashElements(it.candidateId, it.name, it.partyId, it.imageUri)
            println("  '$it' = $cryptoHash")
            println()
        }

        for (contest in manifest.contests) {
            println("  contest ${contest.contestId} = ${contest.contestHash}")
            for (selection in contest.selections) {
                println("    selection ${selection.selectionId} = ${selection.selectionHash}")
            }
        }
        println()
    }

    fun validateManifestHash(config : ElectionConfig) {
        val Hp =  parameterBaseHash(config.constants)
        assertEquals(Hp, config.parameterBaseHash)
        val Hm = manifestHash(Hp, config.manifestFile)
        assertEquals(Hm, config.manifestHash)
        assertEquals(electionBaseHash(Hp, config.numberOfGuardians, config.quorum, config.electionDate, config.jurisdictionInfo, Hm), config.electionBaseHash)

        for (contest in config.manifest.contests) {
            println("  contest ${contest.contestId} crypto_hash: ${contest.contestHash}")
            for (selection in contest.selections) {
                val scryptoHash = selectionDescriptionCryptoHash(
                    selection.selectionId,
                    selection.sequenceOrder,
                    selection.candidateId,
                )
                // println("    Selection ${selection.selectionId} crypto_hash: ${selection.cryptoHash}")
                // println("    Expected  crypto_hash: $scryptoHash")
                assertEquals(selection.selectionHash, scryptoHash)
            }
            val ccryptoHash = contestDescriptionCryptoHash(
                contest.contestId,
                contest.sequenceOrder,
                contest.geopoliticalUnitId,
                contest.voteVariation,
                contest.numberElected,
                contest.votesAllowed,
                contest.name,
                contest.selections,
                contest.ballotTitle,
                contest.ballotSubtitle,
                // contest.primaryPartyIds,
            )
            // println("  Contest ${contest.contestId} crypto_hash: ${contest.cryptoHash}")
            // println("  Expected crypto_hash: $ccryptoHash")
            assertEquals(contest.contestHash, ccryptoHash)
        }
    }
}