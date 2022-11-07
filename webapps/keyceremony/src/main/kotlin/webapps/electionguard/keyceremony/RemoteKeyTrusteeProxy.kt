package webapps.electionguard.keyceremony

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import electionguard.core.ElementModP
import electionguard.core.SchnorrProof
import electionguard.json.*
import electionguard.keyceremony.KeyCeremonyTrusteeIF
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.SecretKeyShare

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import webapps.electionguard.groupContext

class RemoteKeyTrusteeProxy(
    val client: HttpClient,
    val remoteURL: String,
    val id: String,
    val xcoord: Int,
    val quorum: Int,
) : KeyCeremonyTrusteeIF {
    var publicKeys : PublicKeys? = null

    init {
        runBlocking {
            val url = "$remoteURL/ktrustee"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(
                    """{
                      "id": "$id",
                      "xCoordinate": $xcoord,
                      "quorum": $quorum
                    }"""
                )
            }
            println("response.status for $id = ${response.status}")
        }
    }

    override fun publicKeys(): Result<PublicKeys, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/publicKeys"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
            }
            val publicKeysJson: PublicKeysJson = response.body()
            publicKeys = groupContext.importPublicKeys(publicKeysJson)
            println("$id publicKeys = ${response.status}")
            if (publicKeys == null) Err("failed") else Ok(publicKeys!!)
        }
    }

    override fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/receivePublicKeys"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(publicKeys.publish())
            }
            println("$id receivePublicKeys for ${publicKeys.guardianId} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun secretKeyShareFor(otherGuardian: String): Result<SecretKeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/$otherGuardian/secretKeyShareFor"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
            }
            val secretKeyShareJson: SecretKeyShareJson = response.body()
            val secretKeyShare: SecretKeyShare? = groupContext.importSecretKeyShare(secretKeyShareJson)
            println("$id secretKeyShareFor ${secretKeyShare?.designatedGuardianId} = ${response.status}")
            if (secretKeyShare == null) Err("SecretKeyShare") else Ok(secretKeyShare)
        }
    }

    override fun receiveSecretKeyShare(share: SecretKeyShare): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/receiveSecretKeyShare"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(share.publish())
            }
            println("$id receiveSecretKeyShare from ${share.generatingGuardianId} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    fun saveState(): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/saveState"
            val response: HttpResponse = client.get(url)
            println("$id saveState from = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun xCoordinate(): Int {
        return xcoord
    }

    override fun coefficientCommitments(): List<ElementModP> {
        return publicKeys?.coefficientCommitments() ?: throw IllegalStateException()
    }

    override fun coefficientProofs(): List<SchnorrProof> {
        return publicKeys?.coefficientProofs ?: throw IllegalStateException()
    }

    override fun electionPublicKey(): ElementModP {
        return publicKeys?.publicKey()?.key ?: throw IllegalStateException()
    }

    override fun id(): String {
        return id
    }
}