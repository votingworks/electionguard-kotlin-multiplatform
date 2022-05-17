@file:OptIn(ExperimentalCli::class)

package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.getSystemDate
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingMediator
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.ElectionRecord
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode

import kotlinx.cli.ExperimentalCli
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunDecryptingMediatorTest {
    @Test
    fun testDecryptingMediatorAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowAllAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowAllAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingMediator"
        runDecryptingMediator(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir))
    }

    @Test
    fun testDecryptingMediatorSome() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val trusteeDir = "src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"
        val outputDir = "testOut/testDecryptingMediator"
        runDecryptingMediator(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir))
    }
}

fun readDecryptingTrustees(group: GroupContext, inputDir: String, trusteeDir: String): List<DecryptingTrusteeIF> {
    val electionRecordIn = ElectionRecord(inputDir, group)
    val init = electionRecordIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
    val consumer = ElectionRecord(trusteeDir, group)
    return init.guardians.map { consumer.readTrustee(trusteeDir, it.guardianId) }
}

fun runDecryptingMediator(
    group: GroupContext,
    inputDir: String,
    outputDir: String,
    decryptingTrustees: List<DecryptingTrusteeIF>
) {
    val starting = getSystemTimeInMillis()

    val electionRecordIn = ElectionRecord(inputDir, group)
    val tallyResult: TallyResult = electionRecordIn.readTallyResult().getOrThrow { IllegalStateException(it) }
    val trusteeNames = decryptingTrustees.map { it.id()}.toSet()
    val missingGuardians =
        tallyResult.electionIntialized.guardians.filter { !trusteeNames.contains(it.guardianId)}.map { it.guardianId}

    val decryptor = DecryptingMediator(group, tallyResult, decryptingTrustees, missingGuardians)
    val decryptedTally = with(decryptor) { tallyResult.ciphertextTally.decrypt() }
    assertNotNull(decryptedTally)

    val took = getSystemTimeInMillis() - starting
    println("RunDecryptingMediator took $took millisecs")

    val metadata: MutableMap<String, String> = mutableMapOf()
    metadata.put("CreatedBy", "runDecryptingMediator")
    metadata.put("CreatedOn", getSystemDate().toString())
    metadata.put("CreatedFrom", inputDir)

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeDecryptionResult(
        DecryptionResult(
            tallyResult,
            decryptedTally,
            decryptor.availableGuardians,
            metadata,
        )
    )
}
