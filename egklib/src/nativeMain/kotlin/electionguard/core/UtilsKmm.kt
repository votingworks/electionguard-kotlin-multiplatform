package electionguard.core

import electionguard.publish.exists
import electionguard.publish.fgetsFile
import electionguard.publish.isdirectory
import kotlin.system.getTimeMillis

actual fun getSystemTimeInMillis() : Long = getTimeMillis()

actual fun pathExists(path: String): Boolean = exists(path)

actual fun isDirectory(path: String): Boolean = isdirectory(path)

actual fun fileReadLines(filename: String): List<String> {
    return fgetsFile(filename)
}

