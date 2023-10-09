package electionguard.publish

import electionguard.ballot.protocolVersion
import electionguard.cli.ManifestBuilder.Companion.electionScopeId
import electionguard.core.productionGroup
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
            println("readElectionRecord error $topdir")
        }

        val manifest = electionRecord.manifest()
        println("electionRecord.manifest.specVersion = ${manifest.specVersion}")
        assertEquals(electionScopeId, manifest.electionScopeId)
        assertEquals(protocolVersion, manifest.specVersion)
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readSpoiledBallotTallys(topdir: String) {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
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
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (ballot in consumerIn.iterateAllEncryptedBallots { true }) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readEncryptedBallotsCast(topdir: String) {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (ballot in consumerIn.iterateAllCastBallots()) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readSubmittedBallotsSpoiled(topdir: String) {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (ballot in consumerIn.iterateAllSpoiledBallots()) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

}