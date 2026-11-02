package dto

import data.entity.Room
import kotlinx.serialization.Serializable
import tech.ecom.booking.dto.ValidationResult

@Serializable
class RoomEditDto(
    val id: Int,

    val floor: Int,

    val number: String,

    val capacity: Int?
) {

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (id <= 0) {
            errors.add("Room ID must be a positive number")
        }

        if (floor < -100 || floor > 300) {
            errors.add("Floor must be in range from -100 to 300")
        }

        if (number.isBlank()) {
            errors.add("Room number cannot be empty")
        }

        if (number.length > 50) {
            errors.add("Room number cannot exceed 50 characters")
        }

        if (capacity != null && capacity <= 0) {
            errors.add("Capacity must be a positive number")
        }

        if (capacity != null && capacity > 100) {
            errors.add("Capacity cannot exceed 100 people")
        }

        return if (errors.isEmpty()) ValidationResult.success()
        else ValidationResult.errors(errors)
    }

    fun toEntity(): Room {
        return Room(
            id = id,
            floor = floor,
            number = number,
            capacity = capacity
        )
    }
}