package electionguard.core

import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicLong

actual class Stat actual constructor(
    val thing : String,
    val what: String
) {
    var accum : AtomicLong = AtomicLong(0)
    var count : AtomicInt = AtomicInt(0)
    var nthings : AtomicInt = AtomicInt(0)

    actual fun accum(amount : Long, nthings : Int) {
        accum.addAndGet(amount)
        this.nthings.addAndGet(nthings)
        count.increment()
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

    actual fun accum() = this.accum.value

    actual fun nthings() = this.nthings.value

    actual fun count() = this.count.value
}