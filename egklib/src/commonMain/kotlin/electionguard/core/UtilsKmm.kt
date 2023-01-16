package electionguard.core

/** Get the current time in msecs since epoch */
expect fun getSystemTimeInMillis(): Long

/** Does this path exist? */
expect fun pathExists(path: String): Boolean

/** Is this path a directory? */
expect fun isDirectory(path: String): Boolean

/** Read lines from a file. */
expect fun fileReadLines(filename: String): List<String>

