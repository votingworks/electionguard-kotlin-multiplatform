package electionguard.core

/* Keep track of timing stats. Thread-safe */
class Stats() {
    private val stats = mutableMapOf<String, Stat>() // LOOK need thread safe collection

    fun of(who : String, thing: String = "decryption", what: String = "ballot") : Stat = stats.getOrPut(who) { Stat(thing, what) }

    fun show() {
        if (stats.isEmpty()) {
            println("stats is empty")
            return
        }
        var sum = 0L
        stats.forEach {
            println("${it.key.padStart(20, ' ')}: ${it.value.show()}")
            sum += it.value.accum()
        }
        val total = stats.values.first().copy(sum)
        val totalName = "total".padStart(20, ' ')
        println("$totalName: ${total.show()}")
    }
}

expect class Stat(thing : String, what: String) {
    fun thing(): String
    fun what(): String
    fun accum(amount : Long, nthings : Int)
    fun accum(): Long
    fun nthings(): Int
    fun count(): Int
    fun copy(accum: Long): Stat
}

fun Stat.show(): String {
    val perThing = accum().toDouble()/nthings()
    val perWhat = accum().toDouble()/count()
    return "took ${accum()} msecs = ${perThing} msecs/${thing()} (${nthings()} ${thing()}s) = ${perWhat} msecs/${what()} for ${count()} ${what()}s"
}