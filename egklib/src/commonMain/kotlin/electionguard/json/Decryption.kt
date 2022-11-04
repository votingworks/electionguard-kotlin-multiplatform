package electionguard.json

import electionguard.core.*
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.ChallengeResponse
import electionguard.decrypt.PartialDecryption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetMissingRequestJson(
    val lagrange_coeff: ElementModQJson,
    val missing: List<String>
    )

data class SetMissingRequest(
    val lagrangeCoeff: ElementModQ,
    val missing: List<String>
)

fun SetMissingRequest.publish() = SetMissingRequestJson(
    this.lagrangeCoeff.publishModQ(),
    this.missing
)

fun GroupContext.importSetMissingRequest(request: SetMissingRequestJson) = SetMissingRequest(
        this.importModQ(request.lagrange_coeff)!!,
        request.missing
    )

///////////////////////////////////////////

@Serializable
@SerialName("DecryptRequest")
data class DecryptRequestJson(
    val texts: List<ElementModPJson>
)

data class DecryptRequest(
    val texts: List<ElementModP>
)

fun DecryptRequest.publish() = DecryptRequestJson(
    this.texts.map { it.publishModP() }
)

fun GroupContext.importDecryptRequest(request: DecryptRequestJson) = DecryptRequest(
        request.texts.map { this.importModP(it)!! },
    )

///////////////////////////////////////////

@Serializable
@SerialName("DecryptResponse")
data class DecryptResponseJson(
    val shares: List<PartialDecryptionJson>
)

data class DecryptResponse(
    val shares: List<PartialDecryption>
)

fun DecryptResponse.publish() = DecryptResponseJson(
    this.shares.map { it.publish() }
)

fun GroupContext.importDecryptResponse(request: DecryptResponseJson) = DecryptResponse(
        request.shares.map { this.importPartialDecryption(it) },
    )

///////////////////////////////////////////

@Serializable
@SerialName("PartialDecryption")
data class PartialDecryptionJson(
    val guardian_id: String,
    val mbari: ElementModPJson, // ??
    val u: ElementModQJson,
    val a: ElementModPJson,
    val b: ElementModPJson,
)

fun PartialDecryption.publish() = PartialDecryptionJson(
    this.guardianId,
    this.mbari.publishModP(),
    this.u.publishModQ(),
    this.a.publishModP(),
    this.b.publishModP(),
)

fun GroupContext.importPartialDecryption(share: PartialDecryptionJson) = PartialDecryption(
        share.guardian_id,
        this.importModP(share.mbari)!!,
        this.importModQ(share.u)!!,
        this.importModP(share.a)!!,
        this.importModP(share.b)!!,
    )

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeRequests")
data class ChallengeRequestsJson(
    val challenges: List<ChallengeRequestJson>
)

data class ChallengeRequests(
    val challenges: List<ChallengeRequest>
)

fun ChallengeRequests.publish() = ChallengeRequestsJson(
    this.challenges.map { it.publish() }
)

fun GroupContext.importChallengeRequests(request: ChallengeRequestsJson) = ChallengeRequests(
    request.challenges.map { this.importChallengeRequest(it) },
)

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeRequest")
data class ChallengeRequestJson(
    val id: String,
    val challenge: ElementModQJson,
    val nonce: ElementModQJson,
)

/** Publishes a [ChallengeRequest] to its external, serializable form. */
fun ChallengeRequest.publish() = ChallengeRequestJson(
    this.id,
    this.challenge.publishModQ(),
    this.nonce.publishModQ(),
)

/** Imports from a published [DecryptResponse]. Returns `null` if it's malformed. */
fun GroupContext.importChallengeRequest(request: ChallengeRequestJson) = ChallengeRequest(
        request.id,
        this.importModQ(request.challenge)!!,
        this.importModQ(request.nonce)!!,
    )

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeResponses")
data class ChallengeResponsesJson(
    val responses: List<ChallengeResponseJson>
)

data class ChallengeResponses(
    val responses: List<ChallengeResponse>
)

fun ChallengeResponses.publish() = ChallengeResponsesJson(
    this.responses.map { it.publish() }
)

fun GroupContext.importChallengeResponses(org: ChallengeResponsesJson) = ChallengeResponses(
    org.responses.map { this.importChallengeResponse(it) },
)

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeResponse")
data class ChallengeResponseJson(
    val id: String,
    val response: ElementModQJson,
)

fun ChallengeResponse.publish() = ChallengeResponseJson(
    this.id,
    this.response.publishModQ(),
)

fun GroupContext.importChallengeResponse(request: ChallengeResponseJson) = ChallengeResponse(
        request.id,
        this.importModQ(request.response)!!,
    )