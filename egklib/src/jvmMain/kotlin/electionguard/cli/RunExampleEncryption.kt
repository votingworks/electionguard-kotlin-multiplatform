package electionguard.cli

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.core.*
import electionguard.encrypt.AddEncryptedBallot
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.verifier.VerifyEncryptedBallots

class RunExampleEncryption {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val inputDir = "src/commonTest/data/workflow/allAvailableJson"
            val outputDir = "testOut/encrypt/ExampleEncryption"
            val device = "device0"
            val chained = false

            val group = productionGroup()
            val electionRecord = readElectionRecord(group, inputDir)
            val electionInit = electionRecord.electionInit()!!
            val publisher = makePublisher(outputDir, true, true)
            publisher.writeElectionInitialized(electionInit)

            val encryptor = AddEncryptedBallot(
                group,
                electionRecord.manifest(),
                electionInit,
                device,
                electionRecord.config().configBaux0,
                chained,
                outputDir,
                "${outputDir}/invalidDir",
                true, // isJson
                false,
            )

            // encrypt 7 randomly generated ballots
            val ballotProvider = RandomBallotProvider(electionRecord.manifest())
            repeat(7) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot)
                encryptor.submit(result.unwrap().confirmationCode, EncryptedBallot.BallotState.CAST)
            }

            // write out the results to outputDir
            encryptor.close()

            // verify
            verifyOutput(group, outputDir, chained)
        }

        fun verifyOutput(group: GroupContext, outputDir: String, chained: Boolean = false) {
            val consumer = makeConsumer(outputDir, group, false)
            var count = 0
            consumer.iterateAllEncryptedBallots { true }.forEach {
                count++
            }
            println("$count EncryptedBallots")

            val record = readElectionRecord(consumer)
            val verifier = VerifyEncryptedBallots(
                group, record.manifest(),
                ElGamalPublicKey(record.jointPublicKey()!!),
                record.extendedBaseHash()!!,
                record.config(), 1
            )

            // Note we are verifying all ballots, not just CAST
            val verifierResult = verifier.verifyBallots(record.encryptedAllBallots { true })
            println("verifyEncryptedBallots $verifierResult")

            if (chained) {
                val chainResult = verifier.verifyConfirmationChain(record)
                println(" verifyChain $chainResult")
            }
        }
    }
}