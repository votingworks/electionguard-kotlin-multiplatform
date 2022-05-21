package electionguard.core

expect fun getSystemTimeInMillis(): Long

expect fun fileExists(filename: String): Boolean

expect fun fileReadLines(filename: String): List<String>

