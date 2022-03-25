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

    fun validateTally(group : GroupContext, tally : PlaintextTally, nguardians : Int?) {
        for (contest in tally.contests.values) {
            for (selection in contest.selections.values) {
                val actual : Int? = group.dLogG(selection.value)
                assertEquals(selection.tally, actual)
                assertEquals(nguardians, selection.shares.size)
            }
        }
    }

    fun readElectionRecord(context: GroupContext, topdir: String) : ElectionRecordAllData {
        val consumer = Consumer(topdir, context)
        val allData: ElectionRecordAllData = consumer.readElectionRecordAllData()

        assertNotNull(allData)
        assertEquals(PROTO_VERSION, allData.protoVersion)
        assertEquals("", allData.constants.name)
        assertEquals("v0.95", allData.manifest.specVersion)
        assertEquals(3, allData.context.numberOfGuardians)
        assertEquals(2, allData.context.quorum)
        assertEquals(3, allData.guardianRecords.size)
        assertEquals(1, allData.devices.size)
        assertEquals("deviceName", allData.devices[0].location)
        assertEquals("accumulateTally", allData.encryptedTally.tallyId)
        assertEquals("accumulateTally", allData.decryptedTally.tallyId)
        assertNotNull(allData.decryptedTally)
        val contests = allData.decryptedTally.contests
        assertNotNull(contests)
        val contest = contests["justice-supreme-court"]
        assertNotNull(contest)
        assertEquals(2, allData.availableGuardians.size)

        return allData
    }
}