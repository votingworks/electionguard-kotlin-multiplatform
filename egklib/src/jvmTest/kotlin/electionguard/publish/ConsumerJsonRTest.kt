package electionguard.publish

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.productionGroup
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlin.test.Test

class ConsumerJsonRTest {
    val topdir = "/home/stormy/dev/github/electionguard-rust/working/"

    @Test
    fun readElectionConfig() {
        val group = productionGroup()
        val consumerIn = ConsumerJsonR(topdir, group)

        val configResult = consumerIn.readElectionConfig()
        assertTrue(configResult is Ok)
        println("${configResult.unwrap().show()}")
    }

    @Test
    fun readElectionInit() {
        val group = productionGroup()
        val consumerIn = ConsumerJsonR(topdir, group)

        val initResult = consumerIn.readElectionInitialized()
        assertTrue(initResult is Ok)
        println("${initResult.unwrap().show()}")
    }

}