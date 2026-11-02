package tech.ecom.booking.dto.auth

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import tech.ecom.booking.data.entity.Role
import java.util.UUID

@Serializable
data class UserInfoDto(
    @Contextual val id: UUID,
    val role: Role
)