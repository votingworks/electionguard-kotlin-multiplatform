package electionguard

/**
 * Kotest requires its properties to be executed as a suspending function, and doing this isn't
 * quite portable (`runBlocking` on the JVM, isn't available on JS).
 */
expect fun runProperty(f: suspend () -> Unit): Unit
