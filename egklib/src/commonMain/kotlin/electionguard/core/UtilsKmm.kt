package electionguard.core

/** Get the current time in msecs since epoch */
expect fun getSystemTimeInMillis(): Long

/** Does this file exist? */
expect fun fileExists(filename: String): Boolean

/** Read lines from a file. */
expect fun fileReadLines(filename: String): List<String>

