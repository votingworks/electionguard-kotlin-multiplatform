package webapps.electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.publish.Consumer
import electionguard.publish.Publisher
import electionguard.publish.PublisherMode

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.junit.Test

@Test
fun testRemoteKeyCeremonyMain() {
    val inputDir = "/home/snake/tmp/electionguard/kickstart/start"
    val trusteeDir = "/home/snake/tmp/electionguard/MockRemoteKeyCeremonyTest/private_data/trustees"
    val outputDir = "/home/snake/tmp/electionguard/MockRemoteKeyCeremonyTest"
    val remoteUrl = "http://0.0.0.0:11180"

    val group = productionGroup()
    runKeyCeremony(group, remoteUrl, inputDir, outputDir, trusteeDir, "mockRemoteKeyCeremonyTest")
}

fun runKeyCeremony(
    group: GroupContext,
    remoteUrl: String,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    createdBy: String?
): Boolean {
    val starting = getSystemTimeInMillis()

    val consumerIn = Consumer(configDir, group)
    val config: ElectionConfig = consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }

    val client = HttpClient(Java) {
        install(ContentNegotiation) {
            json()
        }
    }

    val trustees: List<RemoteKeyTrusteeProxy> = List(config.numberOfGuardians) {
        val seq = it + 1
        RemoteKeyTrusteeProxy(client, remoteUrl,"trustee$seq", seq, config.quorum)
    }

    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        println(exchangeResult.error)
        return false
    }

    val keyCeremonyResults = exchangeResult.unwrap()
    val electionInitialized = keyCeremonyResults.makeElectionInitialized(
        config,
        mapOf(
            Pair("CreatedBy", createdBy ?: "runKeyCeremony"),
            Pair("CreatedFromDir", configDir),
        )
    )

    val publisher = Publisher(outputDir, PublisherMode.createIfMissing)
    publisher.writeElectionInitialized(electionInitialized)

    // tell the trustees to save their sate in some private place.
    trustees.forEach { it.saveState() }

    val took = getSystemTimeInMillis() - starting
    println("RunTrustedKeyCeremony took $took millisecs")

    return true
}
