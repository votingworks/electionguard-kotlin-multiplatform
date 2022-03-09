package electionguard.publish

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.protoconvert.importElectionRecord
import electionguard.protoconvert.publishElectionRecord
import publish.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElectionRecordTest {

    @Test
    fun readElectionRecordWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
            readElectionRecord(
                context,
                "src/commonTest/data/workflow/decryptor/election_record/"
            )
        }
    }

    @Test
    fun readElectionRecordWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            readElectionRecord(
                context,
                "src/commonTest/data/workflow/encryptor/election_record/"
            )
        }
    }

    fun readElectionRecord(context: GroupContext, topdir: String) {
        val consumer: Consumer = Consumer.fromElectionRecord(topdir, context)
        val allData: ElectionRecordAllData = consumer.readElectionRecordAllData()
        val electionRecord = allData.electionRecord

        val proto = electionRecord.publishElectionRecord()
        val roundtrip = proto.importElectionRecord(context)
        assertNotNull(roundtrip)
        assertEquals(roundtrip.protoVersion, electionRecord.protoVersion)
        assertEquals(roundtrip.constants, electionRecord.constants)
        assertEquals(roundtrip.manifest, electionRecord.manifest)
        assertEquals(roundtrip.context, electionRecord.context)
        assertEquals(roundtrip.guardianRecords, electionRecord.guardianRecords)
        assertEquals(roundtrip.devices, electionRecord.devices)
        assertEquals(roundtrip.encryptedTally, electionRecord.encryptedTally)
        assertEquals(roundtrip.decryptedTally, electionRecord.decryptedTally)
        assertEquals(roundtrip.availableGuardians, electionRecord.availableGuardians)

        assertTrue(roundtrip.equals(electionRecord))
        assertEquals(roundtrip, electionRecord)
    }

}