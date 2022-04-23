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
    val input = "src/commonTest/data/workflow/"

    @Test
    fun readElectionRecordWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            val electionRecordIn = ElectionRecord(input, context)
            val electionConfig = electionRecordIn.readElectionConfig().getOrThrow { IllegalStateException(input) }
            validateManifestHash(electionConfig.manifest)
        }
    }

    fun showManifestHash(manifest: Manifest) {
        println()
        println("***manifest.electionScopeId '${manifest.electionScopeId}' ")
        println("***manifest.electionType '${manifest.electionType}' ")
        println("***manifest.startDate '${manifest.startDate}' ")
        println("***manifest.endDate '${manifest.endDate}' ")
        println("***manifest.name '${manifest.name}' = ${manifest.name?.cryptoHash} ")
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
            println("  contest ${contest.contestId} = ${contest.cryptoHash}")
            for (selection in contest.selections) {
                println("    selection ${selection.selectionId} = ${selection.cryptoHash}")
            }
        }

        println()
        println("***manifestCryptoHash")
        val cryptoHash = manifestCryptoHash(
            manifest.electionScopeId,
            manifest.electionType,
            manifest.startDate,
            manifest.endDate,
            manifest.geopoliticalUnits,
            manifest.parties,
            manifest.candidates,
            manifest.contests,
            manifest.ballotStyles,
            manifest.name,
            manifest.contactInformation
        )

        println()
        println("***manifest.cryptohash '${manifest.cryptoHash}' ")
        println()

        val expect = "b2af12d76e8d1a869213fa7f3136eede0bb125250b9f3a23a95998665180d45f".fromSafeHex()
            .toUInt256()
        assertEquals(expect, manifest.cryptoHash)
    }

    fun recalcManifestHash(manifest: Manifest) {
        println("***Manifest crypto_hash: ${manifest.cryptoHash}")
        val cryptoHash = manifestCryptoHash(
            manifest.electionScopeId,
            manifest.electionType,
            manifest.startDate,
            manifest.endDate,
            manifest.geopoliticalUnits,
            manifest.parties,
            manifest.candidates,
            manifest.contests,
            manifest.ballotStyles,
            manifest.name,
            manifest.contactInformation
        )

        println("***manifest.electionScopeId '${manifest.electionScopeId}' ")
        println("***manifest.electionType '${manifest.electionType}' ")
        println("***manifest.startDate '${manifest.startDate}' ")
        println("***manifest.endDate '${manifest.endDate}' ")
        println("***manifest.name '${manifest.name}' ")
    }

    fun validateManifestHash(manifest: Manifest) {
        println("Manifest crypto_hash: ${manifest.cryptoHash}")
        val cryptoHash = manifestCryptoHash(
            manifest.electionScopeId,
            manifest.electionType,
            manifest.startDate,
            manifest.endDate,
            manifest.geopoliticalUnits,
            manifest.parties,
            manifest.candidates,
            manifest.contests,
            manifest.ballotStyles,
            manifest.name,
            manifest.contactInformation
        )
        println("Expected crypto_hash: $cryptoHash")
        assertEquals(manifest.cryptoHash, cryptoHash)
        for (contest in manifest.contests) {
            println("  contest ${contest.contestId} crypto_hash: ${contest.cryptoHash}")
            for (selection in contest.selections) {
                val scryptoHash = selectionDescriptionCryptoHash(
                    selection.selectionId,
                    selection.sequenceOrder,
                    selection.candidateId,
                )
                // println("    Selection ${selection.selectionId} crypto_hash: ${selection.cryptoHash}")
                // println("    Expected  crypto_hash: $scryptoHash")
                assertEquals(selection.cryptoHash, scryptoHash)
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
                contest.primaryPartyIds,
            )
            // println("  Contest ${contest.contestId} crypto_hash: ${contest.cryptoHash}")
            // println("  Expected crypto_hash: $ccryptoHash")
            assertEquals(contest.cryptoHash, ccryptoHash)
        }
    }
}