@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("electionguard.webapps-conventions")
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

dependencies {
    implementation(project(path = ":egklib", configuration = "jvmRuntimeElements"))

    implementation(libs.kotlin.result)
    implementation(libs.kotlinx.cli)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging.client)

    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.kotlin.test.junit)

    // mocking only available on jvm
    testImplementation(libs.mockk)
}