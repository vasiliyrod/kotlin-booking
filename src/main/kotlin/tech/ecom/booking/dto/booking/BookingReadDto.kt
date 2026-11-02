package dto

import data.entity.Booking
import data.entity.Guest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class BookingReadDto(
    @Contextual val id: UUID,
    @Contextual val startDate: LocalDateTime,
    @Contextual val endDate: LocalDateTime,
    val room: RoomReadDto,
    val guests: List<GuestReadDto> = listOf()
) {
    companion object {
        fun fromEntity(booking: Booking): BookingReadDto {
            return BookingReadDto(
                booking.id,
                booking.startDate,
                booking.endDate,
                RoomReadDto.Companion.fromEntity(booking.room),
                booking.guests
                    .map({ guest: Guest -> GuestReadDto.fromEntity(guest) })
            )
        }
    }
}