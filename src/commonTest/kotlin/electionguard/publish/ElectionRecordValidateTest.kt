package electionguard.publish

import electionguard.ballot.*
import electionguard.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ElectionRecordValidateTest {

    @Test
    fun readElectionRecordAndValidate() {
        runTest {
            val group = productionGroup()
            val electionRecord = readElectionRecord(
                group,
                "src/commonTest/data/workflow/decryptor/"
            )
            validateTally(group, assertNotNull(electionRecord.decryptedTally), electionRecord.context.numberOfGuardians)
        }
    }

    fun validateTally(group : GroupContext, tally : PlaintextTally, nguardians : Int) {
        for (contest in tally.contests.values) {
            for (selection in contest.selections.values) {
                val actual : Int? = group.dLog(selection.value)
                assertEquals(selection.tally, actual)
                assertEquals(nguardians, selection.shares.size)
            }
        }
    }


    fun readElectionRecord(context: GroupContext, topdir: String) : ElectionRecord {
        val consumer = Consumer(topdir, context)
        val allData: ElectionRecordAllData = consumer.readElectionRecordAllData()
        val electionRecord = allData.electionRecord

        assertNotNull(electionRecord)
        assertEquals(PROTO_VERSION, electionRecord.protoVersion)
        assertEquals("", electionRecord.constants.name)
        assertEquals("v0.95", electionRecord.manifest.specVersion)
        assertEquals(3, electionRecord.context.numberOfGuardians)
        assertEquals(2, electionRecord.context.quorum)
        assertEquals(3, electionRecord.guardianRecords.size)
        assertEquals(1, electionRecord.devices.size)
        assertEquals("deviceName", electionRecord.devices[0].location)
        assertEquals("accumulateTally", electionRecord.encryptedTally?.tallyId)
        assertEquals("accumulateTally", electionRecord.decryptedTally?.tallyId)
        assertNotNull(electionRecord.decryptedTally)
        val contests = electionRecord.decryptedTally?.contests
        assertNotNull(contests)
        val contest = contests["justice-supreme-court"]
        assertNotNull(contest)
        assertEquals(2, electionRecord.availableGuardians?.size)

        return electionRecord
    }

}