package electionguard.publish

import electionguard.core.productionGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunElectionRecordConvertTest {

    @Test
    fun runElectionRecordConvertTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/runWorkflowSomeAvailable",
                "-out",
                "testOut/RunElectionRecordConvertTest",
            )
        )
    }

    @Test
    fun runElectionRecordConvertRoundtrip() {
        val orgDir = "src/commonTest/data/runWorkflowSomeAvailable"
        val jsonDir = "testOut/RunElectionRecordConvertJson"
        val protoDir = "testOut/RunElectionRecordConvertProto"
        val group = productionGroup()
        runElectionRecordConvert(group, orgDir, jsonDir, true)
        runElectionRecordConvert(group, jsonDir, protoDir, true)

        val erOrg = electionRecordFromConsumer(makeConsumer(orgDir, group))
        val erProto = electionRecordFromConsumer(makeConsumer(protoDir, group))

        assertEquals(erOrg.stage(), erProto.stage())
        assertEquals(erOrg.constants(), erProto.constants())
        assertEquals(erOrg.manifest(), erProto.manifest())
        if (erOrg.electionInit() != null) {
          assertTrue(erProto.electionInit()!!.approxEquals(erOrg.electionInit()!!))
        }

        assertTrue(erOrg.encryptedBallots{ true }.approxEqualsEncryptedBallots(erProto.encryptedBallots{ true }))
        assertTrue(erOrg.decryptedBallots().approxEqualsDecryptedBallots(erProto.decryptedBallots()))

        assertEquals(erOrg.encryptedTally(), erProto.encryptedTally())
        assertEquals(erOrg.tallyResult()!!.encryptedTally, erProto.tallyResult()!!.encryptedTally)
        assertEquals(erOrg.decryptedTally(), erProto.decryptedTally())
        assertEquals(erOrg.decryptionResult()!!.decryptedTally, erProto.decryptionResult()!!.decryptedTally)
    }
}