package webapps.electionguard.keyceremony

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.core.ElementModP
import electionguard.core.SchnorrProof
import electionguard.json.*
import electionguard.keyceremony.KeyCeremonyTrusteeIF
import electionguard.keyceremony.KeyShare
import electionguard.keyceremony.PublicKeys
import electionguard.keyceremony.EncryptedKeyShare

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
        if (this.publicKeys != null) {
            return Ok(this.publicKeys!!)
        }
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/publicKeys"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
            }
            val publicKeysJson: PublicKeysJson = response.body()
            val publicKeyResult = publicKeysJson.import(groupContext)
            if (publicKeyResult is Ok) {
                publicKeys = publicKeyResult.unwrap()
            } else {
                println("$id publicKeys = ${response.status} err = ${publicKeyResult.unwrapError()}")
            }
            publicKeyResult
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

    override fun encryptedKeyShareFor(otherGuardian: String): Result<EncryptedKeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/$otherGuardian/encryptedKeyShareFor"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
            }
            val encryptedKeyShareJson: EncryptedKeyShareJson = response.body()
            val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(groupContext)
            println("$id encryptedKeyShareFor ${encryptedKeyShare?.availableGuardianId} = ${response.status}")
            if (encryptedKeyShare == null) Err("EncryptedKeyShare") else Ok(encryptedKeyShare)
        }
    }

    override fun receiveEncryptedKeyShare(share: EncryptedKeyShare?): Result<Boolean, String> {
        if (share == null) {
            return Err("$id receiveEncryptedKeyShare sent null share")
        }
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/receiveEncryptedKeyShare"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(share.publish())
            }
            println("$id receiveEncryptedKeyShare from ${share.missingGuardianId} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun keyShareFor(otherGuardian: String): Result<KeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/$otherGuardian/keyShareFor"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
            }
            val keyShareJson: KeyShareJson = response.body()
            val keyShare: KeyShare? = keyShareJson.import(groupContext)
            println("$id secretKeyShareFor ${keyShare?.availableGuardianId} = ${response.status}")
            if (keyShare == null) Err("SecretKeyShare") else Ok(keyShare)
        }
    }

    override fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/receiveKeyShare"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(keyShare.publish())
            }
            println("$id receiveKeyShare from ${keyShare.missingGuardianId} = ${response.status}")
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
        publicKeys()
        return publicKeys?.coefficientCommitments() ?: throw IllegalStateException()
    }

    override fun coefficientProofs(): List<SchnorrProof> {
        publicKeys()
        return publicKeys?.coefficientProofs ?: throw IllegalStateException()
    }

    override fun electionPublicKey(): ElementModP {
        publicKeys()
        return publicKeys?.publicKey()?.key ?: throw IllegalStateException()
    }

    override fun id(): String {
        return id
    }
}