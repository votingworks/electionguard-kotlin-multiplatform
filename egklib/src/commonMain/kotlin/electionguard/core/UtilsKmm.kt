package electionguard.core

/** Get the current time in msecs since epoch */
expect fun getSystemTimeInMillis(): Long

/** Does this path exist? */
expect fun pathExists(path: String): Boolean

/** Create the named directory */
expect fun createDirectories(directory: String): Boolean

/** Is this path a directory? */
expect fun isDirectory(path: String): Boolean

/** Read lines from a file. */
expect fun fileReadLines(filename: String): List<String>

/** Read all the bytes in a file. */
expect fun fileReadBytes(filename: String): ByteArray

/** Read all int text in a file. */
expect fun fileReadText(filename: String): String

/** Determine endianness of machine. */
expect fun isBigEndian(): Boolean

