package electionguard.protoconvert

import electionguard.ballot.DecryptionShare
import electionguard.ballot.PlaintextTally
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.core.tinyGroup
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaintextTallyConvertTest {

    @Test
    fun roundtripPlaintextTally() {
        val context = tinyGroup()
        val tally = generateFakeTally(1, context)
        val tallyConvert = PlaintextTallyConvert(context)
        val proto = tallyConvert.translateToProto(tally)
        val roundtrip = tallyConvert.translateFromProto(proto)
        assertEquals(roundtrip, tally)
    }

    companion object {

        fun generateFakeTally(seq: Int, context: GroupContext): PlaintextTally {
            val contests = List(7) { generateFakeContest(it, context) }
            return PlaintextTally("tallyId%seq", contests.associate { it.contestId to it })
        }

        private fun generateFakeContest(cseq: Int, context: GroupContext): PlaintextTally.Contest {
            val selections = List(11) { generateFakeSelection(it, context) }
            return PlaintextTally.Contest("contest" + cseq, selections.associate { it.selectionId to it })
        }

        private fun generateFakeSelection(sseq: Int, context: GroupContext): PlaintextTally.Selection {
            val dselections = List(11) { generateCiphertextDecryptionSelection(it, context) }
            //         val selectionId: String, // matches SelectionDescription.selectionId
            //        val tally: Int,
            //        val value: ElementModP,
            //        val message: ElGamalCiphertext,
            //        val shares: List<DecryptionShare.CiphertextDecryptionSelection>,
            return PlaintextTally.Selection(
                "selection" + sseq,
                sseq,
                generateElementModP(context),
                generateCiphertext(context),
                dselections
            )
        }

        private fun generateCiphertextDecryptionSelection(
            sseq: Int,
            context: GroupContext
        ): DecryptionShare.CiphertextDecryptionSelection {
            val cdselections = List(11) { generateCiphertextCompensatedDecryptionSelection(it, context) }
            //          val guardianId : String,
            //        val share: ElementModP,
            //        val proof : GenericChaumPedersenProof?,
            //        val recoveredParts: Map<String, CiphertextCompensatedDecryptionSelection>?)
            return DecryptionShare.CiphertextDecryptionSelection(
                "selection" + sseq,
                "guardian" + sseq,
                generateElementModP(context),
                generateGenericChaumPedersenProof(context),
                cdselections.associate { it.missingGuardianId to it }
            )
        }

        private fun generateCiphertextCompensatedDecryptionSelection(
            sseq: Int,
            context: GroupContext
        ): DecryptionShare.CiphertextCompensatedDecryptionSelection {
            //          val guardianId : String,
            //        val missingGuardianId : String,
            //        val share : ElementModP,
            //        val recoveryKey : ElementModP,
            //        val proof : GenericChaumPedersenProof
            return DecryptionShare.CiphertextCompensatedDecryptionSelection(
                "selection" + sseq,
                "guardian" + sseq,
                "guardian" + (sseq + 7),
                generateElementModP(context),
                generateElementModP(context),
                generateGenericChaumPedersenProof(context),
            )
        }
    }

}