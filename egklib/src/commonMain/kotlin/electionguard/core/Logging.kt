package electionguard.core

/** Instruct the underlying logging library to suppress anything besides errors. */
expect fun loggingErrorsOnly()

/** Instruct the underlying logging library to behave as normal */
expect fun loggingNormal()
