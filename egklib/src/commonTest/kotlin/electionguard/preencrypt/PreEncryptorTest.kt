package electionguard.preencrypt

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.Manifest
import electionguard.core.*
import electionguard.decryptBallot.DecryptPreencryptWithNonce
import electionguard.encrypt.cast
import electionguard.input.ManifestBuilder
import electionguard.protoconvert.import
import electionguard.protoconvert.publishProto
import electionguard.publish.makeConsumer
import electionguard.verifier.VerifyEncryptedBallots
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val random = Random

internal class PreEncryptorTest {
    val input = "src/commonTest/data/runWorkflowAllAvailable"
    val group = productionGroup()

    // sanity check that PreEncryptor.preencrypt doesnt barf
    @Test
    fun testPreencrypt() {
        runTest {
            val consumerIn = makeConsumer(input, group)
            val electionInit = consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val manifest = electionInit.manifest()

            val preEncryptor =
                PreEncryptor(group, manifest, electionInit.jointPublicKey, electionInit.extendedBaseHash, ::sigma)

            manifest.ballotStyles.forEach { println(it) }

            val pballot = preEncryptor.preencrypt("testPreencrypt_ballot_id", "ballotStyle", 11U.toUInt256())
            pballot.show()
        }
    }

    // sanity check that Recorder.record doesnt barf
    @Test
    fun testRecord() {
        runTest {
            val consumerIn = makeConsumer(input, group)
            val electionInit: ElectionInitialized =
                consumerIn.readElectionInitialized().getOrThrow { IllegalStateException(it) }
            val manifest = electionInit.manifest()

            val preEncryptor =
                PreEncryptor(group, manifest, electionInit.jointPublicKey, electionInit.extendedBaseHash, ::sigma)

            manifest.ballotStyles.forEach { println(it) }

            val primaryNonce = 42U.toUInt256()
            val pballot = preEncryptor.preencrypt("testDecrypt_ballot_id", "ballotStyle", primaryNonce)
            pballot.show()

            val mballot = markBallotChooseOne(manifest, pballot)
            mballot.show()

            val recorder =
                Recorder(group, manifest, electionInit.jointPublicKey, electionInit.extendedBaseHash, ::sigma)

            with(recorder) {
                mballot.record(primaryNonce)
            }
        }
    }

    // check that CiphertextBallot is correctly formed
    @Test
    fun testSingleLimit() {
        runTest {
            val ebuilder = ManifestBuilder("testSingleLimit")
            val manifest: Manifest = ebuilder.addContest("onlyContest")
                .addSelection("selection1", "candidate1")
                .addSelection("selection2", "candidate2")
                .done()
                .build()

            val chosenBallot = ChosenBallot(1)
            runComplete(group, "testSingleLimit", manifest, chosenBallot::markedBallot, true)
        }
    }

    @Test
    fun testSingleLimitProblem() {
        runTest {
            val ebuilder = ManifestBuilder("testSingleLimit")
            val manifest: Manifest = ebuilder.addContest("onlyContest")
                .addSelection("selection1", "candidate1")
                .done()
                .build()

            val chosenBallot = ChosenBallot(1)
            runComplete(group, "testSingleLimit", manifest, chosenBallot::markedBallot, true)
        }
    }

    @Test
    fun fuzzTestSingleLimit() {
        runTest {
            var count = 0
            println("fuzzTestSingleLimit")
            checkAll(
                iterations = 50,
                Arb.int(min = 1, max = 9),
            ) { nselections ->
                val ebuilder = ManifestBuilder("fuzzTestSingleLimit")
                val cbuilder = ebuilder.addContest("onlyContest")
                    repeat(nselections) {
                        cbuilder.addSelection("selection$it", "candidate$it")
                    }
                cbuilder.done()
                val manifest: Manifest = ebuilder.build()
                runComplete(group, "fuzzTestSingleLimit$count", manifest, ::markBallotChooseOne, false)
                count++
                if (count % 10 == 0) {
                    println(" $count")
                }
            }
        }
    }

