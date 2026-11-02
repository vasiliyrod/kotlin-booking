package tech.ecom.booking.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenDto(
    val accessToken: String,
    val message: String = ""
)