package electionguard.json2

import electionguard.core.*
import electionguard.core.Base16.fromHex
import io.kotest.property.checkAll
import kotlin.test.*
import kotlinx.serialization.json.*


inline fun <reified T> jsonRoundTripWithStringPrimitive(value: T): T {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    val jsonT: JsonElement = jsonReader.encodeToJsonElement(value)

    if (jsonT is JsonPrimitive) {
        assertTrue(jsonT.isString)
        assertNotNull(jsonT.content.fromHex()) // validates that we have a base16 string
    } else {
        fail("expected jsonT to be JsonPrimitive")
    }

    val jsonS = jsonT.toString()
    val backToJ: JsonElement = jsonReader.parseToJsonElement(jsonS)
    val backToT: T = jsonReader.decodeFromJsonElement(backToJ)
    return backToT
}

class ElementsTest {
    @Test
    fun testElementRoundtrip() {
        runTest {
            val group = productionGroup()
            checkAll(elementsModP(group), elementsModQ(group)) { p, q ->
                assertEquals(p, p.publishJson().import(group))
                assertEquals(q, q.publishJson().import(group))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, jsonRoundTripWithStringPrimitive(p.publishJson()).import(group))
                assertEquals(q, jsonRoundTripWithStringPrimitive(q.publishJson()).import(group))
            }
        }
    }

    @Test
    fun importTinyElements() {
        runTest {
            val group = tinyGroup()
            checkAll(elementsModP(group), elementsModQ(group)) { p, q ->
                // shorter round-trip from the core classes to JsonElement and back
                assertEquals(p, p.publishJson().import(group))
                assertEquals(q, q.publishJson().import(group))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, jsonRoundTripWithStringPrimitive(p.publishJson()).import(group))
                assertEquals(q, jsonRoundTripWithStringPrimitive(q.publishJson()).import(group))
            }
        }
    }

    @Test
    fun testUInt256Roundtrip() {
        runTest {
            val context = productionGroup()
            checkAll(elementsModQ(context)) { q ->
                val u : UInt256 = q.toUInt256safe()
                assertEquals(u, u.publishJson().import())
                assertEquals(u, jsonRoundTripWithStringPrimitive(u.publishJson()).import())
            }
        }
    }
}