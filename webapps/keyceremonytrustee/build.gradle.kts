@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    application
    kotlin("jvm") version "1.7.20"
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "webapps.electionguard"
version = "0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/danwallach/electionguard-kotlin-multiplatform")
        credentials {
            username = project.findProperty("github.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("github.key") as String? ?: System.getenv("TOKEN")
        }
    }
    mavenCentral()
}

dependencies {
    implementation("electionguard-kotlin-multiplatform:electionguard-kotlin-multiplatform-jvm:1.52.5-SNAPSHOT")

    implementation(libs.kotlin.result)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging)

    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.kotlin.test.junit)
}