    // multiple selections per contest
    @Test
    fun testMultipleSelections() {
        runTest {
            val ebuilder = ManifestBuilder("testMultipleSelections")
            val manifest: Manifest = ebuilder.addContest("onlyContest")
                .setVoteVariationType(Manifest.VoteVariationType.n_of_m, 2)
                .addSelection("selection1", "candidate1")
                .addSelection("selection2", "candidate2")
                .addSelection("selection3", "candidate3")
                .done()
                .build()

            runComplete(group, "testMultipleSelections", manifest, ::markBallotToLimit, true)
        }
    }

    @Test
    fun fuzzTestMultipleSelections() {
        runTest {
            var count = 0
            println("fuzzTestMultipleSelections")
            checkAll(
                iterations = 50,
                Arb.int(min = 2, max = 9),
                Arb.int(min = 2, max = 9),
            ) { nselections, contestLimit ->
                val votesAllowed = min(nselections, contestLimit)
                val ebuilder = ManifestBuilder("fuzzTestMultipleSelections")
                val cbuilder = ebuilder.addContest("onlyContest")
                    .setVoteVariationType(Manifest.VoteVariationType.n_of_m, votesAllowed)

                repeat(nselections) {
                    cbuilder.addSelection("selection$it", "candidate$it")
                }
                cbuilder.done()
                val manifest: Manifest = ebuilder.build()

                runComplete(group, "fuzzTestMultipleSelections.$count", manifest, ::markBallotToLimit, false)
                count++
                if (count % 10 == 0) {
                    println(" $count")
                }
            }
        }
    }
}

internal fun runComplete(
    group: GroupContext,
    ballot_id: String,
    manifest: Manifest,
    markBallot: (manifest: Manifest, pballot: PreEncryptedBallot) -> MarkedPreEncryptedBallot,
    show: Boolean,
) {
    if (show) println("===================================================================")
    val qbar = 4242U.toUInt256()
    val secret = group.randomElementModQ(minimum = 1)
    val publicKey = group.gPowP(secret)
    val primaryNonce = UInt256.random()

    // pre-encrypt
    val preEncryptor = PreEncryptor(group, manifest, publicKey, qbar, ::sigma)
    val pballot = preEncryptor.preencrypt(ballot_id, "ballotStyle", primaryNonce)
    if (show) pballot.show()

    // vote
    val markedBallot = markBallot(manifest, pballot)
    if (show) markedBallot.show()

    // record
    val recorder = Recorder(group, manifest, publicKey, qbar, ::sigma)
    val (recordedBallot, ciphertextBallot) = with(recorder) {
        markedBallot.record(primaryNonce)
    }

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

    // roundtrip through the proto, combines the recordedBallot
    val encryptedBallot = ciphertextBallot.cast()
    val proto = encryptedBallot.publishProto(recordedBallot)
    val fullEncryptedBallot = proto.import(group).unwrap()

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

    // verify
    val stats = Stats()
    val verifier =
        VerifyEncryptedBallots(group, manifest, ElGamalPublicKey(publicKey), qbar.toElementModQ(group), 1)
    val results = verifier.verifyEncryptedBallot(fullEncryptedBallot, stats)
    if (show || results !is Ok) {
        println("VerifyEncryptedBallots $results\n")
    }

    // decrypt with nonce
    val decryptionWithPrimaryNonce = DecryptPreencryptWithNonce(group, manifest, ElGamalPublicKey(publicKey), qbar, ::sigma)
    val decryptedBallotResult = with(decryptionWithPrimaryNonce) { fullEncryptedBallot.decrypt(primaryNonce) }
    if (decryptedBallotResult is Err) {
        println("decryptedBallotResult $decryptedBallotResult")
    }
    assertTrue(decryptedBallotResult is Ok)
    val decryptedBallot = decryptedBallotResult.unwrap()

    if (show) {
        println("\nDecryptedBallot ${decryptedBallot.ballotId}")
        for (contest in decryptedBallot.contests) {
            println(" contest ${contest.contestId}")
            contest.selections.forEach {
                println("   selection ${it.selectionId} = ${it.vote}")
            }
        }
        println()
    }

    // check votes are correct
    for (contest in decryptedBallot.contests) {
        if (show) println(" check votes for contest ${contest.contestId}")
        val markedContest = markedBallot.contests.find { it.contestId == contest.contestId }!!
        contest.selections.forEach {
            if (show) println("   selection ${it.selectionId} = ${it.vote}")
            val have = it.vote == 1
            val wanted = markedContest.selectedIds.contains(it.selectionId)
            assertEquals(wanted, have)
        }
    }

    // check decryptedBallot.sequenceOrder corresponds to CiphertextBallot
    for (contest in decryptedBallot.contests) {
        if (show) println(" check decryptedBallot contest ${contest.contestId}")
        val cipherContest = ciphertextBallot.contests.find { it.contestId == contest.contestId }!!
        contest.selections.forEach { plainSelection ->
            val cipherSelection = cipherContest.selections.find { it.selectionId == plainSelection.selectionId }!!
            assertEquals(cipherSelection.sequenceOrder, plainSelection.sequenceOrder)
        }
    }

    assertTrue(results is Ok)
}

