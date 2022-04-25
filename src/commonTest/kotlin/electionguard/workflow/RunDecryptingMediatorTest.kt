@file:OptIn(ExperimentalCli::class)

package electionguard.workflow

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.DecryptionResult
import electionguard.ballot.TallyResult
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModP
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
import kotlin.test.assertEquals

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
@Test
fun testDecryptingMediator() {
    val group = productionGroup()
    val inputDir = "testOut/kotlin2"

    val trusteeDir = "testOut/runFakeKeyCeremonyTest/private_data"
    val outputDir = "testOut/kotlin2"
    runDecryptingMediator(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir))
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
    val decryptor = DecryptingMediator(group, tallyResult, decryptingTrustees)
    val decryptedTally = with(decryptor) { tallyResult.ciphertextTally.decrypt() }

    val pkeys: Iterable<ElementModP> = decryptingTrustees.map { it.electionPublicKey() }
    val pkey: ElementModP = with(group) { pkeys.multP() }
    assertEquals(tallyResult.jointPublicKey(), ElGamalPublicKey(pkey))

    val took = getSystemTimeInMillis() - starting
    println("RunDecryptingMediator took $took millisecs")

    val metadata: MutableMap<String, String> = mutableMapOf()
    metadata.put("CreatedBy", "runDecryptingMediatorTest")
    metadata.put("CreatedOn", getSystemDate().toString())
    metadata.put("CreatedFrom", inputDir)

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeDecryptionResult(
        DecryptionResult(
            tallyResult,
            decryptedTally,
            decryptor.computeAvailableGuardians(),
            metadata,
        )
    )
}
