package webapps.electionguard.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import webapps.electionguard.routes.trusteeRouting

fun Application.configureRouting() {

    routing {
        trusteeRouting()
    }
}
