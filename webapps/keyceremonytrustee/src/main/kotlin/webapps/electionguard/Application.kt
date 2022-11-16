package webapps.electionguard

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import webapps.electionguard.plugins.*
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import io.ktor.server.request.*
import org.slf4j.event.Level


// LOOK pass this in on command line
//val trusteeDir = "/home/snake/tmp/electionguard/RunRemoteKeyCeremonyTest/private_data/trustees"
const val trusteeDir = "/home/snake/tmp/electionguard/MockRemoteKeyCeremonyTest/private_data/trustees"

val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            "Status: $status, HTTP method: $httpMethod, Path: $path"
        }
    }
    configureSecurity()
    configureSerialization()
    configureAdministration()
    configureRouting()
}
