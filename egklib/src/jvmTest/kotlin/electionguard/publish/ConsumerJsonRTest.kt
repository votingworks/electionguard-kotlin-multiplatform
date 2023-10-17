package electionguard.publish

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.productionGroup
import electionguard.json.ElectionHashes
import electionguard.json.ElectionParameters
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlin.test.Test

class ConsumerJsonRTest {
    val topdir = "src/commonTest/data/testElectionRecord/egrust"
    val topdirw = "/home/stormy/dev/github/electionguard-rust/src/working3/"

    @Test
    fun readElectionConfig() {
        val group = productionGroup()
        val consumerIn = ConsumerJsonR(topdir, group)

        val configResult = consumerIn.readElectionConfig()
        assertTrue(configResult is Ok)
        println("${configResult.unwrap().show()}")

        val config : ElectionConfig = configResult.unwrap()
        val Hpr = parameterBaseHash( config.constants)
        val Hp = parameterBaseHash( group.constants)
        assertEquals(Hp, Hpr)
        println("OK Hp = $Hp")
        println("OK Hpr= $Hpr")
        println("${group.constants}")

        testHashes(config)
    }

    private fun testHashes(config: ElectionConfig) {
        println("Hp = ${config.parameterBaseHash} ours = ${parameterBaseHash(config.constants)}")
        println("Hm = ${config.manifestHash} should be = ${manifestHash(config.parameterBaseHash, config.manifestBytes)}")
        println("Hb = ${config.electionBaseHash} should be = ${
            electionBaseHash(config.parameterBaseHash, config.manifestHash, config.numberOfGuardians, config.quorum)
        }")
        println()

        assertEquals(parameterBaseHash(config.constants), config.parameterBaseHash)
        assertEquals(manifestHash(config.parameterBaseHash, config.manifestBytes), config.manifestHash)
        assertEquals(
            electionBaseHash(config.parameterBaseHash, config.manifestHash, config.numberOfGuardians, config.quorum),
            config.electionBaseHash)
    }

    @Test
    fun readElectionInit() {
        val group = productionGroup()
        val consumerIn = ConsumerJsonR(topdir, group)

        val initResult = consumerIn.readElectionInitialized()
        assertTrue(initResult is Ok)
        println("${initResult.unwrap().show()}")

        val init = initResult.unwrap()
        val computed = electionExtendedHash(init.config.electionBaseHash, init.jointPublicKey)
        println("He = ${init.extendedBaseHash} should be = ${computed}")
        assertEquals(computed, init.extendedBaseHash)
    }

}