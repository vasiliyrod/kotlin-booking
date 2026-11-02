package dto

import data.entity.Booking
import data.entity.Guest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import tech.ecom.booking.dto.ValidationResult
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class BookingEditDto(
    @Contextual val id: UUID,
    @Contextual val startDate: LocalDateTime,
    @Contextual val endDate: LocalDateTime,
    val roomId: Int,
    val guestIds: List<@Contextual UUID>
) {
    companion object {
        private const val maxBookingDurationDays = 365
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (startDate.isBefore(LocalDateTime.now())) {
            errors.add("Start date cannot be in the past")
        }

        if (startDate.isAfter(endDate)) {
            errors.add("Start date cannot be after end date")
        }

        val durationDays = Duration.between(startDate, endDate).toDays()
        if (durationDays > maxBookingDurationDays) {
            errors.add("Maximum booking duration is $maxBookingDurationDays days")
        }

        if (roomId <= 0) {
            errors.add("Room ID must be a positive number")
        }

        if (guestIds.isEmpty()) {
            errors.add("Booking must have at least one guest")
        }

        val uniqueGuestIds = guestIds.distinct()
        if (uniqueGuestIds.size != guestIds.size) {
            errors.add("Guest list contains duplicates")
        }

        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.errors(errors)
        }
    }

    fun toEntity(room: data.entity.Room, guests: List<Guest>): Booking {
        return Booking(
            id = id,
            startDate = startDate,
            endDate = endDate,
            room = room,
            guests = guests.toMutableList()
        )
    }
}