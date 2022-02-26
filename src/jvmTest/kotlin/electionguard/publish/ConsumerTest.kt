package electionguard.publish

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.core.productionGroup
import electionguard.core.runTest
import electionguard.protoconvert.ElectionRecordFromProto
import electionguard.protoconvert.ElectionRecordToProto
import publish.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsumerTest {

    @Test
    fun readElectionRecordWrittenByDecryptorJava() {
        runTest {
            val context = productionGroup()
            readElectionRecord(context,
                "src/commonTest/data/workflow/decryptor/election_record/")
        }
    }

    @Test
    fun readElectionRecordWrittenByEncryptorJava() {
        runTest {
            val context = productionGroup()
            readElectionRecord(context,
                "src/commonTest/data/workflow/encryptor/election_record/")
        }
    }

    fun readElectionRecord(context : GroupContext, topdir : String) {
            val consumer: Consumer = Consumer.fromElectionRecord(topdir, context)
            val allData : ElectionRecordAllData = consumer.readElectionRecord()
            val electionRecord = allData.electionRecord
            println(electionRecord)

            val convertTo = ElectionRecordToProto(context)
            val proto = convertTo.translateToProto(electionRecord)
            val convertFrom = ElectionRecordFromProto(context)
            val roundtrip = convertFrom.translateFromProto(proto)
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