package electionguard.core

import io.ktor.util.date.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

actual fun getSystemTimeInMillis() : Long = getTimeMillis()

actual fun fileExists(filename: String): Boolean = Files.exists(Path.of(filename))

actual fun fileReadLines(filename: String): List<String> = File(filename).readLines()


