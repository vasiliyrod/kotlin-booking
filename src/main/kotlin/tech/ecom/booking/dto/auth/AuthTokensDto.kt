package tech.ecom.booking.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokensDto(
    val refreshToken: String,
    val accessToken: String
)
