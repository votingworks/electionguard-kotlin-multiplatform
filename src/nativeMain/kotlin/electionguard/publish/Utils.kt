package electionguard.publish

import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.PATH_MAX
import platform.posix.getcwd
import platform.posix.lstat
import platform.posix.mkdir
import platform.posix.posix_errno
import platform.posix.realpath
import platform.posix.stat
import platform.posix.strerror_r

private val debug = false

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

fun exists(filename: String): Boolean {
    memScoped {
        val stat = alloc<stat>()
        // lstat(@kotlinx.cinterop.internal.CCall.CString __file: kotlin.String?,
        //   __buf: kotlinx.cinterop.CValuesRef<platform.posix.stat>?)
        // : kotlin.Int { /* compiled code */ }
        return (lstat(filename, stat.ptr) == 0)
    }
}

// create new directoriess if not exist, starting at the topDir
fun createDirectories(dir: String): Boolean {
    val subdirs = dir.split("/")
    var have = ""
    subdirs.forEach {
        have += it
        if (!exists(have)) {
            println("try to createDirectory = '$have'")
            if (!createDirectory(have)) {
                return false;
            }
        }
        have += "/"
    }
    return true
}

fun createDirectory(dirName: String): Boolean {
    // mkdir(@kotlinx.cinterop.internal.CCall.CString __path: kotlin.String?,
    // __mode: platform.posix.__mode_t /* = kotlin.UInt */)
    // : kotlin.Int { /* compiled code */ }
    if (mkdir(dirName, 774U) == -1) {
        checkErrno { mess -> throw IOException("Fail mkdir $mess on $dirName") }
        return false
    }
    return true
}