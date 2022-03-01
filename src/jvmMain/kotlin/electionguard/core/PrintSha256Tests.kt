package electionguard.core

import electionguard.core.Base64.fromSafeBase64
import electionguard.core.Base64.toBase64

/**
 * Generates a test vector of inputs and outputs, based on Java's built-in SHA256 and HMAC-SHA256 functions,
 * which we're assuming are correct, to validate the other implementations.
 */
fun main() {
    val numInputs = 10

    println("package electionguard.core")
    println()
    println("import kotlin.test.Test")
    println("import kotlin.test.assertContentEquals")
    println("import electionguard.core.Base64.fromSafeBase64")
    println()
    println("class Sha256Tests {")

    val inputs = (1..numInputs).map { randomBytes(128) }
    val keys = (1..numInputs).map { randomBytes(32) }
    val hashes = inputs.map { it.sha256() }
    val hmacs = inputs.zip(keys).map { (input, key) -> input.hmacSha256(key) }

    println("        val inputs: List<ByteArray> = arrayListOf(")
    inputs.forEach { println("          \"${it.toBase64()}\",")}
    println("        ).map { it.fromSafeBase64() }")

    println("        val keys: List<ByteArray> = arrayListOf(")
    keys.forEach { println("          \"${it.toBase64()}\",")}
    println("        ).map { it.fromSafeBase64() }")

    println("        val hashes: List<ByteArray> = arrayListOf(")
    hashes.forEach { println("          \"${it.toBase64()}\",")}
    println("        ).map { it.fromSafeBase64() }")

    println("        val hmacs: List<ByteArray> = arrayListOf(")
    hmacs.forEach { println("          \"${it.toBase64()}\",")}
    println("        ).map { it.fromSafeBase64() }")

    println("    @Test")
    println("    fun testSha256() {")
    println("        runTest {")
    println("            (0..${numInputs-1}).forEach { i ->")
    println("                assertContentEquals(hashes[i], inputs[i].sha256())")
    println("            }")
    println("        }")
    println("    }")
    println()
    println("    @Test")
    println("    fun testHmacSha256() {")
    println("        runTest {")
    println("            (0..${numInputs-1}).forEach { i ->")
    println("                assertContentEquals(hmacs[i], inputs[i].hmacSha256(keys[i]))")
    println("            }")
    println("        }")
    println("    }")
    println("}")
}