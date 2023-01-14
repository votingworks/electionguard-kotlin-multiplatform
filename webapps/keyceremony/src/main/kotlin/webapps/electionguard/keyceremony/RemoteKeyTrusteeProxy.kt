package webapps.electionguard.keyceremony

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
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

/** Implement KeyCeremonyTrusteeIF by connecting to a keyceremonytrustee webapp. */
class RemoteKeyTrusteeProxy(
    val group : GroupContext,
    val client: HttpClient,
    val remoteURL: String,
    val id: String,
    val xcoord: Int,
    val quorum: Int,
    val certPassword: String,
) : KeyCeremonyTrusteeIF {
    var publicKeys : PublicKeys? = null

    init {
        runBlocking {
            val url = "$remoteURL/ktrustee"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    basicAuth("electionguard", certPassword)
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
                    basicAuth("electionguard", certPassword)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                val publicKeysJson: PublicKeysJson = response.body()
                val publicKeyResult = publicKeysJson.import(group)
                if (publicKeyResult is Ok) {
                    publicKeys = publicKeyResult.unwrap()
                } else {
                    println("$id publicKeys = ${response.status} err = ${publicKeyResult.unwrapError()}")
                }
                publicKeyResult
            }
        }
    }

    override fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/receivePublicKeys"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    basicAuth("electionguard", certPassword)
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
                    basicAuth("electionguard", certPassword)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                val encryptedKeyShareJson: EncryptedKeyShareJson = response.body()
                val encryptedKeyShare: EncryptedKeyShare? = encryptedKeyShareJson.import(group)
                println("$id encryptedKeyShareFor ${encryptedKeyShare?.secretShareFor} = ${response.status}")
                if (encryptedKeyShare == null) Err("EncryptedKeyShare") else Ok(encryptedKeyShare)
            }
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
                    basicAuth("electionguard", certPassword)
                }
                setBody(share.publish())
            }
            println("$id receiveEncryptedKeyShare from ${share.polynomialOwner} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun keyShareFor(otherGuardian: String): Result<KeyShare, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/$otherGuardian/keyShareFor"
            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    basicAuth("electionguard", certPassword)
                }
            }
            if (response.status != HttpStatusCode.OK) {
                println("response.status for $url = ${response.status}")
                Err("$url error = ${response.status}")
            } else {
                val keyShareJson: KeyShareJson = response.body()
                val keyShare: KeyShare? = keyShareJson.import(group)
                println("$id secretKeyShareFor ${keyShare?.secretShareFor} = ${response.status}")
                if (keyShare == null) Err("SecretKeyShare") else Ok(keyShare)
            }
        }
    }

    override fun receiveKeyShare(keyShare: KeyShare): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/receiveKeyShare"
            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.ContentType, "application/json")
                    basicAuth("electionguard", certPassword)
                }
                setBody(keyShare.publish())
            }
            println("$id receiveKeyShare from ${keyShare.polynomialOwner} = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    fun saveState(): Result<Boolean, String> {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/saveState"
            val response: HttpResponse = client.get(url) {
                headers {
                    basicAuth("electionguard", certPassword)
                }
            }
            println("$id saveState from = ${response.status}")
            if (response.status == HttpStatusCode.OK) Ok(true) else Err(response.toString())
        }
    }

    override fun xCoordinate(): Int {
        return xcoord
    }

    override fun coefficientCommitments(): List<ElementModP> {
        publicKeys()
        return publicKeys?.coefficientCommitments() ?: throw IllegalStateException("$id coefficientCommitments failed")
    }

    override fun coefficientProofs(): List<SchnorrProof> {
        publicKeys()
        return publicKeys?.coefficientProofs ?: throw IllegalStateException()
    }

    override fun electionPublicKey(): ElementModP {
        publicKeys()
        return publicKeys?.publicKey()?.key ?: throw IllegalStateException()
    }

    override fun keyShare(): ElementModQ {
        return runBlocking {
            val url = "$remoteURL/ktrustee/$xcoord/keyShare"
            val response: HttpResponse = client.get(url) {
                headers {
                    basicAuth("electionguard", certPassword)
                }
            }
            println("$id keyShare ${xcoord} = ${response.status}")
            val keyShareJson : ElementModQJson = response.body()
            keyShareJson.import(group)!!
        }
    }

    override fun id(): String {
        return id
    }
}