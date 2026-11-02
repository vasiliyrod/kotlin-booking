package tech.ecom.booking.integration

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import utils.DataBaseConfig
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

class GuestControllerIntegrationTest : BaseControllerIntegrationTest() {

    companion object {
        val findIdJsonRegex = Regex("\"id\":\"([^\"]+)\"")
    }

    @BeforeEach
    fun cleanDatabase() {
        val jdbcTemplate = DataBaseConfig.namedParameterJdbcTemplate
        jdbcTemplate.jdbcTemplate.execute("TRUNCATE TABLE guest RESTART IDENTITY CASCADE")
    }

    @Test
    fun `POST guest should create guest and return 201`() {
        // given
        val json =
            """{"surname": "Иванов", "firstname": "Иван", "lastname": "Иванович", "birthdate": "1990-01-15", "phoneNumber": "+79161234567"}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        // when
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(201, response.statusCode())
        assertTrue(response.body().contains("id"))
        assertTrue(response.body().contains("Иванов"))
        assertTrue(response.body().contains("Иван"))
        assertTrue(response.body().contains("+79161234567"))
    }

    @Test
    fun `POST guest with empty body should return 201`() {
        // given
        val json = """{}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        // when
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(201, response.statusCode())
        assertTrue(response.body().contains("id"))
    }

    @Test
    fun `POST guest with invalid data should return 400`() {
        // given
        val json = """{"surname": "", "firstname": "", "phoneNumber": "invalid"}"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()

        // when
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(400, response.statusCode())
    }

    @Test
    fun `GET guest by id should return 200`() {
        // given
        val createJson =
            """{"surname": "Кузнецов", "firstname": "Алексей", "lastname": "Петрович", "birthdate": "1985-06-20", "phoneNumber": "+79167890123"}"""
        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createJson))
            .build()
        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, createResponse.statusCode())
        val guestId = extractId(createResponse.body())

        val getRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest/$guestId"))
            .GET()
            .build()

        // when
        val response = client.send(getRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("Кузнецов"))
        assertTrue(response.body().contains("Алексей"))
        assertTrue(response.body().contains("+79167890123"))
    }

    @Test
    fun `GET guest with minimal data should return 200`() {
        // given
        val createJson = """{}"""
        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createJson))
            .build()
        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, createResponse.statusCode())
        val guestId = extractId(createResponse.body())

        val getRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest/$guestId"))
            .GET()
            .build()

        // when
        val response = client.send(getRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains(guestId))
    }

    @Test
    fun `GET guest not found should return 404`() {
        // given
        val nonExistentId = UUID.randomUUID()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest/$nonExistentId"))
            .GET()
            .build()

        // when
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(404, response.statusCode())
    }

    @Test
    fun `PUT guest should update and return 200`() {
        // given
        val createJson = """{}"""
        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createJson))
            .build()
        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, createResponse.statusCode())
        val guestId = extractId(createResponse.body())

        val updateJson =
            """{"id": "$guestId", "surname": "Попов", "firstname": "Дмитрий", "lastname": "Сергеевич", "birthdate": "1990-03-15", "phoneNumber": "+79167778899"}"""
        val updateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
            .build()

        // when
        val response = client.send(updateRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("Попов"))
        assertTrue(response.body().contains("Дмитрий"))
        assertTrue(response.body().contains("+79167778899"))
    }


    @Test
    fun `PUT guest not found should return 404`() {
        // given
        val nonExistentId = UUID.randomUUID()
        val updateJson =
            """{"id": "$nonExistentId", "surname": "Неизвестный", "firstname": "Гость", "phoneNumber": "+79169999999"}"""
        val updateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
            .build()

        // when
        val response = client.send(updateRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(404, response.statusCode())
    }

    @Test
    fun `PUT guest without id should return 400`() {
        // given
        val updateJson = """{"surname": "Новый", "firstname": "Гость", "phoneNumber": "+79169999999"}"""
        val updateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
            .build()

        // when
        val response = client.send(updateRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(400, response.statusCode())
    }

    @Test
    fun `PUT guest with invalid data should return 400`() {
        // given
        // Добавляем гостя
        val createJson = """{"surname": "Федоров", "firstname": "Федор", "phoneNumber": "+79161112233"}"""
        val createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createJson))
            .build()
        val createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, createResponse.statusCode())
        val guestId = extractId(createResponse.body())

        // Невалидные данные
        val updateJson = """{"id": "$guestId", "surname": "", "firstname": ""}"""
        val updateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/guest"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
            .build()

        // when
        val response = client.send(updateRequest, HttpResponse.BodyHandlers.ofString())

        // then
        assertEquals(400, response.statusCode())
    }

    private fun extractId(responseBody: String): String {
        return findIdJsonRegex.find(responseBody)?.groupValues?.get(1) ?: ""
    }
}