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

dependencies {
    // implementation(project(":egklib")) // make this depend on egklib being run

    implementation(files("../egklib/build/libs/egklib-jvm-2.0.0-SNAPSHOT.jar")) // add the library to the fatJar
    implementation(libs.kotlin.result)
    implementation(libs.pbandk)
    implementation(libs.oshai.logging)
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.cli)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
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
