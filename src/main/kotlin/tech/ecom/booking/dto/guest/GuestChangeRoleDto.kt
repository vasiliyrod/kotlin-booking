package tech.ecom.booking.dto.guest

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.dto.ValidationResult
import java.util.UUID

@Serializable
data class GuestChangeRoleDto(
    @Contextual val id: UUID,
    val roleId: Int
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (!Role.entries.contains(Role.fromId(roleId))) {
            errors.add("Role does not exist")
        }

        return if (errors.isEmpty())
            ValidationResult.Companion.success()
        else
            ValidationResult.Companion.errors(errors)
    }
}