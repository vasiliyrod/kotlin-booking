package tech.ecom.booking.dto.auth

import kotlinx.serialization.Serializable
import tech.ecom.booking.dto.ValidationResult

@Serializable
data class AuthLoginDto(
    val phoneNumber: String,
    val password: String
) {
    companion object {
        private val phoneRegex = Regex("^\\+?[0-9]{10,15}$")
        private val phoneCleanRegex = Regex("[\\s\\-()]")
        private val passwordRegex = Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$")
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (phoneNumber.isBlank()) {
            errors.add("Phone number cannot be empty")
        } else {
            val cleanedPhone = phoneNumber.replace(phoneCleanRegex, "")
            if (!phoneRegex.matches(cleanedPhone)) {
                errors.add("Invalid phone number format")
            }
        }

        if (password.length < 6) {
            errors.add("Password must be at least 6 characters")
        } else if (password.length > 30) {
            errors.add("Password must not exceed 30 characters")
        } else if (!passwordRegex.matches(password)) {
            errors.add("Password can only contain: Latin letters," +
                    " digits and special characters (! @ # \$ % ^ & * ( ) _ + - = [ ] { } ; : ' \" , . < > / ? \\ |)")
        }

        return if (errors.isEmpty())
            ValidationResult.Companion.success()
        else
            ValidationResult.Companion.errors(errors)
    }
}
