package electionguard.core

import hacl.*
import kotlinx.cinterop.convert

actual fun ByteArray.sha256(): ByteArray {
    val state = Hacl_Streaming_SHA2_create_in_256()
    val output = ByteArray(32)

    this.useNative {
        Hacl_Streaming_SHA2_update_256(state, it, this.size.convert())
    }

    output.useNative {
        Hacl_Streaming_SHA2_finish_256(state, it)
    }

    Hacl_Streaming_SHA2_free_256(state)

    return output
}

actual fun ByteArray.hmacSha256(key: ByteArray): ByteArray {
    val output = ByteArray(32)
    output.useNative { o ->
        this.useNative { d ->
            key.useNative { k ->
                Hacl_HMAC_compute_sha2_256(o, k, key.size.toUInt(), d, this.size.toUInt())
            }
        }
    }
    return output
}
