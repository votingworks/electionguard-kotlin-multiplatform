package webapps.electionguard

import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import webapps.electionguard.plugins.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }
        client.get("/ktrustee").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("No guardians found", bodyAsText())
        }
    }
}