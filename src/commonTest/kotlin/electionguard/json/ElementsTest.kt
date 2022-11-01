package electionguard.json

import electionguard.core.*
import electionguard.core.Base16.fromHex
import electionguard.serialize.import
import electionguard.serialize.importElementModP
import electionguard.serialize.importElementModQ
import electionguard.serialize.publish
import electionguard.serialize.publishJson
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

class ElementsTest {
    @Test
    fun testElementRoundtrip() {
        runTest {
            val context = productionGroup()
            checkAll(elementsModP(context), elementsModQ(context)) { p, q ->
                assertEquals(p, context.importModP(p.publishModP()))
                assertEquals(q, context.importModQ(q.publishModQ()))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, context.importModP(jsonRoundTripWithStringPrimitive(p.publishModP())))
                assertEquals(q, context.importModQ(jsonRoundTripWithStringPrimitive(q.publishModQ())))
            }
        }
    }

    @Test
    fun importTinyElements() {
        runTest {
            val context = tinyGroup()
            checkAll(elementsModP(context), elementsModQ(context)) { p, q ->
                // shorter round-trip from the core classes to JsonElement and back
                assertEquals(p, context.importElementModP(p.publishJson()))
                assertEquals(q, context.importElementModQ(q.publishJson()))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, context.import(electionguard.serialize.jsonRoundTripWithStringPrimitive(p.publish())))
                assertEquals(q, context.import(electionguard.serialize.jsonRoundTripWithStringPrimitive(q.publish())))
            }
        }
    }

    @Test
    fun testUInt256Roundtrip() {
        runTest {
            val context = productionGroup()
            checkAll(elementsModQ(context)) { q ->
                val u : UInt256 = q.toUInt256()
                assertEquals(u, u.publish().import())
                assertEquals(u, jsonRoundTripWithStringPrimitive(u.publish()).import())
            }
        }
    }
}