package electionguard.publish

import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.PATH_MAX
import platform.posix.getcwd
import platform.posix.posix_errno
import platform.posix.realpath
import platform.posix.strerror_r

private val debug = false

@Throws(IOException::class)
fun absPath(filename: String): String {
    memScoped {
        if (debug) {
            // getcwd(
            //    __buf: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
            //    __size: platform.posix.size_t /* = kotlin.ULong */)
            // : kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>? { /* compiled code */ }
            val cwdRef: CArrayPointer<ByteVar> = allocArray(PATH_MAX)
            val cwd = getcwd(cwdRef, PATH_MAX)
            if (cwd == null) {
                checkErrno { mess -> throw IOException("Fail getcwd $mess") }
            }
            println("++++++++++cwd '${cwd?.toKString()}'")
        }

        // realpath(
        //     @kotlinx.cinterop.internal.CCall.CString __name: kotlin.String?,
        //     __resolved: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?)
        //     : kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?
        val stringRef: CArrayPointer<ByteVar> = allocArray(PATH_MAX)
        val abspathPtr = realpath(filename, stringRef)
        if (abspathPtr == null) {
            checkErrno { mess -> throw IOException("Fail realpath $mess on $filename") }
        }
        if (debug) {
            println("++++++++++absPath '$filename' to '${abspathPtr?.toKString()}'")
        }
        return abspathPtr!!.toKString()
    }
}

fun checkErrno(dothis: (mess : String) -> Unit)  {
    val perrno: Int = posix_errno()
    if (perrno == 0) {
        return
    }

    return memScoped {
        // strerror_r(
        //   __errnum: kotlin.Int,
        //   __buf: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
        //   __buflen: platform.posix.size_t /* = kotlin.ULong */)
        // : kotlin.Int { /* compiled code */ }

        val max = 100
        val mess: CArrayPointer<ByteVar> = allocArray(max)
        val strerr = strerror_r(perrno, mess, max.toULong())
        if (strerr != 0) {
            dothis("errno = $perrno")
            return
        }
        dothis("errno = $perrno '${mess.toKString()}'")
    }
}