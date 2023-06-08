package webapps.electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.protocolVersion
import electionguard.core.GroupContext
import electionguard.core.getSystemTimeInMillis
import electionguard.core.productionGroup
import electionguard.keyceremony.keyCeremonyExchange
import electionguard.publish.makeConsumer
import electionguard.publish.makePublisher
import electionguard.publish.readManifest
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

var keystore = ""
var ksPassword = ""
var egPassword = ""

/**
 * Run Remote KeyCeremony CLI.
 * The keyceremonytrustee webapp must already be running.
 */
fun main(args: Array<String>) {
    val parser = ArgParser("RunRemoteKeyCeremony")
    val inputDir by parser.option(
        ArgType.String,
        shortName = "in",
        description = "Directory containing input ElectionConfig record"
    )
    val electionManifest by parser.option(
        ArgType.String,
        shortName = "manifest",
        description = "Manifest file or directory (json or protobuf)"
    )
    val nguardians by parser.option(
        ArgType.Int,
        shortName = "nguardians",
        description = "number of guardians"
    )
    val quorum by parser.option(
        ArgType.Int,
        shortName = "quorum",
        description = "quorum size"
    )
    val outputDir by parser.option(
        ArgType.String,
        shortName = "out",
        description = "Directory to write output ElectionInitialized record"
    ).required()
    val remoteUrl by parser.option(
        ArgType.String,
        shortName = "remoteUrl",
        description = "URL of keyceremony trustee webapp "
    ).required()
    val sslKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "file path of the keystore file"
    ).required()
    val keystorePassword by parser.option(
        ArgType.String,
        shortName = "kpwd",
        description = "password for the entire keystore"
    ).required()
    val electionguardPassword by parser.option(
        ArgType.String,
        shortName = "epwd",
        description = "password for the electionguard entry"
    ).required()
    val createdBy by parser.option(
        ArgType.String,
        shortName = "createdBy",
        description = "who created for ElectionInitialized metadata"
    )
    val electionDate by parser.option(
        ArgType.String,
        shortName = "electionDate",
        description = "election date"
    )
    val info by parser.option(
        ArgType.String,
        shortName = "info",
        description = "jurisdictional information"
    )
    parser.parse(args)
    keystore = sslKeyStore
    ksPassword = keystorePassword
    egPassword = electionguardPassword

    val group = productionGroup()
    var createdFrom : String

    val config: ElectionConfig = if (electionManifest != null && nguardians != null && quorum != null) {
        val manifest = readManifest(electionManifest!!, group)
        createdFrom = electionManifest!!
        println(
            "RunRemoteKeyCeremony\n" +
                    "  electionManifest = '$electionManifest'\n" +
                    "  nguardians = $nguardians quorum = $quorum\n" +
                    "  outputDir = '$outputDir'\n" +
                    "  sslKeyStore = '$sslKeyStore'\n"
        )
        ElectionConfig(protocolVersion, group.constants, ByteArray(0), manifest.unwrap(), nguardians!!, quorum!!,
            electionDate ?: "N/A",
            info ?: "N/A",
            mapOf(
                Pair("CreatedBy", createdBy ?: "RunRemoteKeyCeremony"),
                Pair("CreatedFromElectionManifest", electionManifest!!),
            ))
    } else {
        val consumerIn = makeConsumer(inputDir!!, group)
        createdFrom = inputDir!!
        println(
            "RunRemoteKeyCeremony\n" +
                    "  inputDir = '$inputDir'\n" +
                    "  outputDir = '$outputDir'\n" +
                    "  sslKeyStore = '$sslKeyStore'\n"
        )
        consumerIn.readElectionConfig().getOrThrow { IllegalStateException(it) }
    }

    runKeyCeremony(group, remoteUrl, createdFrom, config, outputDir, createdBy)
}

fun runKeyCeremony(
    group: GroupContext,
    remoteUrl: String,
    createdFrom: String,
    config: ElectionConfig,
    outputDir: String,
    createdBy: String?
): Boolean {
    val starting = getSystemTimeInMillis()

    val client = HttpClient(Java) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            // level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json()
        }
        engine {
            config {
                sslContext(SslSettings.getSslContext())
            }
        }
    }

    val trustees: List<RemoteKeyTrusteeProxy> = List(config.numberOfGuardians) {
        val seq = it + 1
        RemoteKeyTrusteeProxy(group, client, remoteUrl, "trustee$seq", seq, config.quorum, egPassword)
    }

    val exchangeResult = keyCeremonyExchange(trustees)
    if (exchangeResult is Err) {
        println(exchangeResult.error)
        return false
    }

    val keyCeremonyResults = exchangeResult.unwrap()
    val electionInitialized = keyCeremonyResults.makeElectionInitialized(
        config,
        mapOf(
            Pair("CreatedBy", createdBy ?: "RunRemoteKeyCeremony"),
            Pair("CreatedFrom", createdFrom),
        )
    )

    val publisher = makePublisher(outputDir)
    publisher.writeElectionInitialized(electionInitialized)

    // tell the trustees to save their state in some private place.
    trustees.forEach { it.saveState() }
    client.close()

    val took = getSystemTimeInMillis() - starting
    println("RunTrustedKeyCeremony took $took millisecs")

    return true
}

private object SslSettings {
    fun getKeyStore(): KeyStore {
        val keyStoreFile = FileInputStream(keystore)
        val keyStorePassword = ksPassword.toCharArray()
        val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(keyStoreFile, keyStorePassword)
        return keyStore
    }

    fun getTrustManagerFactory(): TrustManagerFactory? {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(getKeyStore())
        return trustManagerFactory
    }

    fun getSslContext(): SSLContext? {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, getTrustManagerFactory()?.trustManagers, null)
        return sslContext
    }

    fun getTrustManager(): X509TrustManager {
        return getTrustManagerFactory()?.trustManagers?.first { it is X509TrustManager } as X509TrustManager
    }
}
