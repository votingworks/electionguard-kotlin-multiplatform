package electionguard

/** Distinguish if we're in Node.js (true) or in a browser (false). */
fun isNodeJs(): Boolean {
    // the process title is typically something like:
    // /Users/dwallach/.gradle/nodejs/node-v14.17.0-darwin-x64/bin/node

    val processTitle = js("process").title
    return processTitle.toString().endsWith("node")
}

/**
 * Fetches `count` bytes of entropy from the browser or platform's secure random number generator.
 */
fun platformRandomValues(count: Int): ByteArray {
    val array = ByteArray(count)
    if (isNodeJs()) {
        // The level of experimentation required to get here was insane, but at least it works;
        // GitHub CoPilot made a bunch of recommendations that were wildly incorrect, but it
        // did come up with a version of the line below, which was cool.
        val c = js("require")("crypto")
        c.randomFillSync(array)
    } else {
        // TODO: Validate this in a browser, since Kotlin's test framework uses Node.
        js("window").crypto.getRandomValues(array)
    }
    return array
}