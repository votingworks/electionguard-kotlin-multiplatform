package electionguard

import hacl.Hacl_Streaming_SHA2_create_in_256
import hacl.Hacl_Streaming_SHA2_finish_256
import hacl.Hacl_Streaming_SHA2_free_256
import hacl.Hacl_Streaming_SHA2_update_256
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