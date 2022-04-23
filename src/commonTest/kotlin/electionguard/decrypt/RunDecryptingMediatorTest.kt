@file:OptIn(ExperimentalCli::class)

package electionguard.decrypt

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.TallyResult
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode

import kotlinx.cli.ExperimentalCli
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunDecryptingMediatorTest {

    @Test
    fun testDecryptingMediator() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/workflow"
        val guardianDir =  "src/commonTest/data/testJava/kickstart/keyCeremony/election_private_data"
        val outputDir =  "testOut/kotlin2"
        runDecryptingMediator(group, inputDir, outputDir, makeDecryptingTrustees(group, guardianDir))
    }

    fun makeDecryptingTrustees(group: GroupContext, guardianDir: String) : List<DecryptingTrusteeIF> {
        val consumer = ElectionRecord(guardianDir, group)
        return consumer.readTrustees(guardianDir)
    }

    fun runDecryptingMediator(group: GroupContext, inputDir: String, outputDir: String, decryptingTrustees : List<DecryptingTrusteeIF>) {
        val electionRecordIn = ElectionRecord(inputDir, group)
        val tallyResult: TallyResult = electionRecordIn.readTallyResult().getOrThrow { IllegalStateException( it ) }
        val decryptor = DecryptingMediator(group, tallyResult, decryptingTrustees)
        val decryptedTally = with (decryptor) { tallyResult.ciphertextTally.decrypt() }

        val pkeys: Iterable<ElementModP> = decryptingTrustees.map {it.electionPublicKey()}
        val pkey: ElementModP = with (group) { pkeys.multP() }
        assertEquals(tallyResult.jointPublicKey(), ElGamalPublicKey(pkey))

        val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
        publisher.writeDecryptionResult(
            DecryptionResult(
                tallyResult,
                decryptedTally,
                decryptor.computeAvailableGuardians(),
            )
        )
    }
}
