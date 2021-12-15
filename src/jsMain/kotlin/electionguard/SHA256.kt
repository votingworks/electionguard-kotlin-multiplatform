package electionguard

@JsModule("@aws-crypto/sha256-universal")
external class Sha256 {
    fun update(data: String) : Unit
    fun digest(): ByteArray
}

actual fun ByteArray.sha256(): ByteArray {
    // import {Sha256} from '@aws-crypto/sha256-universal'
    //
    val hash = Sha256()
    hash.update(this)
    //const result = await hash.digest();


}

