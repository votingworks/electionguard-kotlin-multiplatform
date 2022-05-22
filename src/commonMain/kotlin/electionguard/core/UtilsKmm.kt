package electionguard.core

expect fun getSystemTimeInMillis(): Long

expect fun fileExists(filename: String): Boolean

/** Read lines from a file. */
expect fun fileReadLines(filename: String): List<String>

