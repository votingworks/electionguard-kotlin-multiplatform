package electionguard.core

import electionguard.publish.exists
import kotlin.system.getTimeMillis

actual fun getSystemTimeInMillis() : Long = getTimeMillis()

actual fun fileExists(filename: String): Boolean = exists(filename)

actual fun fileReadLines(filename: String): List<String> {
    // LOOK not working
    return emptyList()
}

