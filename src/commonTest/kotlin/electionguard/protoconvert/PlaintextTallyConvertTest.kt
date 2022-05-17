package electionguard.protoconvert

import com.github.michaelbull.result.getOrThrow
import electionguard.ballot.PlaintextTally
import electionguard.core.GroupContext
import electionguard.core.tinyGroup
import electionguard.decrypt.PartialDecryption
import electionguard.decrypt.RecoveredPartialDecryption
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlaintextTallyConvertTest {

    @Test
    fun roundtripPlaintextTally() {
        val context = tinyGroup()
        val tally = generateFakeTally(1, context)
        val proto = tally.publishPlaintextTally()
        val roundtrip = context.importPlaintextTally(proto).getOrThrow { IllegalStateException(it) }
        assertNotNull(roundtrip)
        for (entry in roundtrip.contests) {
            val contest =
                tally.contests.get(entry.key)
                    ?: throw RuntimeException("Cant find contest $entry.key")
            val rcontest = entry.value
            for (entry2 in rcontest.selections) {
                val selection =
                    contest.selections.get(entry2.key)
                        ?: throw RuntimeException("Cant find selection $entry2.key")
                val rselection = entry2.value
                for (shareIdx in 0 until rselection.partialDecryptions.size) {
                    val share = selection.partialDecryptions[shareIdx]
                    val rshare = rselection.partialDecryptions[shareIdx]
                    assertEquals(rshare, share)
                }
            }
        }
        assertEquals(roundtrip, tally)
    }

    companion object {

        fun generateFakeTally(seq: Int, context: GroupContext): PlaintextTally {
            val contests = List(7) { generateFakeContest(it, context) }
            return PlaintextTally("tallyId$seq", contests.associate { it.contestId to it })
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): PlaintextTally.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return PlaintextTally.Contest(
                "contest$cseq",
                selections.associate { it.selectionId to it }
            )
        }

        private fun generateFakeSelection(
            sseq: Int,
            context: GroupContext
        ): PlaintextTally.Selection {
            val dselections = List(11) { generateCiphertextDecryptionSelection(it, context) }
            //         val selectionId: String, // matches SelectionDescription.selectionId
            //        val tally: Int,
            //        val value: ElementModP,
            //        val message: ElGamalCiphertext,
            //        val shares: List<DecryptionShare.CiphertextDecryptionSelection>,
            return PlaintextTally.Selection(
                "selection$sseq",
                sseq,
                generateElementModP(context),
                generateCiphertext(context),
                dselections
            )
        }

        private fun generateCiphertextDecryptionSelection(
            sseq: Int,
            context: GroupContext
        ): PartialDecryption {
            val cdselections =
                List(11) { generateCiphertextCompensatedDecryptionSelection(it, context) }
            val proofOrParts = Random.nextBoolean()
            //          val guardianId : String,
            //        val share: ElementModP,
            //        val proof : GenericChaumPedersenProof?,
            //        val recoveredParts: Map<String, CiphertextCompensatedDecryptionSelection>?)
            return PartialDecryption(
                "selection$sseq",
                "guardian$sseq",
                generateElementModP(context),
                if (proofOrParts) generateGenericChaumPedersenProof(context) else null,
                if (proofOrParts) null else {
                    cdselections
                },
            )
        }

        private fun generateCiphertextCompensatedDecryptionSelection(
            sseq: Int,
            context: GroupContext
        ): RecoveredPartialDecryption {
            //          val guardianId : String,
            //        val missingGuardianId : String,
            //        val share : ElementModP,
            //        val recoveryKey : ElementModP,
            //        val proof : GenericChaumPedersenProof
            return RecoveredPartialDecryption(
                "guardian$sseq",
                "guardian" + (sseq + 7),
                generateElementModP(context),
                generateElementModP(context),
                generateGenericChaumPedersenProof(context),
            )
        }
    }
}