package webapps.electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.decrypt.ChallengeRequest
import electionguard.decrypt.ChallengeResponse
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.decrypt.PartialDecryption
import electionguard.json.*

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import webapps.electionguard.groupContext

class DecryptingTrusteeProxy(
    val client: HttpClient,
    val remoteURL: String,
    val id: String,
    val xcoord: Int,
    val publicKey: ElementModP,
) : DecryptingTrusteeIF {

    init {
        runBlocking {
            val url = "$remoteURL/trustee"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(
                    """{
                      "guardian_id": "$id"
                    }"""
                )
            }
            println("DecryptingTrusteeProxy create $id = ${response.status}")
        }
    }

    override fun setMissing(group: GroupContext, lagrangeCoeff: ElementModQ, missingGuardians: List<String>): Boolean {
        return runBlocking {
            val url = "$remoteURL/trustee/$xcoord/setMissing"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(SetMissingRequest(lagrangeCoeff, missingGuardians).publish())
            }
            println("DecryptingTrusteeProxy setMissing $id = ${response.status}")
            (response.status == HttpStatusCode.OK)
        }
    }

    override fun decrypt(
        group: GroupContext,
        texts: List<ElementModP>,
        nonce: ElementModQ?, // LOOK do we need this?
    ): List<PartialDecryption> {
        return runBlocking {
            val url = "$remoteURL/trustee/$xcoord/decrypt"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(DecryptRequest(texts).publish())
            }
            val decryptResponseJson: DecryptResponseJson = response.body()
            val decryptResponse = groupContext.importDecryptResponse(decryptResponseJson)
            println("DecryptingTrusteeProxy decrypt $id = ${response.status}")
            decryptResponse.shares
        }
    }

    override fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse> {
        return runBlocking {
            val url = "$remoteURL/trustee/$xcoord/challenge"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(ChallengeRequests(challenges).publish())
            }
            println("DecryptingTrusteeProxy challenge $id = ${response.status}")
            val challengeResponsesJson: ChallengeResponsesJson = response.body()
            val challengeResponses = groupContext.importChallengeRequests(challengeResponsesJson) // importChallengeResponses
            challengeResponses.responses
        }
    }

    override fun xCoordinate(): Int {
        return xcoord
    }

    override fun electionPublicKey(): ElementModP {
        return publicKey
    }

    override fun id(): String {
        return id
    }
}