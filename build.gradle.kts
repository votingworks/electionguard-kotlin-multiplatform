plugins {
    kotlin("multiplatform") version "1.6.0"
    id("tech.formatter-kt.formatter") version "0.7.7"
}

group = "electionguard-kotlin-multiplatform"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
//        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(LEGACY) {
        useCommonJs()
        browser { commonWebpackConfig { cssSupport.enabled = true } }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }


    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libhacl by
                cinterops.creating {
                    defFile(project.file("src/nativeInterop/libhacl.def"))
                    packageName("hacl")
                    compilerOpts("-Ilibhacl/include")
                    includeDirs.allHeaders("${System.getProperty("user.dir")}/libhacl/include")
                }
            }
        }

//        binaries { executable { entryPoint = "main" } }
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
                implementation(kotlin("stdlib-common", "1.6.0"))
                implementation(kotlin("stdlib", "1.6.0"))

                // JSON serialization and DSL
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

                // Coroutines
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
            }
        }
        val commonTest by
        getting {
            dependencies {
                implementation(kotlin("test-common", "1.6.0"))
                implementation(kotlin("test-annotations-common", "1.6.0"))

                // Fancy property-based testing
                implementation("io.kotest:kotest-property:5.0.1")
            }
        }
        val jvmMain by
        getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8", "1.6.0"))
            }
        }
        val jvmTest by
        getting {
            dependencies {
            }
        }
        val jsMain by
        getting {
            dependencies {
//                    implementation(npm("gmp-wasm", "0.9.0", generateExternals = true))
                implementation(kotlin("org.jetbrains.kotlin","stdlib-js:1.6.0"))

                // Portable, Kotlin port of Java's BigInteger; slow but works
                implementation("io.github.gciatto:kt-math-js:0.4.0")
            }
        }
        val jsTest by
        getting {
            dependencies {
            }
        }
        val nativeMain by getting
        val nativeTest by
        getting {
            dependencies {
            }
        }
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
