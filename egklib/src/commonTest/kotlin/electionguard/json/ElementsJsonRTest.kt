package electionguard.json

import electionguard.core.*
import io.kotest.property.checkAll
import kotlin.test.*
import kotlinx.serialization.json.*

class ElementsJsonRTest {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    // we need this to actually test from teh string form and back
    inline fun <reified T> jsonRoundTripWithStringPrimitive(value: T): T {
        val jsonT: JsonElement = jsonReader.encodeToJsonElement(value)

        if (jsonT is JsonPrimitive) {
            assertTrue(jsonT.isString)
        } else {
            fail("expected jsonT to be JsonPrimitive")
        }

        val jsonS = jsonT.toString()
        val backToJ: JsonElement = jsonReader.parseToJsonElement(jsonS)
        val backToT: T = jsonReader.decodeFromJsonElement(backToJ)
        return backToT
    }

    @Test
    fun testElementProundtrip() {
        runTest {
            val group = productionGroup()
            checkAll(elementsModP(group)) { p ->
                assertEquals(p, p.publishJsonR().import(group))
                assertEquals(p, jsonRoundTripWithStringPrimitive(p.publishJsonR()).import(group))
            }
        }
    }

    @Test
    fun testElementQroundtrip() {
        runTest {
            val group = productionGroup()
            checkAll(elementsModQ(group)) { q ->
                assertEquals(q, q.publishJsonR().import(group))
                assertEquals(q, jsonRoundTripWithStringPrimitive(q.publishJsonR()).import(group))
            }
        }
    }

    @Test
    fun testUInt256roundtrip() {
        runTest {
            val context = productionGroup()
            checkAll(elementsModQ(context)) { q ->
                val u : UInt256 = q.toUInt256safe()
                assertEquals(u, u.publishJsonR().import())
                assertEquals(u, jsonRoundTripWithStringPrimitive(u.publishJsonR()).import())
            }
        }
    }
}