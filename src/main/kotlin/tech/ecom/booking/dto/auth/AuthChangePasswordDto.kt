package tech.ecom.booking.dto.auth

import kotlinx.serialization.Serializable
import tech.ecom.booking.dto.ValidationResult

@Serializable
data class AuthChangePasswordDto(
    val phoneNumber: String,
    val oldPassword: String,
    val newPassword: String
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

        if (oldPassword.length < 6) {
            errors.add("Old password must be at least 6 characters")
        } else if (oldPassword.length > 30) {
            errors.add("Old password must not exceed 30 characters")
        } else if (!passwordRegex.matches(oldPassword)) {
            errors.add("Old password can only contain: Latin letters," +
                    " digits and special characters (! @ # \$ % ^ & * ( ) _ + - = [ ] { } ; : ' \" , . < > / ? \\ |)")
        }

        if (newPassword.length < 6) {
            errors.add("New password must be at least 6 characters")
        } else if (newPassword.length > 30) {
            errors.add("New password must not exceed 30 characters")
        } else if (!passwordRegex.matches(newPassword)) {
            errors.add("New password can only contain: Latin letters," +
                    " digits and special characters (! @ # \$ % ^ & * ( ) _ + - = [ ] { } ; : ' \" , . < > / ? \\ |)")
        }

        if (oldPassword == newPassword) {
            errors.add("New password must differ from the old one")
        }

        return if (errors.isEmpty())
            ValidationResult.success()
        else
            ValidationResult.errors(errors)
    }
}
