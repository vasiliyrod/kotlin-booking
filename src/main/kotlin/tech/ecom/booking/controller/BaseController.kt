package controller

import com.sun.net.httpserver.HttpExchange
import dto.OperationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import tech.ecom.booking.dto.serializers.LocalDateSerializer
import tech.ecom.booking.dto.serializers.LocalDateTimeSerializer
import tech.ecom.booking.dto.serializers.UuidSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

abstract class BaseController {
    companion object {
        val findIdRegex = Regex("""/(\d+)""")
        val findUuidRegex = Regex("""/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$""")
    }

    protected val logger = KotlinLogging.logger { }

    protected val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UuidSerializer)
            contextual(LocalDate::class, LocalDateSerializer)
            contextual(LocalDateTime::class, LocalDateTimeSerializer)
        }
    }

    abstract fun configureHandlers(router: Router)

    protected fun extractIntId(httpExchange: HttpExchange): Int? {
        val path = httpExchange.requestURI.path
        val matchResult = findIdRegex.find(path)
        val id = matchResult?.groupValues?.get(1)?.toIntOrNull()

        return id
    }

    protected fun extractUuid(httpExchange: HttpExchange): UUID? {
        val path = httpExchange.requestURI.path
        val matchResult = findUuidRegex.find(path)
        val uuidString = matchResult?.groupValues?.get(1)

        return uuidString?.let { UUID.fromString(it) }
    }

    protected fun getCredentialsFromToken(httpExchange: HttpExchange) {

    }

    protected fun extractQueryParams(httpExchange: HttpExchange): Map<String, String> {
        val query = httpExchange.requestURI.query ?: return emptyMap()
        return query.split("&")
            .filter { it.isNotBlank() }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }
    }

    protected inline fun <reified T> parseRequestBody(httpExchange: HttpExchange): OperationResult<T> {
        val bodyString: String
        httpExchange.requestBody.bufferedReader(Charsets.UTF_8).use {
            bodyString = it.readText()
        }
        logger.trace { "Request body: $bodyString" }
        try {
            val data = json.decodeFromString<T>(bodyString)

            return OperationResult.Success<T>(data)

        } catch (e: SerializationException) {
            logger.error { "Failed to parse request body: $bodyString" }
            return OperationResult.badRequest("Failed to parse request body")

        } catch (e: IllegalArgumentException) {
            logger.warn {
                "Invalid data format. Failed to parse request body as JSON: $bodyString"
            }
            return OperationResult.badRequest("Invalid data format. Failed to parse request body as JSON")
        }
    }

    protected inline fun <reified T> writeResultSuccess(
        httpExchange: HttpExchange,
        result: OperationResult.Success<T>
    ) {
        try {
            val jsonString = json.encodeToString(result.data)
            logger.info { "Sending response $jsonString" }

            httpExchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")

            val responseBytes = jsonString.toByteArray(Charsets.UTF_8)
            httpExchange.sendResponseHeaders(result.statusCode, responseBytes.size.toLong())

            httpExchange.responseBody.use { outputStream ->
                outputStream.write(responseBytes)
                outputStream.flush()
            }

        } catch (e: SerializationException) {
            logger.error(e) { "Serialization error for ${result.data}" }
            val errorJson = """{"error": "Serialization error: ${e.message}"}"""
            val errorBytes = errorJson.toByteArray(Charsets.UTF_8)
            httpExchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
            httpExchange.sendResponseHeaders(500, errorBytes.size.toLong())
            httpExchange.responseBody.write(errorBytes)

        } catch (e: Exception) {
            logger.error(e) { "Unexpected error while sending response" }
        }
    }

    protected fun writeResultError(httpExchange: HttpExchange, result: OperationResult.Error) {
        val jsonString = json.encodeToString(mapOf("error" to result.message))
        logger.info { "Sending response $jsonString" }

        if (result.statusCode >= 500) {
            logger.error { "Server error: ${result.statusCode}, ${result.message}" }
        } else if (result.statusCode >= 400) {
            logger.warn { "Client error: ${result.statusCode}, ${result.message}" }
        }

        httpExchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")

        val responseBytes = jsonString.toByteArray(Charsets.UTF_8)
        httpExchange.sendResponseHeaders(result.statusCode, responseBytes.size.toLong())

        httpExchange.responseBody.use { outputStream ->
            outputStream.write(responseBytes)
            outputStream.flush()
        }
    }
}