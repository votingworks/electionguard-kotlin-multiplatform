package electionguard.json2

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.unwrap
import electionguard.core.productionGroup
import electionguard.util.ErrorMessages
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnitWithMessage
import java.lang.RuntimeException
import kotlin.test.*

class ElectionConstantsTest {
    @Test
    fun badFieldsTest() {
        val errs = ErrorMessages("badFieldsTest")
        var subject = ElectionConstantsJson("any", "any", "any", "any", "any")
            .import(errs)
        assertNull(subject)
        assertTrue(errs.contains("malformed large_prime"))

        subject = ElectionConstantsJson("any", "29", "any", "any", "any")
            .import(errs)
        assertNull(subject)
        assertTrue(errs.contains("malformed small_prime"))

        subject = ElectionConstantsJson("any", "29", "5", "any", "any")
            .import(errs)
        assertNull(subject)
        assertTrue(errs.contains("malformed cofactor"))

        subject = ElectionConstantsJson("any", "29", "5", "17", "any")
            .import(errs)
        assertNull(subject)
        assertTrue(errs.contains("malformed generator"))
    }

    @Test
    fun goodTest() {
        val good = ElectionConstantsJson("any", "29", "5", "17", "95")
            .import(ErrorMessages("goodTest"))
        assertNotNull(good)
    }

    @Test
    fun roundtripTest() {
        val group = productionGroup()
        val json = group.constants.publishJson()
        val subject = json.import(ErrorMessages("roundtripTest"))
        assertNotNull(subject)
        assertEquals(group.constants, subject)
    }

}