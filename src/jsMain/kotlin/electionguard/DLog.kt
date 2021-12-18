package electionguard

private const val MAX_DLOG: Int = 1_000_000_000

actual class DLog(val context: GroupContext) {
    // Implementation note for JavaScript: the JS interpreter is single-threaded,
    // so we don't have to worry about concurrency inside here. That certainly
    // simplifies things.

    private val dLogMapping: MutableMap<ElementModP, Int> =
        HashMap<ElementModP, Int>().apply { this[context.ONE_MOD_P] = 0 }

    private var dLogMaxElement = context.ONE_MOD_P
    private var dLogMaxExponent = 0

    actual fun dLog(input: ElementModP): Int? =
        if (input in dLogMapping) {
            dLogMapping[input]
        } else {
            var error = false

            while (input != dLogMaxElement) {
                if (dLogMaxExponent++ > MAX_DLOG) {
                    error = true
                    break
                } else {
                    dLogMaxElement *= context.G_MOD_P
                    dLogMapping[dLogMaxElement] = dLogMaxExponent
                }
            }

            if (error) null else dLogMaxExponent
        }
}