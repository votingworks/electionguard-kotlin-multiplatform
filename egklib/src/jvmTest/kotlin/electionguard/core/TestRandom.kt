package electionguard.core

import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.security.Security
import java.util.*

class TestRandom {

    @Test
    fun testRandom() {
        val rng = Random()
        println("Random $rng")
    }

    @Test
    fun testSecureRandom() {
        val rng = SecureRandom.getInstanceStrong()
        println("SecureRandom.getInstanceStrong")
        println("  algo=${rng.algorithm}")
        println("  params=${rng.parameters}")
        println("  provider=${rng.provider}")
    }

    @Test
    fun showAlgorithms() {
        val algorithms = Security.getAlgorithms ("SecureRandom");
        println("Available algorithms")
        algorithms.forEach { println("  $it") }
    }

}