fun sigma(hash: UInt256): String = hash.toHex().substring(0, 5)

internal class ChosenBallot(val selectedIdx: Int) {

    fun markedBallot(manifest: Manifest, pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
        val pcontests = mutableListOf<MarkedPreEncryptedContest>()
        for (pcontest in pballot.contests) {
            if (selectedIdx < pcontest.selections.size) {
                val pselection = pcontest.selections[selectedIdx]
                pcontests.add(
                    MarkedPreEncryptedContest(
                        pcontest.contestId,
                        listOf(sigma(pselection.selectionHash.toUInt256())),
                        listOf(pselection.selectionId),
                    )
                )
            } else {
                pcontests.add(
                    MarkedPreEncryptedContest(
                        pcontest.contestId,
                        listOf(),
                        listOf(),
                    )
                )
            }
        }

        return MarkedPreEncryptedBallot(
            pballot.ballotId,
            pballot.ballotStyleId,
            pcontests,
        )
    }
}

// pick one selection to vote for
internal fun markBallotChooseOne(manifest: Manifest, pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
    val pcontests = mutableListOf<MarkedPreEncryptedContest>()
    for (pcontest in pballot.contests) {
        val n = pcontest.selections.size
        val idx = random.nextInt(n)
        val pselection = pcontest.selections[idx]
        pcontests.add(
            MarkedPreEncryptedContest(
                pcontest.contestId,
                listOf(sigma(pselection.selectionHash.toUInt256())),
                listOf(pselection.selectionId),
            )
        )
    }

    return MarkedPreEncryptedBallot(
        pballot.ballotId,
        pballot.ballotStyleId,
        pcontests,
    )
}

// pick all selections 0..limit-1
internal fun markBallotToLimit(manifest: Manifest, pballot: PreEncryptedBallot): MarkedPreEncryptedBallot {
    val pcontests = mutableListOf<MarkedPreEncryptedContest>()
    for (pcontest in pballot.contests) {
        val shortCodes = mutableListOf<String>()
        val selections = mutableListOf<String>()
        val nselections = pcontest.selections.size
        val doneIdx = mutableSetOf<Int>()
        val nvotes = random.nextInt(pcontest.votesAllowed + 1)

        while (doneIdx.size < nvotes) {
            val idx = random.nextInt(nselections)
            if (!doneIdx.contains(idx)) {
                shortCodes.add(sigma(pcontest.selections[idx].selectionHash.toUInt256()))
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