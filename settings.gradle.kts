rootProject.name = "electionguard-kotlin-multiplatform"

// eventually this may become canonical source for all versions, including webapps.
// 9/9/2023
val coroutinesVersion = "1.6.4" // "1.7.3" see issue #362
val jupitorVersion = "5.10.0"
val kotlinVersion = "1.9.0"
val kotlinxCliVersion = "0.3.6"
val kotlinxDatetimeVersion = "0.4.1"
val kotlinxSerializationCoreVersion = "1.6.0"
val kotestVersion = "5.7.2"
val ktorVersion = "2.3.4"
val logbackVersion = "1.4.11"
val microutilsLoggingVersion = "3.0.5"
val mockkVersion = "1.13.7"
val pbandkVersion = "0.14.2"
val resultVersion = "1.1.18"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("kotlin-version", kotlinVersion)
            version("ktor-version", ktorVersion)

            plugin("ktor", "io.ktor.plugin").versionRef("ktor-version")
            plugin("serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin-version")

            library("kotlin-result", "com.michael-bull.kotlin-result:kotlin-result:$resultVersion")
            library("pbandk", "pro.streem.pbandk:pbandk-runtime:$pbandkVersion")

            //// logging
            library("microutils-logging", "io.github.microutils:kotlin-logging:$microutilsLoggingVersion")
            library("logback-classic", "ch.qos.logback:logback-classic:$logbackVersion")

            library("kotlinx-cli", "org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
            library("kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version(coroutinesVersion)
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationCoreVersion")

            //// ktor client
            library("ktor-utils", "io.ktor", "ktor-utils").versionRef("ktor-version")
            library("ktor-client-java", "io.ktor", "ktor-client-java").versionRef("ktor-version")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor-version")
            library("ktor-serialization-kotlinx-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor-version")
            bundle(
                "ktor-client",
                listOf(
                    "ktor-client-java",
                    "ktor-client-content-negotiation",
                    "ktor-serialization-kotlinx-json",
                )
            )

            //// ktor server
            library("ktor-server-core-jvm", "io.ktor", "ktor-server-core-jvm").versionRef("ktor-version")
            library("ktor-server-auth-jvm", "io.ktor", "ktor-server-auth-jvm").versionRef("ktor-version")
            library("ktor-server-auth-jwt-jvm", "io.ktor", "ktor-server-auth-jwt-jvm").versionRef("ktor-version")
            library("ktor-server-content-negotiation-jvm", "io.ktor", "ktor-server-content-negotiation-jvm")
                .versionRef("ktor-version")
            library("ktor-server-netty-jvm", "io.ktor", "ktor-server-netty-jvm").versionRef("ktor-version")
            library("ktor-serialization-kotlinx-json-jvm", "io.ktor", "ktor-serialization-kotlinx-json-jvm")
                .versionRef("ktor-version")
            library("ktor-network-tls-certificates", "io.ktor", "ktor-network-tls-certificates")
                .versionRef("ktor-version")
            bundle(
                "ktor-server",
                listOf(
                    "ktor-server-core-jvm",
                    "ktor-server-auth-jvm",
                    "ktor-server-auth-jwt-jvm",
                    "ktor-server-content-negotiation-jvm",
                    "ktor-server-netty-jvm",
                    "ktor-serialization-kotlinx-json-jvm",
                    "ktor-network-tls-certificates",
                )
            )

            library("kotlin-server-logging", "io.ktor", "ktor-server-call-logging").versionRef("ktor-version")
            bundle(
                "logging-server",
                listOf(
                    "kotlin-server-logging",
                    "logback-classic",
                )
            )

            library("kotlin-client-logging", "io.ktor", "ktor-client-logging").versionRef("ktor-version")
            bundle(
                "logging-client",
                listOf(
                    "kotlin-client-logging",
                    "logback-classic",
                )
            )

            //// testing
            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin-version")
            library("kotlin-test-common", "org.jetbrains.kotlin", "test-common").versionRef("kotlin-version")
            library("kotlin-test-annotations-common", "org.jetbrains.kotlin", "test-annotations-common").versionRef("kotlin-version")
            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").version(coroutinesVersion)

            library("ktor-server-tests-jvm", "io.ktor", "ktor-server-tests-jvm").versionRef("ktor-version")
            library("ktor-server-test-host", "io.ktor", "ktor-server-test-host").versionRef("kotlin-version")

            // property based testing
            library("kotest-property", "io.kotest", "kotest-property").version(kotestVersion)

            //// jvm only
            library("mockk", "io.mockk", "mockk").version(mockkVersion)
            library("kotlin-test-junit5", "org.jetbrains.kotlin", "kotlin-test-junit5").versionRef("kotlin-version")
            library("kotlin-test-junit", "org.jetbrains.kotlin", "kotlin-test-junit").versionRef("kotlin-version")

            // use ParameterizedTest feature feature
            library("junit-jupiter-params", "org.junit.jupiter:junit-jupiter-params:$jupitorVersion")
        }
    }
}

include ("egklib")
include ("egkliball")
