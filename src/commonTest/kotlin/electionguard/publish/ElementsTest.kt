package electionguard.publish

import electionguard.core.*
import electionguard.core.Base16.fromHex
import io.kotest.property.checkAll
import kotlin.test.*
import kotlinx.serialization.json.*

inline fun <reified T> jsonRoundTripWithStringPrimitive(value: T): T {
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
    fun importExportForProductionElements() {
        runTest {
            val context = productionGroup()
            checkAll(elementsModP(context), elementsModQ(context)) { p, q ->
                // shorter round-trip from the core classes to JsonElement and back
                assertEquals(p, context.importElementModP(p.publishJson()))
                assertEquals(q, context.importElementModQ(q.publishJson()))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, context.import(jsonRoundTripWithStringPrimitive(p.publish())))
                assertEquals(q, context.import(jsonRoundTripWithStringPrimitive(q.publish())))
            }
        }
    }
    @Test
    fun importExportForTinyElements() {
        runTest {
            val context = tinyGroup()
            checkAll(elementsModP(context), elementsModQ(context)) { p, q ->
                // shorter round-trip from the core classes to JsonElement and back
                assertEquals(p, context.importElementModP(p.publishJson()))
                assertEquals(q, context.importElementModQ(q.publishJson()))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, context.import(jsonRoundTripWithStringPrimitive(p.publish())))
                assertEquals(q, context.import(jsonRoundTripWithStringPrimitive(q.publish())))
            }
        }
    }
}