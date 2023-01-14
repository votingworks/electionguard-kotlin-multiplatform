package webapps.electionguard

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import webapps.electionguard.plugins.*
import electionguard.core.PowRadixOption
import electionguard.core.ProductionMode
import electionguard.core.productionGroup
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore

var trusteeDir = ""
var credentialsPassword = ""
val groupContext = productionGroup(PowRadixOption.HIGH_MEMORY_USE, ProductionMode.Mode4096)

fun main(args: Array<String>) {
    val parser = ArgParser("KeyCeremonyRemoteTrustee")
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
    val trustees by parser.option(
        ArgType.String,
        shortName = "trusteeDir",
        description = "trustee output directory"
    ).required()
    val serverPort by parser.option(
        ArgType.Int,
        shortName = "port",
        description = "listen on this port, default = 11183"
    )
    parser.parse(args)

    val sport = serverPort ?: 11183
    trusteeDir = trustees
    credentialsPassword = electionguardPassword

    println("KeyCeremonyRemoteTrustee\n" +
            "  sslKeyStore = '$sslKeyStore'\n" +
            "  serverPort = '$sport'\n" +
            "  trusteeDir = '$trusteeDir'\n" +
            " ")

    // println("trusteeDir = '$trusteeDir'")
    // io.ktor.server.netty.EngineMain.main(args)

    val keyStoreFile = File(sslKeyStore)
    val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()) // LOOK assumes jks
    keyStore.load(FileInputStream(keyStoreFile), keystorePassword.toCharArray())

    val environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        sslConnector(
            keyStore = keyStore,
            keyAlias = "electionguard",
            keyStorePassword = { keystorePassword.toCharArray() },
            privateKeyPassword = { electionguardPassword.toCharArray() }) {
            port = sport
            keyStorePath = keyStoreFile
        }
        module(Application::module)
    }

    embeddedServer(Netty, environment).start(wait = true)
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            "Status: $status, HTTP method: $httpMethod, Path: $path"
        }
    }
    configureSecurity()
    configureSerialization()
    configureAdministration()
    configureRouting()
}
