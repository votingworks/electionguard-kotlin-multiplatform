package electionguard.publish

import electionguard.ballot.*
import electionguard.core.productionGroup
import electionguard.core.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test Manifest hash values against the ones that python and java generate. */
class ManifestHashValidateTest {
    val input = "src/commonTest/data/testPython/"

    @Test
    fun readElectionRecordWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            val consumer = Consumer(input, context)
            val electionRecord: ElectionRecord = consumer.readElectionRecord()
            validateManifestHash(electionRecord.manifest)
        }
    }

    fun validateManifestHash(manifest: Manifest) {
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
        println("Manifest crypto_hash: ${manifest.cryptoHash}")
        println("Expected crypto_hash: $cryptoHash")
        assertEquals(manifest.cryptoHash, cryptoHash)
        for (contest in manifest.contests) {
            for (selection in contest.selections) {
                val scryptoHash = selectionDescriptionCryptoHash(
                    selection.selectionId,
                    selection.sequenceOrder,
                    selection.candidateId,
                )
                println("    Selection crypto_hash: ${selection.cryptoHash}")
                println("    Expected  crypto_hash: $scryptoHash")
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
            println("  Contest  crypto_hash: ${contest.cryptoHash}")
            println("  Expected crypto_hash: $ccryptoHash")
            assertEquals(contest.cryptoHash, ccryptoHash)
        }
    }
}