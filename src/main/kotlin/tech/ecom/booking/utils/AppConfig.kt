package tech.ecom.booking.utils

import io.github.cdimascio.dotenv.dotenv
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey

object AppConfig {
    private val env = System.getenv()
    private val dotenv = dotenv()

    val serverPort: Int = env["SERVER_PORT"]?.toIntOrNull() ?: 8080
    val dbUrl: String = System.getProperty("DB_URL") ?: dotenv["DB_URL"]
    val dbPassword: String = System.getProperty("DB_PASSWORD") ?: dotenv["DB_PASSWORD"]
    val dbUser: String = System.getProperty("DB_USER") ?: dotenv["DB_USER"]

    val jwtSecretKey: SecretKey = Keys.hmacShaKeyFor(
        (System.getProperty("JWT_SECRET_KEY") ?: dotenv["JWT_SECRET_KEY"]).toByteArray()
    )

    val accessTokenTtlMs: Long =
        System.getProperty("ACCESS_TOKEN_TTL_MS")?.toLongOrNull() ?: dotenv["ACCESS_TOKEN_TTL_MS"]?.toLongOrNull()
        ?: (15 * 60 * 1000L)

    val refreshTokenTtlDays: Long =
        System.getProperty("REFRESH_TOKEN_TTL_DAYS")?.toLongOrNull() ?: dotenv["REFRESH_TOKEN_TTL_DAYS"].toLongOrNull()
        ?: (30L)

}