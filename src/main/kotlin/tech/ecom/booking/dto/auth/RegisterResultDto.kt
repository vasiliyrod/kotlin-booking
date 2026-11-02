package tech.ecom.booking.dto.auth

import dto.GuestReadDto
import kotlinx.serialization.Serializable

@Serializable
data class RegisterResultDto(
    val guestReadDto: GuestReadDto,
    val accessTokenDto: AccessTokenDto
)
