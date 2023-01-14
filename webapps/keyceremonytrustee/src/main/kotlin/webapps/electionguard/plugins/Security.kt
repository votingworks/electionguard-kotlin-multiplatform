package webapps.electionguard.plugins

import io.ktor.server.auth.*
import io.ktor.server.application.*
import webapps.electionguard.credentialsPassword

fun Application.configureSecurity() {

    authentication {
        // https://ktor.io/docs/basic.html
        basic("auth-basic") {
            realm = "Access to the '/' path"
            validate { credentials ->
                if (credentials.name == "electionguard" && credentials.password == credentialsPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

}
