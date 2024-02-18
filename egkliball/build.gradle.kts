plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("../egklib/build/libs/egklib-jvm-2.0.4-SNAPSHOT.jar")) // add the library to the fatJar
    implementation(libs.bundles.eglib)
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
