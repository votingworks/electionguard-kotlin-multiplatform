package webapps.electionguard

import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.*

// https://ktor.io/docs/ssl.html#self-signed
fun main(args: Array<String>) {
    val parser = ArgParser("MakeKeyStore")
    val keystorePassword by parser.option(
        ArgType.String,
        shortName = "kpwd",
        description = "password for the entire keystore"
    ).required()
    val electionguardPassword by parser.option(
        ArgType.String,
        shortName = "epwd",
        description = "password for the electionguard certificate entry"
    ).required()
    val sslKeyStore by parser.option(
        ArgType.String,
        shortName = "keystore",
        description = "write the keystore file to this path, default webapps/keystore.jks"
    )
    parser.parse(args)
    println("MakeKeyStore\n keystorePassword = '$keystorePassword' electionguardPassword = '$electionguardPassword'")

    val keyStorePath = sslKeyStore ?: "webapps/keystore.jks"
    val keyStoreFile = File(keyStorePath)

    // if you use default hash or keySize, get warning from "keytool -list -keystore keystore.jks"
    // Also get warning "The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12..."
    // Looks like we would have to mess with KeyStoreBuilder.build(), line 160, to fix that warning.
    //
    val keyStore = buildKeyStore {
        certificate("electionguard") {
            hash = HashAlgorithm.SHA256
            keySizeInBits = 3072
            password = electionguardPassword
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, keystorePassword)
    println(" write to path = '$keyStorePath'")
}