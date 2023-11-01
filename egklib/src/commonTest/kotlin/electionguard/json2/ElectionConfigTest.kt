package electionguard.json2

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionConstants
import electionguard.core.Base16.fromHex
import electionguard.core.UInt256
import electionguard.core.productionGroup
import electionguard.util.ErrorMessages
import io.kotest.assertions.throwables.shouldThrowUnitWithMessage
import kotlin.test.*

class ElectionConfigTest {
    val group =  productionGroup()

    @Test
    fun roundtripTest() {
        val constants = group.constants
        val manifestBytes = "1234ABC".fromHex()!!
        val config = makeConfig(constants, manifestBytes)
        val json = config.publishJson()
        val errs = ErrorMessages("roundtripTest")
        val subject = json.import(constants, manifestBytes, errs)
        assertNotNull(subject)
        assertFalse(errs.hasErrors())
        assertEquals(config, subject)
    }

    fun makeConfig(constants: ElectionConstants, manifestBytes: ByteArray) = ElectionConfig(
        "configVersion",
        constants,
        7, 4,
        UInt256.random(),
        UInt256.random(),
        UInt256.random(),
        manifestBytes,
        true,
        "ABCDEF".fromHex()!!,
        mapOf(),
    )

    @Test
    fun badField1Test() {
        val constants = group.constants
        val manifestBytes = "1234ABC".fromHex()!!
        val config = makeConfig(constants, manifestBytes)
        val json = config.publishJson()

        val badjson = json.copy(parameter_base_hash = UInt256Json(manifestBytes))
        val errs = ErrorMessages("")
        val subject = badjson.import(constants, manifestBytes, errs)
        assertNull(subject)
        assertTrue(errs.hasErrors())
        assertTrue(errs.contains("malformed parameter_base_hash"))
    }

    @Test
    fun badField2Test() {
        val constants = group.constants
        val manifestBytes = "1234ABC".fromHex()!!
        val config = makeConfig(constants, manifestBytes)
        val json = config.publishJson()

        val badjson = json.copy(manifest_hash = UInt256Json(manifestBytes))
        val errs = ErrorMessages("")
        val subject = badjson.import(constants, manifestBytes, errs)
        assertNull(subject)
        assertTrue(errs.hasErrors())
        assertTrue(errs.contains("malformed manifest_hash"))
    }

    @Test
    fun badField3Test() {
        val constants = group.constants
        val manifestBytes = "1234ABC".fromHex()!!
        val config = makeConfig(constants, manifestBytes)
        val json = config.publishJson()

        val badjson = json.copy(election_base_hash = UInt256Json(manifestBytes))
        val errs = ErrorMessages("")
        val subject = badjson.import(constants, manifestBytes, errs)
        assertNull(subject)
        assertTrue(errs.hasErrors())
        assertTrue(errs.contains("malformed election_base_hash"))
    }

}