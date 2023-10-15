package electionguard.json2

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.*
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.ChallengeResponse
import electionguard.decrypt.PartialDecryption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// stuff used by the webapps - easiest to have it here as a common dependency

@Serializable
data class SetMissingRequestJson(
    val lagrange_coeff: ElementModQJson,
    val missing: List<String>
)

data class SetMissingRequest(
    val lagrangeCoeff: ElementModQ,
    val missing: List<String>
)

fun SetMissingRequest.publishJson() = SetMissingRequestJson(
    this.lagrangeCoeff.publishJson(),
    this.missing
)

fun SetMissingRequestJson.import(group: GroupContext): SetMissingRequest? {
    val coeff = this.lagrange_coeff.import(group)
    return if (coeff == null) null else
        SetMissingRequest(
            coeff,
            this.missing
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

fun DecryptRequest.publishJson() = DecryptRequestJson(
    this.texts.map { it.publishJson() }
)

fun DecryptRequestJson.import(group: GroupContext): Result<DecryptRequest, String> {
    val texts = this.texts.map { it.import(group) }
    val allgood = texts.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(DecryptRequest(texts.map { it!! }))
    else Err("importModP error")
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

fun DecryptResponse.publishJson() = DecryptResponseJson(
    this.shares.map { it.publishJson() }
)

fun DecryptResponseJson.import(group: GroupContext): Result<DecryptResponse, String> {
    val shares = this.shares.map { it.import(group) }
    val allgood = shares.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(DecryptResponse(shares.map { it!! }))
    else Err("importPartialDecryption error")
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

fun PartialDecryption.publishJson() = PartialDecryptionJson(
    this.guardianId,
    this.Mi.publishJson(),
    this.u.publishJson(),
    this.a.publishJson(),
    this.b.publishJson(),
)

fun PartialDecryptionJson.import(group: GroupContext): PartialDecryption? {
    val mbari = this.mbari.import(group)
    val u = this.u.import(group)
    val a = this.a.import(group)
    val b = this.b.import(group)
    return if (mbari == null || u == null || a == null || b == null) null
    else PartialDecryption(this.guardian_id, mbari, u, a, b)
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

fun ChallengeRequests.publishJson() = ChallengeRequestsJson(
    this.challenges.map { it.publishJson() }
)

fun ChallengeRequestsJson.import(group: GroupContext) : Result<ChallengeRequests, String> {
    val challenges = this.challenges.map { it.import(group) }
    val allgood = challenges.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(ChallengeRequests(challenges.map { it!! }))
    else Err("importChallengeRequest error")
}

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeRequest")
data class ChallengeRequestJson(
    val id: String,
    val challenge: ElementModQJson,
    val nonce: ElementModQJson,
)

fun ChallengeRequest.publishJson() = ChallengeRequestJson(
    this.id,
    this.challenge.publishJson(),
    this.nonce.publishJson(),
)

fun ChallengeRequestJson.import(group: GroupContext) : ChallengeRequest? {
    val challenge = this.challenge.import(group)
    val nonce = this.nonce.import(group)
    return if (challenge == null || nonce == null) null
    else ChallengeRequest(this.id, challenge, nonce)
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

fun ChallengeResponses.publishJson() = ChallengeResponsesJson(
    this.responses.map { it.publishJson() }
)

fun ChallengeResponsesJson.import(group: GroupContext): Result<ChallengeResponses, String> {
    val responses = this.responses.map { it.import(group) }
    val allgood = responses.map { it != null }.reduce { a, b -> a && b }

    return if (allgood) Ok(ChallengeResponses(responses.map { it!! }))
    else Err("importChallengeResponse error")
}

///////////////////////////////////////////

@Serializable
@SerialName("ChallengeResponse")
data class ChallengeResponseJson(
    val id: String,
    val response: ElementModQJson,
)

fun ChallengeResponse.publishJson() = ChallengeResponseJson(
    this.id,
    this.response.publishJson(),
)

fun ChallengeResponseJson.import(group: GroupContext): ChallengeResponse? {
    val response = this.response.import(group)
    return if (response == null) null else
        ChallengeResponse(
            this.id,
            response,
        )
}