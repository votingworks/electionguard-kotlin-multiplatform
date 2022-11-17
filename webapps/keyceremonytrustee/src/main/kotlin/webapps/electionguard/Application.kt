package webapps.electionguard

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import webapps.electionguard.plugins.*
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import io.ktor.server.engine.*
import io.ktor.server.request.*
import org.slf4j.event.Level

var trusteeDir = ""

val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)

fun main(args: Array<String>) {
    val argumentsPairs = args.mapNotNull { it.splitPair('=') }.toMap()
    trusteeDir = argumentsPairs["-trusteeDir"] ?: throw RuntimeException("missing argument -trusteeDir=trustee output directory")
    println("trusteeDir = '$trusteeDir'")
    io.ktor.server.netty.EngineMain.main(args)
}

internal fun String.splitPair(ch: Char): Pair<String, String>? = indexOf(ch).let { idx ->
    when (idx) {
        -1 -> null
        else -> Pair(take(idx), drop(idx + 1))
    }
}

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
