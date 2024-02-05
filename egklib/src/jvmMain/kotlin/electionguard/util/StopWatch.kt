package electionguard.util

import java.util.concurrent.TimeUnit

// adapted from Guava's Stopwatch
class Stopwatch(running: Boolean = true) {
    private var isRunning = false
    private var elapsedNanos: Long = 0
    private var startTick: Long = 0

    init {
        if (running) start()
    }

    fun start(): Stopwatch {
        elapsedNanos = 0
        isRunning = true
        startTick = System.nanoTime()
        return this
    }

    fun stop(): Long {
        val tick: Long = System.nanoTime()
        isRunning = false
        elapsedNanos += tick - startTick
        return elapsedNanos
    }

    private fun elapsedNanos(): Long {
        return if (isRunning) System.nanoTime() - startTick + elapsedNanos else elapsedNanos
    }

    // TimeUnit.SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS
    fun elapsed(desiredUnit: TimeUnit): Long {
        return desiredUnit.convert(elapsedNanos(), TimeUnit.NANOSECONDS)
    }

    override fun toString(): String {
        val nanos = elapsedNanos()
        val unit = chooseUnit(nanos)
        val value = nanos.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit)
        // Too bad this functionality is not exposed as a regular method call
        return String.format("%.4g %s", value, abbreviate(unit))
    }

    companion object {

        fun took(took: Long): String {
            val tookMs = took / 1_000_000
            return "took ${tookMs} ms"
        }

        fun perRow(took: Long, nrows: Int): String {
            val tookMs = took / 1_000_000
            val perRow = tookMs.toDouble()  / nrows
            return "took ${tookMs} ms for $nrows rows, ${perRow.sigfig(2)} ms per row"
        }

        // TODO units option
        fun ratio(num: Long, den: Long): String {
            val ratio = num.toDouble() / den
            val numValue = num / 1_000_000
            val denValue = den / 1_000_000
            return "$numValue / $denValue ms =  ${ratio.sigfig(2)}"
        }

        fun perRow(num: Long, den: Long, nrows: Int): String {
            val numValue = num.toDouble() / nrows / 1_000_000
            val denValue = den.toDouble() / nrows / 1_000_000
            return "${numValue.sigfig(2)} / ${denValue.sigfig(2)} ms per row"
        }

        fun ratioAndPer(num: Long, den: Long, nrows: Int): String {
            return "${ratio(num, den)};  ${perRow(num, den, nrows)}"
        }

        private fun chooseUnit(nanos: Long): TimeUnit {
            if (TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                return TimeUnit.SECONDS
            }
            if (TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                return TimeUnit.MILLISECONDS
            }
            if (TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                return TimeUnit.MICROSECONDS
            }
            return TimeUnit.NANOSECONDS
        }

        private fun abbreviate(unit: TimeUnit): String {
            return when (unit) {
                TimeUnit.NANOSECONDS -> "ns"
                TimeUnit.MICROSECONDS -> "\u03bcs" // μs
                TimeUnit.MILLISECONDS -> "ms"
                TimeUnit.SECONDS -> "s"
                else -> throw AssertionError()
            }
        }
    }
}