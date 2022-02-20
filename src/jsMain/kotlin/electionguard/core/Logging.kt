package electionguard.core

import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel

actual fun loggingErrorsOnly() {
    KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.ERROR
}

actual fun loggingNormal() {
    KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.INFO
}
