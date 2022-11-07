// cant get access to version catalog in buildSrc.
// may get fixed in future gradle??

plugins {
    application
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/danwallach/electionguard-kotlin-multiplatform")
        credentials {
            username = project.findProperty("github.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("github.key") as String? ?: System.getenv("TOKEN")
        }
    }
    mavenCentral()
}