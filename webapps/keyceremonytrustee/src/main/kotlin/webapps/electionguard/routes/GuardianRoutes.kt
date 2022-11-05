package webapps.electionguard.routes

import webapps.electionguard.groupContext
import webapps.electionguard.models.*
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.json.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.guardianRouting() {
    route("/guardian") {
        get {
            if (guardians.isNotEmpty()) {
                call.respond(guardians)
            } else {
                call.respondText("No guardians found", status = HttpStatusCode.OK)
            }
        }

        post {
            val rguardian = call.receive<RemoteGuardianJson>()
            guardians.add(rguardian)
            call.respondText("RemoteGuardian ${rguardian.id} stored correctly", status = HttpStatusCode.Created)
        }

        get("{id?}/sendPublicKeys") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                guardians.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No guardian with id $id",
                    status = HttpStatusCode.NotFound
                )
            val result = rguardian.sendPublicKeys()
            if (result is Ok) {
                val pk = result.unwrap()
                call.respond(pk.publish())
            } else {
                call.respondText(
                    "RemoteGuardian ${rguardian.id} sendPublicKeys failed ${result.unwrapError()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        post("{id?}/receivePublicKeys") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                guardians.find { it.xCoordinate == id.toInt() } ?: return@post call.respondText(
                    "No guardian with id $id",
                    status = HttpStatusCode.NotFound
                )
            val publicKeysJson = call.receive<PublicKeysJson>()
            val publicKeys = groupContext.importPublicKeys(publicKeysJson)
                ?: return@post call.respondText(
                    "Bad Public Keys",
                    status = HttpStatusCode.BadRequest
                )
            val result = rguardian.receivePublicKeys(publicKeys)
            if (result is Ok) {
                call.respondText("RemoteGuardian ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} correctly", status = HttpStatusCode.OK)
            } else {
                call.respondText(
                    "RemoteGuardian ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} failed ${result.unwrapError()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        get("{id?}/{from?}/sendSecretKeyShare") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                guardians.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No guardian with id $id",
                    status = HttpStatusCode.NotFound
                )
            val from = call.parameters["from"] ?: return@get call.respondText(
                "Missing from id",
                status = HttpStatusCode.BadRequest
            )
            val result = rguardian.sendSecretKeyShare(from)
            if (result is Ok) {
                call.respond(result.unwrap().publish())
            } else {
                call.respondText(
                    "RemoteGuardian ${rguardian.id} sendSecretKeyShare from ${from} failed ${result.unwrapError()}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        post("{id?}/receiveSecretKeyShare") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                guardians.find { it.xCoordinate == id.toInt() } ?: return@post call.respondText(
                    "No guardian with id $id",
                    status = HttpStatusCode.NotFound
                )
            val secretShare = call.receive<SecretKeyShareJson>()
            val result = rguardian.receiveSecretKeyShare(groupContext.importSecretKeyShare(secretShare)!!)
            if (result is Ok) {
                call.respondText("RemoteGuardian ${rguardian.id} receiveSecretKeyShare correctly", status = HttpStatusCode.OK)
            } else {
                call.respondText(
                    "RemoteGuardian ${rguardian.id} receiveSecretKeyShare failed ${result.unwrapError()}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        get("{id?}/saveState") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                guardians.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No guardian with id $id",
                    status = HttpStatusCode.NotFound
                )
            val result = rguardian.saveState()
            if (result is Ok) {
                call.respondText(
                    "RemoteGuardian ${rguardian.id} saveState succeeded",
                    status = HttpStatusCode.OK
                )
            } else {
                call.respondText(
                    "RemoteGuardian ${rguardian.id} saveState failed ${result.unwrapError()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}