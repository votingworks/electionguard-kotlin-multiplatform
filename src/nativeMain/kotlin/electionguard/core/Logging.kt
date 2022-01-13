package electionguard.core

import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel

actual fun loggingErrorsOnly() {
    KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.ERROR
}

actual fun loggingNormal() {
    KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.INFO
}
