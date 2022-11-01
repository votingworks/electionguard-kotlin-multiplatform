buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform") version "1.7.20"

    // cross-platform serialization support
    kotlin("plugin.serialization") version "1.7.20"

    // https://github.com/hovinen/kotlin-auto-formatter
    // Creates a `formatKotlin` Gradle action that seems to be reliable.
    id("tech.formatter-kt.formatter") version "0.7.9"

    id("maven-publish")

    java
    application
}

group = "electionguard-kotlin-multiplatform"
version = "1.52-SNAPSHOT"

val coroutinesVersion by extra("1.6.4")
val jsonSerializationVersion by extra("1.4.1")
val kotlinVersion by extra("1.7.20")
val ktorVersion by extra("2.1.3")
val logback_version by extra("1.3.4")
val pbandkVersion by extra("0.14.1")

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        //        withJava()
        testRuns["test"].executionTask
            .configure {
                useJUnitPlatform()
                minHeapSize = "512m"
                maxHeapSize = "2048m"
                jvmArgs = listOf("-Xss128m")

                // Make tests run in parallel
                // More info: https://www.jvt.me/posts/2021/03/11/gradle-speed-parallel/
                systemProperties["junit.jupiter.execution.parallel.enabled"] = "true"
                systemProperties["junit.jupiter.execution.parallel.mode.default"] = "same_thread"
                systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] =
                    "concurrent"
            }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val arch = System.getProperty("os.arch")
    val nativeTarget =
        when {
            hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
            hostOs == "Mac OS X"-> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported.")
        }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libhacl by
                    creating {
                        defFile(project.file("src/nativeInterop/libhacl.def"))
                        packageName("hacl")
                        compilerOpts("-Ilibhacl/include")
                        includeDirs.allHeaders("${System.getProperty("user.dir")}/libhacl/include")
                    }
            }
        }

        binaries {
            executable("RunBatchEncryption") {
                entryPoint = "electionguard.encrypt.main"
            }
            sharedLib() {
                baseName = "ekm" // on Linux and macOS
                // baseName = "libekm // on Windows
            }
        }
    }

    sourceSets {
        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        val commonMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-common", kotlinVersion))
                    implementation(kotlin("stdlib", kotlinVersion))

                    // JSON serialization and DSL
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$jsonSerializationVersion")

                    // Coroutines
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

                    // Useful, portable routines
                    implementation("io.ktor:ktor-utils:$ktorVersion")

                    // Portable logging interface. On the JVM, we'll get "logback", which gives
                    // us lots of features. On Native, it ultimately just prints to stdout.
                    // On JS, it uses console.log, console.error, etc.
                    implementation("io.github.microutils:kotlin-logging:3.0.2")

                    // A multiplatform Kotlin library for working with date and time.
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                    // A multiplatform Kotlin library for working with protobuf.
                    implementation("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")

                    // A multiplatform Kotlin library for command-line parsing (could use enableEndorsedLibs instead)
                    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")

                    // A multiplatform Kotlin library for Result monads
                    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.16")
                }
            }
        val commonTest by
            getting {
                dependencies {
                    implementation(kotlin("test-common", kotlinVersion))
                    implementation(kotlin("test-annotations-common", kotlinVersion))

                    // runTest() for running suspend functions in tests
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

                    // Fancy property-based testing
                    implementation("io.kotest:kotest-property:5.4.0")
                }
            }
        val jvmMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-jdk8", kotlinVersion))

                    // Progress bars
                    implementation("me.tongfei:progressbar:0.9.3")

                    // Logging implementation (used by "kotlin-logging"). Note that we need
                    // a bleeding-edge implementation to ensure we don't have vulnerabilities
                    // similar to (but not as bad) as the log4j issues.
                    implementation("ch.qos.logback:logback-classic:1.3.4")
                }
            }
        val jvmTest by
            getting {
                dependencies {
                    // Unclear if we really need all the extra features of JUnit5, but it would
                    // at least be handy if we could get its parallel test runner to work.
                    implementation(kotlin("test-junit5", kotlinVersion))
                }
            }
        val nativeMain by getting { dependencies {} }
        val nativeTest by getting { dependencies {} }
    }
}

val protoGenSource by
    extra("build/generated/source/proto")

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
                "protoc --pbandk_out=./src/commonMain/kotlin/ --proto_path=./src/commonMain/proto " +
                    "common.proto encrypted_ballot.proto encrypted_tally.proto " +
                    "election_record.proto manifest.proto " +
                    "plaintext_ballot.proto decrypted_tally.proto " +
                    "trustees.proto"
            project.exec { commandLine = commandLineStr.split(" ") }
        }
    }

tasks.register("libhaclBuild") {
    doLast {
        exec {
            workingDir(".")
            // -p flag will ignore errors if the directory already exists
            commandLine("mkdir", "-p", "build")
        }
        exec {
            workingDir("build")
            commandLine("cmake", "-DCMAKE_BUILD_TYPE=Release", "..")
        }
        exec {
            workingDir("build")
            commandLine("make")
        }
    }
}

// hack to make sure that we've compiled the library prior to running cinterop on it
tasks["cinteropLibhaclNative"].dependsOn("libhaclBuild")

tasks.withType<Test> { testLogging { showStandardStreams = true } }

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask> {
        args += "--ignore-scripts"
    }
}

// Workaround the Gradle bug resolving multi-platform dependencies.
// Fix courtesy of https://github.com/square/okio/issues/647
configurations.forEach {
    if (it.name.toLowerCase().contains("kapt") || it.name.toLowerCase().contains("proto")) {
        it.attributes
            .attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>()
    .configureEach { kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn" }

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/danwallach/electionguard-kotlin-multiplatform")
            credentials {
                username = project.findProperty("github.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("github.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
