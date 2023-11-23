package electionguard.cli

import kotlin.test.Test

class RunShowElectionRecordTest {

    @Test
    fun testShowElectionRecordTest() {
        RunShowElectionRecord.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/eg",
                "-show", "manifest",
            )
        )
    }

    @Test
    fun testShowElectionRecordDetailsTest() {
        RunShowElectionRecord.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/eg",
                "-show", "manifest",
                "--details",
            )
        )
    }

    @Test
    fun testShowElectionRecordBallotStyleTest() {
        RunShowElectionRecord.main(
            arrayOf(
                "-in", "src/commonTest/data/rave/eg",
                "-show", "manifest",
                "-ballotStyle", "ballot-style-1",
                "--details",
            )
        )
    }

}

