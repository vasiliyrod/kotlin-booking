package dto

import data.entity.Room
import kotlinx.serialization.Serializable

@Serializable
data class RoomReadDto(
    val id: Int,
    val floor: Int,
    val number: String,
    val capacity: Int?
) {
    fun toEntity(): Room {
        return Room(
            id = this.id,
            floor = this.floor,
            number = this.number,
            capacity = this.capacity
        )
    }

    companion object {
        fun fromEntity(room: Room): RoomReadDto {
            return RoomReadDto(
                id = requireNotNull(room.id) { "Can not convert unsaved Room to ReadDto. Room must have an ID." },
                floor = room.floor,
                number = room.number,
                capacity = room.capacity
            )
        }

    }
}