package tech.ecom.booking.integration

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import utils.DataBaseConfig
import java.net.URI

import java.net.http.HttpRequest
import java.net.http.HttpResponse


class RoomControllerIntegrationTest : BaseControllerIntegrationTest() {

    companion object {
        val findIdJsonRegex = Regex("\"id\":(\\d+)")
    }

    @BeforeEach
    fun cleanDatabase() {
        val jdbcTemplate = DataBaseConfig.namedParameterJdbcTemplate
        jdbcTemplate.jdbcTemplate.execute("TRUNCATE TABLE room RESTART IDENTITY CASCADE")
    }

    @Test
    fun `POST room should create room and return 201`() {
        // given
        val json = """{"floor": 1, "number": "101", "capacity": 4}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        // when
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(201, response.statusCode())
        assertTrue(response.body().contains("id"))
        assertTrue(response.body().contains("101"))
    }

    @Test
    fun `POST room duplicate should return 409`() {
        // given
        val firstJson = """{"floor": 1, "number": "101", "capacity": 4}"""
        val firstRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(firstJson))
            .build()
        val firstResponse = client.send(firstRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, firstResponse.statusCode())

        // Дубликат комнаты
        val duplicateJson = """{"floor": 1, "number": "101", "capacity": 6}"""
        val duplicateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(duplicateJson))
            .build()

        // when
        val duplicateResponse = client.send(duplicateRequest, HttpResponse.BodyHandlers.ofString())

        //then
        assertEquals(409, duplicateResponse.statusCode())
    }

    @Test
    fun `GET room by id should return 200`() {
        // given
        val createJson = """{"floor": 2, "number": "202", "capacity": 3}"""
        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createJson))
            .build()
        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        val roomId = extractId(createResponse.body())

        val getRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room/$roomId"))
            .GET()
            .build()

        // when
        val response = client.send(getRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("202"))
    }

    @Test
    fun `GET room not found should return 404`() {
        // given
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room/99999"))
            .GET()
            .build()

        // when
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        //then
        assertEquals(404, response.statusCode())
    }

    @Test
    fun `PUT room should update and return 200`() {
        // given
        val createJson = """{"floor": 1, "number": "101", "capacity": 4}"""
        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createJson))
            .build()
        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        val roomId = extractId(createResponse.body())

        val updateJson = """{"id": $roomId, "floor": 3, "number": "303", "capacity": 6}"""
        val updateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/room"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
            .build()

        // when
        val response = client.send(updateRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("303"))
    }

    private fun extractId(responseBody: String): Int {
        return findIdJsonRegex.find(responseBody)?.groupValues?.get(1)?.toInt() ?: -1
    }
}