package webapps.electionguard.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import webapps.electionguard.routes.guardianRouting

fun Application.configureRouting() {

    routing {
        guardianRouting()
    }
}
