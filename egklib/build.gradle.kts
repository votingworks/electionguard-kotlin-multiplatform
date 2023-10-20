val kotlin_version: String by project

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform") version "1.9.10"
    alias(libs.plugins.serialization)
    application
}

repositories {
    mavenCentral()
}

group = "electionguard-kotlin-multiplatform"
version = "2.0.0-SNAPSHOT"

kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        //        withJava()
        testRuns["test"].executionTask
            .configure {
                useJUnitPlatform()
                minHeapSize = "512m"
                maxHeapSize = "8g"
                jvmArgs = listOf("-Xss128m")

                // Make tests run in parallel
                // More info: https://www.jvt.me/posts/2021/03/11/gradle-speed-parallel/
                systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
                systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
                systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
            }
    }

    /*
val hostOs = System.getProperty("os.name")
val isMingwX64 = hostOs.startsWith("Windows")
val arch = System.getProperty("os.arch")
val nativeTarget =
    when {
        hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
        hostOs == "Mac OS X"-> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS '$hostOs' arch '$arch' is not supported.")
    }

nativeTarget.apply {
    binaries {
        sharedLib() {
            baseName = "ekm" // on Linux and macOS
            // baseName = "libekm // on Windows
        }
    }
}

 */

    sourceSets {
        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        val commonMain by
            getting {
                dependencies {
                    // JSON serialization
                    implementation(libs.kotlinx.serialization.json)

                    // Coroutines
                    implementation(libs.kotlinx.coroutines.core)

                    // Useful, portable routines
                    // implementation(libs.ktor.utils)

                    // Portable logging interface.
                    implementation(libs.microutils.logging)

                    // A multiplatform Kotlin library for working with date and time.
                    implementation(libs.kotlinx.datetime)

                    // A multiplatform Kotlin library for working with protobuf.
                    implementation(libs.pbandk)

                    // A multiplatform Kotlin library for command-line parsing (could use enableEndorsedLibs instead)
                    implementation(libs.kotlinx.cli)

                    // A multiplatform Kotlin library for Result monads
                    implementation(libs.kotlin.result)
                }
            }
        val commonTest by
            getting {
                dependencies {
                    implementation(kotlin("test-common"))
                    implementation(kotlin("test-annotations-common"))

                    // runTest() for running suspend functions in tests
                    implementation(libs.kotlinx.coroutines.test)

                    // Fancy property-based testing
                    implementation(libs.kotest.property)
                }
            }
        val jvmMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-jdk8")) }
            }
        val jvmTest by
            getting {
                dependencies {
                    // Progress bars
                    implementation("me.tongfei:progressbar:0.9.3")

                    // junit5 only available on jvm
                    implementation(libs.kotlin.test.junit5)

                    // mocking only available on jvm
                    implementation(libs.mockk)

                    // logger implementation
                    implementation(libs.logback.classic)

                    // use ParameterizedTest feature
                    implementation(libs.junit.jupiter.params)
                }
            }
        /* val nativeMain by getting {
            dependencies {
                implementation(project(":hacllib"))
            }
        }
        val nativeTest by getting { dependencies {} }

         */
    }
}

// val protoGenSource by extra("build/generated/source/proto")

/*
protoc --pbandk_out=./egklib/src/commonMain/kotlin/ --proto_path=./egklib/src/commonMain/proto \
                    common.proto encrypted_ballot.proto encrypted_tally.proto \
                    election_record.proto manifest.proto \
                    plaintext_ballot.proto decrypted_tally.proto \
                    trustees.proto
 */
val compileProtobuf =
    tasks.register("compileProtobuf") {
        doLast {
            print("* Compiling protobuf *\n")
            /* project.exec {
             *        commandLine = "rm -f ./src/commonMain/kotlin/electionguard/protogen".split("
             * ")
             * } */
            // TODO lame
            val commandLineStr =
                "protoc --pbandk_out=./egklib/src/commonMain/kotlin/ --proto_path=./egklib/src/commonMain/proto " +
                    "common.proto encrypted_ballot.proto encrypted_tally.proto " +
                    "election_record.proto manifest.proto " +
                    "plaintext_ballot.proto decrypted_tally.proto " +
                    "trustees.proto"
            project.exec { commandLine = commandLineStr.split(" ") }
        }
    }

tasks.withType<Test> { testLogging { showStandardStreams = true } }

// LOOK some kind of javascript security thing, but may be causing coupled projects
// https://docs.gradle.org/current/userguide/multi_project_configuration_and_execution.html#sec:decoupled_projects
// allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask> {
        args += "--ignore-scripts"
    }
// }

// Workaround the Gradle bug resolving multi-platform dependencies.
// Fix courtesy of https://github.com/square/okio/issues/647
configurations.forEach {
    if (it.name.lowercase().contains("kapt") || it.name.lowercase().contains("proto")) {
        it.attributes
            .attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach { kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn" }
