@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform") version "1.7.21"
    id("electionguard.common-conventions")

    // for some reason we need these, else get error
    // "Cannot add task 'commonizeNativeDistribution' as a task with that name already exists."
    // maybe related to bug in configuration caching to be fixed in kotlin 1.8.0
    alias(libs.plugins.serialization)
    // alias(libs.plugins.formatter)
}

// create build/libs/native/main/hacllib-cinterop-libhacl.klib
kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val arch = System.getProperty("os.arch")
    val nativeTarget =
        when {
            hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported.")
        }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libhacl by
                creating {
                    defFile(project.file("nativeInterop/libhacl.def"))
                    packageName("hacl")
                    compilerOpts("-Ilibhacl/include")
                    includeDirs.allHeaders("${System.getProperty("user.dir")}/libhacl/include")
                }
            }
        }
    }
}

// create build/libhacl.a
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