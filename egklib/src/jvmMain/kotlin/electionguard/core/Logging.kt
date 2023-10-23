package electionguard.core

actual fun loggingErrorsOnly() {
    // set in the logback.xml and logback-test.xml file, not here
    // DelegatingKLogger.underlyingLogger
    // Configurator.setLevel(logger.name, Level.DEBUG)
    // https://github.com/oshai/kotlin-logging/wiki#q-how-do-i-set-the-log-level
}

actual fun loggingNormal() {
    // set in the logback.xml and logback-test.xml file, not here
}
