package data.entity

import java.time.LocalDateTime
import java.util.UUID

class RefreshToken(
    var id: Int? = null,
    var guestId: UUID,
    var token: String,
    var expiresAt: LocalDateTime,
    var createdAt: LocalDateTime? = null
)
