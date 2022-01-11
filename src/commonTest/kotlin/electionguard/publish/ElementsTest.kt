package electionguard.publish

import electionguard.core.elementsModP
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.runTest
import io.kotest.property.checkAll
import kotlin.test.*
import kotlinx.serialization.json.*

private inline fun <reified T> jsonRoundTrip(value: T): T {
    val jsonT: JsonElement = Json.encodeToJsonElement(value)

    // while we're here, verify that the JSON is an "object" with a single key, "value"
    // and a string associated with it

    if (jsonT is JsonObject) {
        assertEquals(setOf("value"), jsonT.keys)
        val expectedString = jsonT["value"] ?: fail("won't ever fail")
        assertTrue(expectedString.jsonPrimitive.isString)
    } else {
        fail("expected jsonT to be JsonObject")
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
                // short round-trip from the core classes to the publish classes
                assertEquals(p, context.import(p.publish()))
                assertEquals(q, context.import(q.publish()))

                // longer round-trip through JSON strings and back
                assertEquals(p, context.import(jsonRoundTrip(p.publish())))
                assertEquals(q, context.import(jsonRoundTrip(q.publish())))
            }
        }
    }
}