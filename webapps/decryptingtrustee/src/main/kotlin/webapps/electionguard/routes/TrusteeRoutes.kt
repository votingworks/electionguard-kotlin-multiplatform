package webapps.electionguard.routes

import webapps.electionguard.groupContext
import webapps.electionguard.models.*
import electionguard.json.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.trusteeRouting() {
    route("/trustee") {
        get {
            if (trustees.isNotEmpty()) {
                call.respond(trustees)
            } else {
                call.respondText("No trustees found", status = HttpStatusCode.OK)
            }
        }

        post {
            val trustee = call.receive<RemoteDecryptingTrusteeJson>()
            trustees.add(trustee)
            println("RemoteDecryptingTrustee ${trustee.id()} created")
            call.respondText("RemoteDecryptingTrustee ${trustee.id()} stored correctly", status = HttpStatusCode.Created)
        }

        post("{id?}/setMissing") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val trustee =
                trustees.find { it.xCoordinate() == id.toInt() } ?: return@post call.respondText(
                    "No trustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val missingReqJson = call.receive<SetMissingRequestJson>()
            val missingReq = groupContext.importDecryptRequest(missingReqJson) // importSetMissingRequest
            val ok = trustee.setMissing(missingReq.lagrangeCoeff, missingReq.missing)
            println("RemoteDecryptingTrustee ${trustee.id()} setMissing $ok")
            call.respondText("RemoteDecryptingTrustee $id setMissing $ok",
                status = if (ok) HttpStatusCode.OK else HttpStatusCode.InternalServerError)
        }

        post("{id?}/decrypt") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val trustee =
                trustees.find { it.xCoordinate() == id.toInt() } ?: return@post call.respondText(
                    "No trustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val decryptRequestJson = call.receive<DecryptRequestJson>()
            val decryptRequest = groupContext.importDecryptRequest(decryptRequestJson)
            val decryptions = trustee.decrypt(decryptRequest.texts)
            val response = DecryptResponse(decryptions)
            println("RemoteDecryptingTrustee ${trustee.id()} decrypt")
            call.respond(response.publish())
        }

        post("{id?}/challenge") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val trustee =
                trustees.find { it.xCoordinate() == id.toInt() } ?: return@post call.respondText(
                    "No trustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val requestsJson = call.receive<ChallengeRequestsJson>()
            val requests = groupContext.importChallengeRequests(requestsJson)
            val responses = trustee.challenge(requests.challenges)
            val response = ChallengeResponses(responses)
            println("RemoteDecryptingTrustee ${trustee.id()} challenge")
            call.respond(response.publish())
        }

    }

}