package electionguard.cli

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.encrypt.AddEncryptedBallot
import electionguard.input.ManifestInputValidation
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.util.ErrorMessages
import electionguard.verifier.VerifyEncryptedBallots
import kotlin.random.Random

class RunExampleEncryption {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val inputDir = "src/commonTest/data/workflow/allAvailableJson"
            val outputDir = "testOut/encrypt/RunExampleEncryption"
            val device = "device0"

            val group = productionGroup()
            val consumerIn = makeConsumer(group, inputDir)
            val initResult = consumerIn.readElectionInitialized()
            if (initResult is Err) {
                println("readElectionInitialized error ${initResult.error}")
                return
            }
            val electionInit = initResult.unwrap()
            val manifest = consumerIn.makeManifest(electionInit.config.manifestBytes)
            val errors = ManifestInputValidation(manifest).validate()
            if (ManifestInputValidation(manifest).validate().hasErrors()) {
                throw RuntimeException("ManifestInputValidation error $errors")
            }

            val publisher = makePublisher(outputDir, true, consumerIn.isJson())
            publisher.writeElectionInitialized(electionInit)

            val encryptor = AddEncryptedBallot(
                group,
                manifest,
                electionInit.config.chainConfirmationCodes,
                electionInit.config.configBaux0,
                electionInit.jointPublicKey(),
                electionInit.extendedBaseHash,
                device,
                outputDir = outputDir,
                invalidDir ="testOut/encrypt/invalidDir",
                isJson = publisher.isJson(),
            )

            // encrypt randomly generated ballots
            val nballots = 17
            val ballotProvider = RandomBallotProvider(manifest)
            repeat(nballots) {
                val ballot = ballotProvider.getFakeBallot(manifest, "ballotStyle", "ballot$it")
                val errs = ErrorMessages("Ballot ${ballot.ballotId}")
                val cballot = encryptor.encrypt(ballot, errs)
                if (cballot != null) {
                    val ccode = cballot.confirmationCode
                    // randomly challenge a few
                    val challengeIt = Random.nextInt(nballots) < 2
                    if (challengeIt) {
                        val decryptResult = encryptor.challengeAndDecrypt(ccode)
                        if (decryptResult is Ok) {
                            println("challenged $ccode, decryption Ok = ${ballot == decryptResult.unwrap()}")
                        } else {
                            println("challengeAndDecrypt error = ${decryptResult.getError()}")
                        }
                    } else {
                        encryptor.cast(ccode)
                    }
                } else {
                    println("encryptResult error = ${errs}")
                }
            }

            // write out the results to outputDir
            encryptor.close()

            // verify
            verifyOutput(group, outputDir, nballots, electionInit.config.chainConfirmationCodes)
        }

        fun verifyOutput(group: GroupContext, outputDir: String, expectedCount : Int, chained: Boolean = false) {
            val consumer = makeConsumer(group, outputDir, false)
            val count = consumer.iterateAllEncryptedBallots { true }.count()
            println("$count EncryptedBallots ok=${count == expectedCount}")

            val record = readElectionRecord(consumer)
            val verifier = VerifyEncryptedBallots(
                group, record.manifest(),
                ElGamalPublicKey(record.jointPublicKey()!!),
                record.extendedBaseHash()!!,
                record.config(), 1
            )

            // Note we are verifying all ballots, not just CAST
            val verifierResult = verifier.verifyBallots(record.encryptedAllBallots { true })
            println("verifyEncryptedBallots: $verifierResult")

            if (chained) {
                val chainResult = verifier.verifyConfirmationChain(record)
                println(" verifyChain: $chainResult")
            }
        }
    }
}