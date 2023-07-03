package electionguard.publish

import io.ktor.utils.io.errors.*
import kotlinx.cinterop.*
import platform.posix.*

private const val debug = false

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
            if (debug) println("++++++++++cwd '${cwd?.toKString()}'")
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

fun checkErrno(dothis: (mess: String) -> Unit) {
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

fun exists(path: String): Boolean {
    memScoped {
        val stat = alloc<stat>()
        // lstat(@kotlinx.cinterop.internal.CCall.CString __file: kotlin.String?,
        //   __buf: kotlinx.cinterop.CValuesRef<platform.posix.stat>?)
        // : kotlin.Int { /* compiled code */ }
        return (lstat(path, stat.ptr) == 0)
    }
}

fun isdirectory(path: String): Boolean {
    memScoped {
        val stat = alloc<stat>()
        if (lstat(path, stat.ptr) != 0) { // does it exist?
            return false
        }
        return S_ISDIR(stat.st_mode)
    }
}

///////////
// these macros are not in the cinterop libraries, so we add them here
// see eg https://youtrack.jetbrains.com/issue/KT-43719/C-Interop-Support-function-like-macros
private val S_IFMT : UInt = "0170000".toUInt(8)
private val S_IFDIR : UInt = "0040000".toUInt(8)

// taken from ubuntu dist, /usr/include/x86_64-linux-gnu/[sys|bits]/stat.h
// hopefully its the same across posix distros

// S_ISDIR(m)	(((m) & S_IFMT) == S_IFDIR)	/* directory */
// S_IFMT          0170000         /* [XSI] type of file mask */
// S_IFDIR         0040000         /* [XSI] directory */
fun S_ISDIR(mode: UInt): Boolean {
    return (mode and S_IFMT) == S_IFDIR
}

/* File types.
    #define	__S_IFDIR	0040000	/* Directory.  */
    #define	__S_IFCHR	0020000	/* Character device.  */
    #define	__S_IFBLK	0060000	/* Block device.  */
    #define	__S_IFREG	0100000	/* Regular file.  */
    #define	__S_IFIFO	0010000	/* FIFO.  */
    #define	__S_IFLNK	0120000	/* Symbolic link.  */
    #define	__S_IFSOCK	0140000	/* Socket.  */
*/
///////////

// create new directories if not exist, starting at the topDir
fun createDirectories(dir: String): Boolean {
    val subdirs = dir.split("/")
    var have = ""
    subdirs.forEach {
        have += it
        if (!exists(have)) {
            if (debug) println("create directory = '$have'")
            if (!createDirectory(have)) {
                return false
            }
        }
        have += "/"
    }
    return true
}

/**
 * Converts a 32-bit unsigned integer, representing a file's `mode_t` into
 * the platform `mode_t` (which might be 16 bits or 32 bits, depending on
 * the platform).
 */
private inline fun UInt.toModeT(): mode_t {
    // The convert() method comes to us from kotlinx.cinterop and is a "typed intrinsic",
    // so it's getting the output type (mode_t) from the lexical context and is then
    // doing the desired conversion. What kind of conversion? That's not specified
    // anywhere, but it *probably* behaves like a C typecast, which is to say, it
    // just passes the data along without any bounds checking of any kind.
    return this.convert()
}

fun createDirectory(dirName: String): Boolean {
    // mkdir(@kotlinx.cinterop.internal.CCall.CString __path: kotlin.String?,
    // __mode: platform.posix.__mode_t /* = kotlin.UInt */)
    // : kotlin.Int { /* compiled code */ }
    if (mkdir(dirName, convertOctalToDecimal(775).toModeT()) == -1) {
        checkErrno { mess -> throw IOException("Fail mkdir $mess on $dirName") }
        return false
    }
    return true
}

fun convertOctalToDecimal(octal: Int): UInt {
    var work = octal
    var decimalNumber = 0
    var pow = 1

    while (work != 0) {
        decimalNumber += (work % 10 * pow)
        work /= 10
        pow *= 8
    }

    return decimalNumber.toUInt()
}

@Throws(IOException::class)
fun openDir(dirpath: String, suffix: String = ".json"): List<String> {
    memScoped {
        // opendir(
        //    @kotlinx.cinterop.internal.CCall.CString __name: kotlin.String?)
        // : kotlinx.cinterop.CPointer<platform.posix.DIR /* = cnames.structs.__dirstream */>? { /* compiled code */ }
        val dir: CPointer<DIR>? = opendir(dirpath)
        if (dir == null) {
            checkErrno { mess -> throw IOException("Fail opendir $mess on $dirpath") }
        }
        if (debug) println(" success opendir $dir from $dirpath")

        // readdir(
        //    __dirp: kotlinx.cinterop.CValuesRef<platform.posix.DIR /* = cnames.structs.__dirstream */>?)
        // : kotlinx.cinterop.CPointer<platform.posix.dirent>? { /* compiled code */ }
        val result = mutableListOf<String>()
        while (true) {
            val ddir: CPointer<dirent> = readdir(dir)
                ?: // this happens when no more files to be read
                break
            val dirent = ddir[0]
            val filenamep: CArrayPointer<ByteVar> = dirent.d_name
            val filename = filenamep.toKString()
            if (filename.endsWith(suffix)) {
                result.add(filename)
                if (debug) println(" success readdir filename= ${filenamep.toKString()}")
            }
        }
        return result
    }
}

fun openFile(abspath: String, modes: String): CPointer<FILE> {
    memScoped {
        // fopen(
        //       @kotlinx.cinterop.internal.CCall.CString __filename: kotlin.String?,
        //       @kotlinx.cinterop.internal.CCall.CString __modes: kotlin.String?)
        //       : kotlinx.cinterop.CPointer<platform.posix.FILE>?
        val file = fopen(abspath, modes)
        if (file == null) {
            checkErrno { mess -> throw IOException("Fail open $mess on $abspath") }
        }
        return file!!
    }
}

fun fgetsFile(filename: String): List<String> {
    val result = mutableListOf<String>()
    memScoped {
        val file: CPointer<FILE> = openFile(filename, "rb")
        val bufferLength = 255 // nice buffer overflow
        val linep: CValuesRef<ByteVar> = allocArray(bufferLength)

        // gets(
        // __s: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
        // __n: kotlin.Int,
        // __stream: kotlinx.cinterop.CValuesRef<platform.posix.FILE /* = platform.posix._IO_FILE */>?)
        // : kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>? { /* compiled code */ }
        while (fgets(linep, bufferLength, file) != null) {
            val s = linep.getPointer(memScope)
            val ks = s.toKString()
            result.add(ks)
        }

        fclose(file)
    }
    return result
}
