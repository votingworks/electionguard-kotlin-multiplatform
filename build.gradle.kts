

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform") version "1.6.10"

    // cross-platform serialization support
    kotlin("plugin.serialization") version "1.6.10"

    // https://github.com/hovinen/kotlin-auto-formatter
    // Creates a `formatKotlin` Gradle action that seems to be reliable.
    id("tech.formatter-kt.formatter") version "0.7.9"

    java
}

group = "electionguard-kotlin-multiplatform"
version = "1.0-SNAPSHOT"

val protobufVersion by extra("3.19.4")
val pbandkVersion by extra("0.13.0")

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

    js(LEGACY) {
        moduleName = "electionguard"

        useCommonJs()
        binaries.executable()

        nodejs {
            version = "16.13.1"

            testTask {
                useMocha {
                    // thirty seconds rather than the default of two seconds
                    timeout = "30000"
                }

                testLogging {
                    showExceptions = true
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showCauses = true
                    showStackTraces = true
                }
            }
        }

        browser {
            testTask {
                useKarma { useChromeHeadless() }

                testLogging {
                    showExceptions = true
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showCauses = true
                    showStackTraces = true
                }
            }
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
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
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

        binaries { executable { entryPoint = "main" } }
        //        binaries {
        //            staticLib {
        //                entryPoint = "main"
        //            }
        //        }
    }

    sourceSets {
        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        val commonMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-common", "1.6.10"))
                    implementation(kotlin("stdlib", "1.6.10"))

                    // JSON serialization and DSL
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")

                    // Coroutines
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

                    // Useful, portable routines
                    implementation("io.ktor:ktor-utils:1.6.7")

                    // Portable logging interface. On the JVM, we'll get "logback", which gives
                    // us lots of features. On Native, it ultimately just prints to stdout.
                    // On JS, it uses console.log, console.error, etc.
                    implementation("io.github.microutils:kotlin-logging:2.1.21")

                    // A multiplatform Kotlin library for working with date and time.
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

                    // A multiplatform Kotlin library for working with date and time.
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

                    implementation("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
                }
            }
        val commonTest by
            getting {
                dependencies {
                    implementation(kotlin("test-common", "1.6.10"))
                    implementation(kotlin("test-annotations-common", "1.6.10"))

                    // runTest() for running suspend functions in tests
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

                    // Fancy property-based testing
                    implementation("io.kotest:kotest-property:5.0.1")
                }
            }
        val jvmMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-jdk8", "1.6.10"))

                    // Progress bars
                    implementation("me.tongfei:progressbar:0.9.2")

                    // Logging implementation (used by "kotlin-logging"). Note that we need
                    // a bleeding-edge implementation to ensure we don't have vulnerabilities
                    // similar to (but not as bad) as the log4j issues.
                    implementation("ch.qos.logback:logback-classic:1.3.0-alpha12")
                }
            }
        val jvmTest by
            getting {
                dependencies {
                    // Unclear if we really need all the extra features of JUnit5, but it would
                    // at least be handy if we could get its parallel test runner to work.
                    implementation(kotlin("test-junit5", "1.6.10"))
                }
            }
        val jsMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-js", "1.6.10"))

                    // Portable, Kotlin port of Java's BigInteger; slow but works
                    implementation("io.github.gciatto:kt-math:0.4.0")
                }
            }
        val jsTest by getting { dependencies { implementation(kotlin("test-js", "1.6.10")) } }
        val nativeMain by getting { dependencies {} }
        val nativeTest by getting { dependencies {} }
    }
}

val protoGenSource by
    extra("/home/snake/dev/github/electionguard-kotlin-multiplatform/build/generated/source/proto")

val compileProtobuf =
    tasks.register("compileProtobuf") {
        doLast {
            print("* Compiling protobuf *\n")
            /* project.exec {
             *        commandLine = "rm -f ./src/commonMain/kotlin/electionguard/protogen".split("
             * ")
             * } */
            val commandLineStr =
                "protoc --pbandk_out=./src/commonMain/kotlin/ --proto_path=./src/commonMain/proto" +
                    " " + "ciphertext_ballot.proto ciphertext_tally.proto common.proto " +
                    "election_record.proto manifest.proto " +
                    "plaintext_ballot.proto plaintext_tally.proto"
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

task ("printSha256Tests", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    main = "electionguard.core.PrintSha256TestsKt"
}

tasks["printSha256Tests"].dependsOn("jvmMainClasses")

tasks.withType<Test> { testLogging { showStandardStreams = true } }

// Hack to get us a newer version of NodeJs than the default of 14.17.0
rootProject.plugins
    .withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()
            .download = true
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()
            .nodeVersion = "16.13.1"
    }

// ensures that the yarn.lock file is persistent
// https://blog.jetbrains.com/kotlin/2021/10/control-over-npm-dependencies-in-kotlin-js/
rootProject.plugins
    .withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>()
            .disableGranularWorkspaces()
    }

tasks.register("backupYarnLock") {
    dependsOn(":kotlinNpmInstall")

    doLast {
        copy {
            from("$rootDir/build/js/yarn.lock")
            rename { "yarn.lock.bak" }
            into(rootDir)
        }
    }

    inputs.file("$rootDir/build/js/yarn.lock").withPropertyName("inputFile")
    outputs.file("$rootDir/yarn.lock.bak").withPropertyName("outputFile")
}

val restoreYarnLock =
    tasks.register("restoreYarnLock") {
        doLast {
            copy {
                from("$rootDir/yarn.lock.bak")
                rename { "yarn.lock" }
                into("$rootDir/build/js")
            }
        }

        inputs.file("$rootDir/yarn.lock.bak").withPropertyName("inputFile")
        outputs.file("$rootDir/build/js/yarn.lock").withPropertyName("outputFile")
    }

tasks["kotlinNpmInstall"].dependsOn("restoreYarnLock")

tasks.register("validateYarnLock") {
    dependsOn(":kotlinNpmInstall")

    doLast {
        val expected = file("$rootDir/yarn.lock.bak").readText()
        val actual = file("$rootDir/build/js/yarn.lock").readText()

        if (expected != actual) {
            throw AssertionError(
                "Generated yarn.lock differs from the one in the repository. " +
                    "It can happen because someone has updated a dependency and haven't run " +
                    "`./gradlew :backupYarnLock --refresh-dependencies` " + "afterwards."
            )
        }
    }

    inputs.files("$rootDir/yarn.lock.bak", "$rootDir/build/js/yarn.lock")
        .withPropertyName("inputFiles")
}

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
    .configureEach { kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn" }