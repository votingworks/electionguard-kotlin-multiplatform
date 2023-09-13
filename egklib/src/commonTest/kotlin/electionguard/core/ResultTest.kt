import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.partition
import kotlin.test.Test

class ResultTest {

    @Test
    fun testPartition() {
        val results = makeResults()
        val (values, errors) = results.partition()
        println("values = $values")
        println("errors = $errors")
    }

    fun makeResults() : List<Result<Int, String>> =
        mutableListOf(
            Ok(1),
            Ok(2),
            Ok(3),
            Err("4"),
            Err("5"),
        )
}