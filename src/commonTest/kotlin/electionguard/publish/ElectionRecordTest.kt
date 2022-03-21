package electionguard.publish

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.protoconvert.importElectionRecord
import electionguard.protoconvert.publishElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ElectionRecordTest {

    @Test
    fun readElectionRecordWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
            readElectionRecordAll(
                context,
                "src/commonTest/data/workflow/decryptor/"
            )
        }
    }

    @Test
    fun readElectionRecordWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            readElectionRecord(
                context,
                "src/commonTest/data/workflow/encryptor/"
            )
        }
    }

    fun readElectionRecordAll(context: GroupContext, topdir: String) {
        val consumer = Consumer(topdir, context)
        val allData: ElectionRecordAllData = consumer.readElectionRecordAllData()

        val proto = allData.publishElectionRecord()
        val roundtrip = proto.importElectionRecord(context)
        assertNotNull(roundtrip)
        assertEquals(roundtrip.protoVersion, allData.protoVersion)
        assertEquals(roundtrip.constants, allData.constants)
        assertEquals(roundtrip.manifest, allData.manifest)
        assertEquals(roundtrip.context, allData.context)
        assertEquals(roundtrip.guardianRecords, allData.guardianRecords)
        assertEquals(roundtrip.devices, allData.devices)
        assertEquals(roundtrip.encryptedTally, allData.encryptedTally)
        assertEquals(roundtrip.decryptedTally, allData.decryptedTally)
        assertEquals(roundtrip.availableGuardians, allData.availableGuardians)
    }

    fun readElectionRecord(context: GroupContext, topdir: String) {
        val consumer = Consumer(topdir, context)
        val allData: ElectionRecord = consumer.readElectionRecord()

        val proto = allData.publishElectionRecord()
        val roundtrip = proto.importElectionRecord(context)
        assertNotNull(roundtrip)
        assertEquals(roundtrip.protoVersion, allData.protoVersion)
        assertEquals(roundtrip.constants, allData.constants)
        assertEquals(roundtrip.manifest, allData.manifest)
        assertEquals(roundtrip.context, allData.context)
        assertEquals(roundtrip.guardianRecords, allData.guardianRecords)
        assertEquals(roundtrip.devices, allData.devices)
        assertEquals(roundtrip.encryptedTally, allData.encryptedTally)
        assertEquals(roundtrip.decryptedTally, allData.decryptedTally)
        assertEquals(roundtrip.availableGuardians, allData.availableGuardians)
    }
}