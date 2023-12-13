package electionguard.preencrypt

import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.encrypt.cast
import electionguard.cli.ManifestBuilder
import electionguard.json2.import
import electionguard.json2.publishJson
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.util.ErrorMessages
import kotlin.random.Random
import kotlin.test.Test

private val random = Random

class PreEncryptorOutputTest {

    // multiple selections per contest
    @Test
    fun testMultipleSelections() {
        val group = productionGroup()

        val ebuilder = ManifestBuilder("testMultipleSelections")
        val manifest: Manifest = ebuilder.addContest("onlyContest")
            .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
            .addSelection("selection1", "candidate1")
            .addSelection("selection2", "candidate2")
            .addSelection("selection3", "candidate3")
            .done()
            .build()

        runCompleteOutput(group,
            "src/commonTest/data/workflow/allAvailableProto",
            "testOut/preencrypt/PreEncryptorOutputProto",
            "testPreencrypt",
            manifest,
            ::markBallotToLimit,
                true)

        runCompleteOutput(group,
            "src/commonTest/data/workflow/allAvailableJson",
            "testOut/preencrypt/PreEncryptorOutputJson",
            "testPreencrypt",
            manifest,
            ::markBallotToLimit,
            true)
    }

    internal fun runCompleteOutput(
        group: GroupContext,
        input: String,
        output:String,
        ballot_id: String,
        manifest: Manifest,
        markBallot: (manifest: Manifest, pballot: PreEncryptedBallot) -> MarkedPreEncryptedBallot,
        show: Boolean,
    ) {
        if (show) println("===================================================================")

        val primaryNonce = UInt256.random()

        // pre-encrypt
        val electionRecord = readElectionRecord(group, input)
        val publicKey = electionRecord.jointPublicKey()!!
        val extendedBaseHash = electionRecord.extendedBaseHash()!!

        val preEncryptor = PreEncryptor(group, manifest, publicKey, extendedBaseHash, ::sigma)
        val pballot = preEncryptor.preencrypt(ballot_id, "ballotStyle", primaryNonce)
        if (show) pballot.show()

        // vote
        val markedBallot = markBallot(manifest, pballot)
        if (show) markedBallot.show()

        // record
        val recorder = Recorder(group, manifest, publicKey, extendedBaseHash, "device", ::sigma)
        val errs = ErrorMessages("MarkedBallot ${markedBallot.ballotId}")
        val pair = with(recorder) {
            markedBallot.record(primaryNonce, errs)
        }
        if (errs.hasErrors()) {
            println(errs)
            return
        }
        val (recordedBallot, ciphertextBallot) = pair!!

        // show record results
        if (show) {
            println("\nCiphertextBallot ${ciphertextBallot.ballotId}")
            for (contest in ciphertextBallot.contests) {
                println(" contest ${contest.contestId}")
                contest.selections.forEach {
                    println("   selection ${it.selectionId} = ${it.ciphertext}")
                }
            }
            println()
            recordedBallot.show()
            println()
        }

        // roundtrip through the serialization, which combines the recordedBallot
        val encryptedBallot = ciphertextBallot.cast()
        val json = encryptedBallot.publishJson(recordedBallot)
        val fullEncryptedBallot = json.import(group, ErrorMessages(""))!!

        // show what ends up in the election record
        if (show) {
            println("\nEncryptedBallot ${fullEncryptedBallot.ballotId}")
            for (contest in fullEncryptedBallot.contests) {
                println(" contest ${contest.contestId}")
                contest.selections.forEach {
                    println("   selection ${it.selectionId} = ${it.encryptedVote}")
                }
                contest.preEncryption?.show()
            }
            println()
        }

        val publisher = makePublisher(output, true, electionRecord.isJson())
        publisher.writeElectionInitialized(electionRecord.electionInit()!!)
        val sink = publisher.encryptedBallotSink("PreEncryptorOutputTest")
        sink.writeEncryptedBallot(fullEncryptedBallot)
    }

    // pick all selections 0..limit-1
    internal fun markBallotToLimit(pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
        val pcontests = mutableListOf<MarkedPreEncryptedContest>()
        for (pcontest in pballot.contests) {
            val shortCodes = mutableListOf<String>()
            val selections = mutableListOf<String>()
            val nselections = pcontest.selections.size
            val doneIdx = mutableSetOf<Int>()

            while (doneIdx.size < pcontest.votesAllowed) {
                val idx = random.nextInt(nselections)
                if (!doneIdx.contains(idx)) {
                    shortCodes.add(sigma(pcontest.selections[idx].selectionHash.toUInt256safe()))
                    selections.add(pcontest.selections[idx].selectionId)
                    doneIdx.add(idx)
                }
            }

            pcontests.add(
                MarkedPreEncryptedContest(
                    pcontest.contestId,
                    shortCodes,
                    selections,
                )
            )
        }

        return MarkedPreEncryptedBallot(
            pballot.ballotId,
            pballot.ballotStyleId,
            pcontests,
        )
    }
}