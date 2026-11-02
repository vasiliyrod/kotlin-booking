package tech.ecom.booking.dto.guest

import data.entity.Guest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import tech.ecom.booking.dto.ValidationResult
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Serializable
data class GuestCreateDto(
    val surname: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    @Contextual val birthdate: LocalDate? = null,
    val phoneNumber: String,
    val password: String
) {
    companion object {
        private val phoneRegex = Regex("^\\+?[0-9]{10,15}$")
        private val phoneCleanRegex = Regex("[\\s\\-()]")
        private val passwordRegex = Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$")
        private const val maxStringLength = 100
        private const val maxAge = 140
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (surname != null && surname.isBlank()) {
            errors.add("Surname cannot be empty if provided")
        } else if (surname != null && surname.length > maxStringLength) {
            errors.add("Surname cannot exceed $maxStringLength characters")
        }

        if (firstname != null && firstname.isBlank()) {
            errors.add("First name cannot be empty if provided")
        } else if (firstname != null && firstname.length > maxStringLength) {
            errors.add("First name cannot exceed $maxStringLength characters")
        }

        if (lastname != null && lastname.isBlank()) {
            errors.add("Last name cannot be empty if provided")
        } else if (lastname != null && lastname.length > maxStringLength) {
            errors.add("Last name cannot exceed $maxStringLength characters")
        }

        if (birthdate != null) {
            val today = LocalDate.now()
            val age = Period.between(birthdate, today).years

            if (birthdate.isAfter(today)) {
                errors.add("Birth date cannot be in the future")
            } else if (age > maxAge) {
                errors.add("Age cannot exceed $maxAge years")
            }
        }

        if (phoneNumber.isBlank()) {
            errors.add("Phone number cannot be empty if provided")
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

    fun toEntity(id: UUID): Guest {
        return Guest(
            id = id,
            surname = surname,
            firstname = firstname,
            lastname = lastname,
            birthdate = birthdate,
            phoneNumber = phoneNumber
        )
    }
}