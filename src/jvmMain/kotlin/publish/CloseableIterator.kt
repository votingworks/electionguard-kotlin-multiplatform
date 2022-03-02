package publish

import java.io.Closeable
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * An iterator that must be closed.
 *
 *
 * <pre>
 * void method(CloseableIterable&lt;T&gt; ballotsIterable) {
 * try (CloseableIterator&lt;T&gt; ballotsIter = ballotsIterable.iterator()) {
 * while (ballotsIter.hasNext()) {
 * T ballot = ballotsIter.next();
 * // do stuff
 * }
 * }
 * }
</pre> *
 * or
 *
 *
 * <pre>
 * void method(CloseableIterator&lt;T&gt; ballotsIter) {
 * try (Stream&lt;T&gt; ballotsStream = ballotsIter.stream()) {
 * ballotsStream.filter(b -&gt; b.state == State.CAST)
 * .forEach(ballot -&gt; {
 * // do stuff
 * });
 * }
 * }
</pre> *
 *
 * The convention is that if CloseableIterator is passed to a method, it is not already in a try-finally block.
 *
 *
 * @see [Spring CloseableIterator](https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/util/CloseableIterator.java)
 */
interface CloseableIterator<T> : Iterator<T>, Closeable {
    /** IOException is converted to RuntimeException.  */
    override fun close()

    /** Could be parallel.  */
    fun spliterator(): Spliterator<T>? {
        return Spliterators.spliterator(this, 0, 0)
    }

    /** Convert to a stream, can be declared as a resource in a try-with-resources statement.  */
    fun stream(): Stream<T>? {
        return StreamSupport.stream(spliterator(), false).onClose { close() }
    }
}

interface CloseableIterable<T> : Iterable<T> {
    override fun iterator(): CloseableIterator<T>
}