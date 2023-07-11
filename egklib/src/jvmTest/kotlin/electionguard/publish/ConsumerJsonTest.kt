package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.core.productionGroup
import electionguard.input.electionScopeId
import electionguard.input.specVersion
import kotlin.jvm.JvmStatic
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ConsumerJsonTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            Arguments.of("src/commonTest/data/testElectionRecord/convertJson"),
        )
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readElectionRecord(topdir: String) {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, topdir)
        val electionInit = electionRecord.electionInit()

        if (electionInit == null) {
            println("readElectionRecord failed $topdir")
        }

        val manifest = electionRecord.manifest()
        println("electionRecord.manifest.specVersion = ${manifest.specVersion}")
        assertEquals(electionScopeId, manifest.electionScopeId)
        assertEquals(specVersion, manifest.specVersion)
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readSpoiledBallotTallys(topdir: String) {
        val context = productionGroup()
        val consumerIn = makeConsumer(topdir, context)
        var count = 0
        for (tally in consumerIn.iterateDecryptedBallots()) {
            println("$count tally = ${tally.id}")
            assertTrue(tally.id.startsWith("ballot-id"))
            count++
        }
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readEncryptedBallots(topdir: String) {
        val context = productionGroup()
        val consumerIn = makeConsumer(topdir, context)
        var count = 0
        for (ballot in consumerIn.iterateEncryptedBallots { true }) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readEncryptedBallotsCast(topdir: String) {
        val context = productionGroup()
        val consumerIn = makeConsumer(topdir, context)
        var count = 0
        for (ballot in consumerIn.iterateCastBallots()) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readSubmittedBallotsSpoiled(topdir: String) {
        val context = productionGroup()
        val consumerIn = makeConsumer(topdir, context)
        var count = 0
        for (ballot in consumerIn.iterateSpoiledBallots()) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

}