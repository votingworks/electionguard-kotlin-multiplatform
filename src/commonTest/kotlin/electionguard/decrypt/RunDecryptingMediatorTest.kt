@file:OptIn(ExperimentalCli::class)

package electionguard.decrypt

import electionguard.ballot.ElectionRecord
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode
import kotlinx.cli.ExperimentalCli
import kotlin.test.Test

/** Test DecryptingMediator with in-process DecryptingTrustee's. Cannot use this in production */
class RunDecryptingMediatorTest {

    @Test
    fun testDecryptingMediator() {
        val group = productionGroup()
        val inputDir =  "/home/snake/tmp/electionguard/kotlin/runTallyAccumulation"
        val outputDir =  "/home/snake/tmp/electionguard/kotlin/runDecryptingMediator"
        runDecryptingMediator(group, inputDir, outputDir, decryptingTrustees)
    }

    fun runDecryptingMediator(group: GroupContext, inputDir: String, outputDir: String, decryptingTrustees : List<DecryptingTrustee>) {
        val consumer = Consumer(inputDir, group)
        val electionRecord: ElectionRecord = consumer.readElectionRecord()
        val decryptor = DecryptingMediator(group, electionRecord.context!!, decryptingTrustees)
        val decryptedTally = with(decryptor) { electionRecord.encryptedTally!!.decrypt() }

        val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
        publisher.writeElectionRecordProto(
            electionRecord.manifest,
            electionRecord.constants,
            electionRecord.context,
            electionRecord.guardianRecords,
            electionRecord.devices,
            consumer.iterateSubmittedBallots(),
            electionRecord.encryptedTally,
            decryptedTally,
            null,
            decryptor.computeAvailableGuardians(),
        )
    }

    class DecryptingTrusteeTest(val id : String, val xCoordinate : Int, val publicKey : ElementModP) : DecryptingTrustee {
        override fun id(): String {
            return id
        }

        override fun xCoordinate(): Int {
            return xCoordinate
        }

        override fun electionPublicKey(): ElementModP {
            return publicKey
        }

        override fun partialDecrypt(
            texts: List<ElGamalCiphertext>,
            extended_base_hash: ElementModQ,
            nonce_seed: ElementModQ?
        ): List<PartialDecryptionProof> {
            TODO("Not yet implemented")
        }

        override fun compensatedDecrypt(
            missing_guardian_id: String,
            texts: List<ElGamalCiphertext>,
            extended_base_hash: ElementModQ,
            nonce_seed: ElementModQ?
        ): List<DecryptionProofRecovery> {
            TODO("Not yet implemented")
        }

    }
}
