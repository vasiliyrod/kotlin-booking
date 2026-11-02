package dto

import data.entity.Guest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import tech.ecom.booking.data.entity.Role
import java.time.LocalDate
import java.util.UUID

@Serializable
data class GuestReadDto(
    @Contextual val id: UUID?,
    val surname: String?,
    val firstname: String?,
    val lastname: String?,
    @Contextual val birthdate: LocalDate?,
    val phoneNumber: String?,
    val role: Role = Role.USER
) {
    fun toEntity(): Guest {
        return Guest(
            id = requireNotNull(this.id) { "Can not convert DTO to Guest. Guest must have an ID." },
            surname = this.surname,
            firstname = this.firstname,
            lastname = this.lastname,
            birthdate = this.birthdate,
            phoneNumber = this.phoneNumber ?: ""
        )
    }

    companion object {
        fun fromEntity(guest: Guest): GuestReadDto {
            return GuestReadDto(
                guest.id,
                guest.surname,
                guest.firstname,
                guest.lastname,
                guest.birthdate,
                guest.phoneNumber,
                guest.role
            )
        }
    }
}