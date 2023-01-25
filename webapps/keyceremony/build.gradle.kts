buildscript {
    repositories {
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version providers.gradleProperty("kotlinVersion").get()
    id("electionguard.common-conventions")
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "webapps.electionguard"
version = "0.2"
application {
    mainClass.set("webapps.electionguard.keyceremony.RunRemoteKeyCeremonyKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(project(path = ":egklib", configuration = "jvmRuntimeElements"))

    implementation(libs.kotlin.result)
    implementation(libs.kotlinx.cli)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.logging.client)

    testImplementation(libs.kotlin.test.junit)
    // mocking only available on jvm
    testImplementation(libs.mockk)
}