package electionguard.core

import electionguard.publish.exists
import electionguard.publish.fgetsFile
import kotlin.system.getTimeMillis

actual fun getSystemTimeInMillis() : Long = getTimeMillis()

actual fun fileExists(filename: String): Boolean = exists(filename)

actual fun fileReadLines(filename: String): List<String> {
    return fgetsFile(filename)
}

