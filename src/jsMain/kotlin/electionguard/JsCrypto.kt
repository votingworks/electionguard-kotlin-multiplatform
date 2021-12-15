package electionguard

import kotlinx.coroutines.*
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.js.Promise

@JsModule("@aws-crypto/random-source-node")
external fun randomValues(size: Int): Promise<Uint8Array>

/**
 * Fetches `count` bytes of entropy from the browser or platform's secure random number generator.
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun platformRandomValues(count: Int): ByteArray {
    return GlobalScope.async {
        val ubytes = randomValues(count).await()
        val bytes = ByteArray(count) { ubytes[it] }
        bytes
    }.getCompleted()
}
