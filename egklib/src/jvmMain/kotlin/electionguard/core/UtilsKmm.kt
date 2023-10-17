package electionguard.core

import mu.KotlinLogging
import java.io.File
import java.nio.ByteOrder
import java.nio.ByteOrder.nativeOrder
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger("UtilsKmmJvm")

actual fun getSystemTimeInMillis() : Long = System.currentTimeMillis()

actual fun pathExists(path: String): Boolean = Files.exists(Path.of(path))

actual fun createDirectories(directory: String): Boolean {
    if (pathExists(directory)) {
        return true
    }
    try {
        Files.createDirectories(Path.of(directory))
        logger.warn("error createDirectories = '$directory' ")
        return true
    } catch (t: Throwable) {
        return false
    }
}

actual fun isDirectory(path: String): Boolean = Files.isDirectory(Path.of(path))

actual fun fileReadLines(filename: String): List<String> = File(filename).readLines()

actual fun fileReadBytes(filename: String): ByteArray = File(filename).readBytes()

actual fun isBigEndian(): Boolean = nativeOrder() == ByteOrder.BIG_ENDIAN





