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

fun Route.trusteeRouting() {
    route("/ktrustee") {
        get {
            if (remoteKeyTrustees.isNotEmpty()) {
                call.respond(remoteKeyTrustees)
            } else {
                call.respondText("No guardians found", status = HttpStatusCode.OK)
            }
        }

        post {
            val rguardian = call.receive<RemoteKeyTrustee>()
            remoteKeyTrustees.add(rguardian)
            call.respondText("RemoteKeyTrustee ${rguardian.id} stored correctly", status = HttpStatusCode.Created)
        }

        get("{id?}/publicKeys") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No RemoteKeyTrustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val result = rguardian.publicKeys()
            if (result is Ok) {
                val pk = result.unwrap()
                call.respond(pk.publish())
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} publicKeys failed ${result.unwrapError()}",
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
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@post call.respondText(
                    "No RemoteKeyTrustee with id $id",
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
                call.respondText("RemoteKeyTrustee ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} correctly", status = HttpStatusCode.OK)
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} receivePublicKeys from ${publicKeys.guardianId} failed ${result.unwrapError()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        get("{id?}/{from?}/secretKeyShareFor") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No RemoteKeyTrustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val from = call.parameters["from"] ?: return@get call.respondText(
                "Missing from id",
                status = HttpStatusCode.BadRequest
            )
            val result = rguardian.secretKeyShareFor(from)
            if (result is Ok) {
                call.respond(result.unwrap().publish())
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} sendSecretKeyShare from ${from} failed ${result.unwrapError()}",
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
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@post call.respondText(
                    "No RemoteKeyTrustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val secretShare = call.receive<EncryptedKeyShareJson>()
            val result = rguardian.receiveSecretKeyShare(groupContext.importSecretKeyShare(secretShare))
            if (result is Ok) {
                call.respondText("RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare correctly", status = HttpStatusCode.OK)
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare failed ${result.unwrapError()}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }


        get("{id?}/{from?}/keyShareFor") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No RemoteKeyTrustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val from = call.parameters["from"] ?: return@get call.respondText(
                "Missing from id",
                status = HttpStatusCode.BadRequest
            )
            val result = rguardian.keyShareFor(from)
            if (result is Ok) {
                call.respond(result.unwrap().publish())
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} sendSecretKeyShare from ${from} failed ${result.unwrapError()}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        post("{id?}/receiveKeyShare") {
            val id = call.parameters["id"] ?: return@post call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val rguardian =
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@post call.respondText(
                    "No RemoteKeyTrustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val secretShare = call.receive<KeyShareJson>()
            val result = rguardian.receiveKeyShare(groupContext.importKeyShare(secretShare))
            if (result is Ok) {
                call.respondText("RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare correctly", status = HttpStatusCode.OK)
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} receiveSecretKeyShare failed ${result.unwrapError()}",
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
                remoteKeyTrustees.find { it.xCoordinate == id.toInt() } ?: return@get call.respondText(
                    "No RemoteKeyTrustee with id $id",
                    status = HttpStatusCode.NotFound
                )
            val result = rguardian.saveState()
            if (result is Ok) {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} saveState succeeded",
                    status = HttpStatusCode.OK
                )
            } else {
                call.respondText(
                    "RemoteKeyTrustee ${rguardian.id} saveState failed ${result.unwrapError()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}