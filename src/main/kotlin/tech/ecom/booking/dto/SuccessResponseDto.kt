package tech.ecom.booking.dto

import kotlinx.serialization.Serializable

@Serializable
data class SuccessResponseDto(
    val message: String
)