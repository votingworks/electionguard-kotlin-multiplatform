package electionguard.publish

import electionguard.core.productionGroup
import electionguard.verifier.runVerifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// TODO decide if we support conversion
class RunElectionRecordConvertTest {

    // @Test
    fun runElectionRecordConvertTest() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableProto",
                "-out",
                "testOut/publish/RunElectionRecordConvertTest",
            )
        )
    }

    // @Test
    fun runElectionRecordConvertRoundtrip() {
        val orgDir = "src/commonTest/data/workflow/someAvailableProto"
        val jsonDir = "testOut/publish/RunElectionRecordConvertJson"
        val protoDir = "testOut/publish/RunElectionRecordConvertProto"
        val group = productionGroup()
        runElectionRecordConvert(group, orgDir, jsonDir, true)
        runElectionRecordConvert(group, jsonDir, protoDir, true)

        val erOrg = readElectionRecord(group, orgDir)
        val erProto = readElectionRecord(group, protoDir)

        assertEquals(erOrg.stage(), erProto.stage())
        assertEquals(erOrg.constants(), erProto.constants())
        assertEquals(erOrg.manifest(), erProto.manifest())
        if (erOrg.electionInit() != null) {
          assertTrue(erProto.electionInit()!!.approxEquals(erOrg.electionInit()!!))
        }

        assertTrue(erOrg.encryptedAllBallots{ true }.approxEqualsEncryptedBallots(erProto.encryptedAllBallots{ true }))
        assertTrue(erOrg.decryptedBallots().approxEqualsDecryptedBallots(erProto.decryptedBallots()))

        assertEquals(erOrg.encryptedTally(), erProto.encryptedTally())
        assertEquals(erOrg.tallyResult()!!.encryptedTally, erProto.tallyResult()!!.encryptedTally)
        assertEquals(erOrg.decryptedTally(), erProto.decryptedTally())
        assertEquals(erOrg.decryptionResult()!!.decryptedTally, erProto.decryptionResult()!!.decryptedTally)

        // test rountrip verify
        runVerifier(productionGroup(), "testOut/publish/RunElectionRecordConvertProto", 11, true)
    }
}