package dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import tech.ecom.booking.dto.ValidationResult
import java.time.LocalDateTime

@Serializable
data class BookingAvailableRoomsDto(
    @Contextual val startDate: LocalDateTime = LocalDateTime.now(),
    @Contextual val endDate: LocalDateTime
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (startDate.isAfter(endDate)) {
            errors.add("Start date cannot be after end date")
        }

        if (startDate.isBefore(LocalDateTime.now())) {
            errors.add("Start date cannot be in the past")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.errors(errors)
        }
    }
}