package electionguard.core

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

actual fun getSystemTimeInMillis() : Long = System.currentTimeMillis()

actual fun pathExists(path: String): Boolean = Files.exists(Path.of(path))

actual fun isDirectory(path: String): Boolean = Files.isDirectory(Path.of(path))

actual fun fileReadLines(filename: String): List<String> = File(filename).readLines()

actual fun fileReadBytes(filename: String): ByteArray = File(filename).readBytes()



