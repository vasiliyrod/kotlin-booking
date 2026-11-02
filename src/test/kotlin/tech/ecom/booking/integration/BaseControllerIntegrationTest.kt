package tech.ecom.booking.integration

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tech.ecom.booking.startServer

import java.net.http.HttpClient

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseControllerIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
    }

    protected lateinit var server: HttpServer
    protected lateinit var client: HttpClient
    protected var port: Int = 0

    @BeforeAll
    fun setUp() {
        // Настраиваем DataBaseConfig на тестовый контейнер
        System.setProperty("DB_URL", postgres.jdbcUrl)
        System.setProperty("DB_USER", postgres.username)
        System.setProperty("DB_PASSWORD", postgres.password)

        server = startServer(0)

        port = server.address.port

        client = HttpClient.newHttpClient()
    }

    @AfterAll
    fun tearDown() {
        server.stop(0)
    }
}