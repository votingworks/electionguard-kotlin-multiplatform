package electionguard.util

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** So we can use AtomicXXX */
actual class Stat actual constructor(
    val thing : String,
    val what: String
) {
    var accum : AtomicLong = AtomicLong(0)
    var count : AtomicInteger = AtomicInteger(0)
    var nthings : AtomicInteger = AtomicInteger(0)

    actual fun accum(amount : Long, nthings : Int) {
        accum.addAndGet(amount)
        this.nthings.addAndGet(nthings)
        count.incrementAndGet()
    }

    actual fun copy(accum: Long): Stat {
        val copy = Stat(this.thing, this.what)
        copy.accum = AtomicLong(accum)
        copy.count = this.count
        copy.nthings = this.nthings
        return copy
    }

    actual fun thing() = this.thing

    actual fun what() = this.what

    actual fun accum() = this.accum.get()

    actual fun nthings() = this.nthings.get()

    actual fun count() = this.count.get()
}