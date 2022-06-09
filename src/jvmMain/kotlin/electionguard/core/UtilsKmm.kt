package electionguard.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

actual fun getSystemTimeInMillis() : Long = System.currentTimeMillis()

actual fun fileExists(filename: String): Boolean = Files.exists(Path.of(filename))

actual fun fileReadLines(filename: String): List<String> = File(filename).readLines()


