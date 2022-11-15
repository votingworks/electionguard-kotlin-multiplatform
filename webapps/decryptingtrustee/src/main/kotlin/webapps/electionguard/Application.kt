package webapps.electionguard

import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import io.ktor.server.application.*
import webapps.electionguard.plugins.*

val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)
const val trusteeDir = "/home/snake/dev/github/electionguard-kotlin-multiplatform/src/commonTest/data/runWorkflowSomeAvailable/private_data/trustees"

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureAdministration()
    configureSerialization()
    configureRouting()
}
