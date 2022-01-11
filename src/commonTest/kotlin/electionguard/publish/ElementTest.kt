package electionguard.publish

import electionguard.core.elementsModP
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.core.runTest
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

inline fun <reified T> jsonRoundTrip(value: T): T {
    val jsonT: JsonElement = Json.encodeToJsonElement(value)
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