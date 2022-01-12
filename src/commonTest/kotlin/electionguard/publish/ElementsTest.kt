package electionguard.publish

import electionguard.core.Base16.fromHex
import electionguard.core.elementsModP
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.runTest
import io.kotest.property.checkAll
import kotlin.test.*
import kotlinx.serialization.json.*

private inline fun <reified T> jsonRoundTrip(value: T): T {
    val jsonT: JsonElement = Json.encodeToJsonElement(value)

    if (jsonT is JsonPrimitive) {
        assertTrue(jsonT.isString)
        assertNotNull(jsonT.content.fromHex()) // validates that we have a base16 string
    } else {
        fail("expected jsonT to be JsonPrimitive")
    }

    val jsonS = jsonT.toString()
    val backToJ: JsonElement = Json.parseToJsonElement(jsonS)
    val backToT: T = Json.decodeFromJsonElement(backToJ)
    return backToT
}

class ElementTest {
    @Test
    fun importExportForElements() {
        runTest {
            val context = productionGroup()
            checkAll(elementsModP(context), elementsModQ(context)) { p, q ->
                // shorter round-trip from the core classes to JsonElement and back
                assertEquals(p, context.importElementModP(p.publishJson()))
                assertEquals(q, context.importElementModQ(q.publishJson()))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, context.import(jsonRoundTrip(p.publish())))
                assertEquals(q, context.import(jsonRoundTrip(q.publish())))
            }
        }
    }
}