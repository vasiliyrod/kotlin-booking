package tech.ecom.booking.utils

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtil {
    private val iterations = 310000
    private val keyLength = 256
    private val algorithm = "PBKDF2WithHmacSHA256"

    fun hashPassword(rawPassword: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(rawPassword.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance(algorithm)
        val hash = factory.generateSecret(spec).encoded

        // Сохраняем соль + хэш вместе
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        return "$iterations:${saltBase64}:$hashBase64"
    }

    fun verifyPassword(rawPassword: String, hashedPassword: String): Boolean {
        val parts = hashedPassword.split(":")
        val iterations = parts[0].toInt()
        val salt = Base64.getDecoder().decode(parts[1])
        val expectedHash = Base64.getDecoder().decode(parts[2])

        val spec = PBEKeySpec(rawPassword.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance(algorithm)
        val actualHash = factory.generateSecret(spec).encoded

        return actualHash.contentEquals(expectedHash)
    }
}