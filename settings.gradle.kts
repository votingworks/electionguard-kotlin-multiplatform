rootProject.name = "electionguard-kotlin-multiplatform"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("kotlin-version", "1.7.20")
            version("coroutines-version", "1.6.4")
            version("ktor-version", "2.2.2")

            plugin("ktor", "io.ktor.plugin").versionRef("ktor-version")
            plugin("serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin-version")
            plugin("formatter", "tech.formatter-kt.formatter").version("0.7.9")

            library("kotlinx-cli", "org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
            library("kotlinx-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("coroutines-version")
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

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

            // depends on kotlin-stdlib-common:1.6.20 -> 1.7.20
            library("kotlin-result", "com.michael-bull.kotlin-result:kotlin-result:1.1.16")
            // depends on kotlin 1.5.32 -> 1.7.20, coroutines 1.5.2, protoc 3.19.1
            library("pbandk", "pro.streem.pbandk:pbandk-runtime:0.14.1")

            //// logging
            library("microutils-logging", "io.github.microutils:kotlin-logging:3.0.4")
            library("logback-classic", "ch.qos.logback:logback-classic:1.3.4")

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

            //implementation(kotlin("test-common", kotlinVersion))
            //implementation(kotlin("test-annotations-common", kotlinVersion))

            //// testing
            library("kotlin-test-common", "org.jetbrains.kotlin", "test-common").versionRef("kotlin-version")
            library("kotlin-test-annotations-common", "org.jetbrains.kotlin", "test-annotations-common").versionRef("kotlin-version")

            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("coroutines-version")
            library("ktor-server-tests-jvm", "io.ktor", "ktor-server-tests-jvm").versionRef("ktor-version")

            // property based testing
            library("kotest-property", "io.kotest", "kotest-property").version("5.5.4")

            //// jvm only
            // uses junit:junit:4.13.2
            library("mockk", "io.mockk", "mockk").version("1.13.2")
            library("kotlin-test-junit5", "org.jetbrains.kotlin", "kotlin-test-junit5").versionRef("kotlin-version")
            library("kotlin-test-junit", "org.jetbrains.kotlin", "kotlin-test-junit").versionRef("kotlin-version")
        }
    }
}

include ("egklib")
include ("hacllib")
include ("webapps:decryptingtrustee")
include ("webapps:decryption")
include ("webapps:keyceremony")
include ("webapps:keyceremonytrustee")
