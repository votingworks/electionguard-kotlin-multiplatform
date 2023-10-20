buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.9.10"
    application
}

repositories {
    mavenCentral()
}

// 9/9/2023
val coroutinesVersion = "1.6.4" // "1.7.3" see issue #362
val jupitorVersion = "5.10.0"
val kotlinVersion = "1.9.10"
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

tasks.startScripts {
    dependsOn(":egklib:jar")
}

dependencies {
    // project(":egklib").setTargetConfiguration("jvmRuntimeElements")
    implementation(files("../egklib/build/libs/egklib-jvm-2.0.0-SNAPSHOT.jar"))
    implementation("com.michael-bull.kotlin-result:kotlin-result:$resultVersion")
    implementation("pro.streem.pbandk:pbandk-runtime:$pbandkVersion")
    implementation("io.github.microutils:kotlin-logging:$microutilsLoggingVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCliVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation(libs.logback.classic)
}

tasks.register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveBaseName = "egklib"


    manifest {
            attributes("Main-Class" to "electionguard.cli.RunVerifierKt")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from runtimeClasspath: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
