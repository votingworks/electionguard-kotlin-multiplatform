package electionguard

actual fun randomBytes(length: Int): ByteArray {
    val array = ByteArray(length)
    if (isNodeJs()) {
        val c = js("require")("crypto")
        c.randomFillSync(array)
    } else if (isBrowser()) {
        js("window").crypto.getRandomValues(array)
    } else {
        throw NotImplementedError("we don't have randomBytes on this platform")
    }
    return array
}