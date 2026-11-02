package data.entity

import tech.ecom.booking.data.entity.Role
import java.util.UUID


class Guest(
    var id: UUID,
    var surname: String? = null,
    var firstname: String? = null,
    var lastname: String? = null,
    var birthdate: java.time.LocalDate? = null,
    var phoneNumber: String,
    var passwordHash: String? = null,
    var role: Role = Role.USER
)
