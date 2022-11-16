package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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

fun GroupContext.importSetMissingRequest(json: SetMissingRequestJson): SetMissingRequest? {
    val coeff = this.importModQ(json.lagrange_coeff)
    return if (coeff == null) null else
        SetMissingRequest(
            coeff,
            json.missing
        )
}

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

fun GroupContext.importDecryptRequest(json: DecryptRequestJson): Result<DecryptRequest, String> {
    val texts = json.texts.map { this.importModP(it) }
    val allgood = texts.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(DecryptRequest(texts.map { it!! }))
    else Err("importModP failed")
}

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

fun GroupContext.importDecryptResponse(json: DecryptResponseJson): Result<DecryptResponse, String> {
    val shares = json.shares.map { this.importPartialDecryption(it) }
    val allgood = shares.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(DecryptResponse(shares.map { it!! }))
    else Err("importPartialDecryption failed")
}

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

fun GroupContext.importPartialDecryption(json: PartialDecryptionJson): PartialDecryption? {
    val mbari = this.importModP(json.mbari)
    val u = this.importModQ(json.u)
    val a = this.importModP(json.a)
    val b = this.importModP(json.b)
    return if (mbari == null || u == null || a == null || b == null) null
    else PartialDecryption(json.guardian_id, mbari, u, a, b)
}

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

fun GroupContext.importChallengeRequests(json: ChallengeRequestsJson) : Result<ChallengeRequests, String> {
    val challenges = json.challenges.map { this.importChallengeRequest(it) }
    val allgood = challenges.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(ChallengeRequests(challenges.map { it!! }))
    else Err("importChallengeRequest failed")
}

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeRequest")
data class ChallengeRequestJson(
    val id: String,
    val challenge: ElementModQJson,
    val nonce: ElementModQJson,
)

fun ChallengeRequest.publish() = ChallengeRequestJson(
    this.id,
    this.challenge.publishModQ(),
    this.nonce.publishModQ(),
)

fun GroupContext.importChallengeRequest(json: ChallengeRequestJson) : ChallengeRequest? {
    val challenge = this.importModQ(json.challenge)
    val nonce = this.importModQ(json.nonce)
    return if (challenge == null || nonce == null) null
    else ChallengeRequest(json.id, challenge, nonce)
}

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

fun GroupContext.importChallengeResponses(json: ChallengeResponsesJson): Result<ChallengeResponses, String> {
    val responses = json.responses.map { this.importChallengeResponse(it) }
    val allgood = responses.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(ChallengeResponses(responses.map { it!! }))
    else Err("importChallengeResponse failed")
}

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

fun GroupContext.importChallengeResponse(json: ChallengeResponseJson): ChallengeResponse? {
    val response = this.importModQ(json.response)
    return if (response == null) null else
        ChallengeResponse(
            json.id,
            response,
        )
}