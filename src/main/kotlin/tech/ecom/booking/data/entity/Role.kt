package tech.ecom.booking.data.entity

enum class Role(
    val id: Int,
    val displayName: String
) {
    PUBLIC(0, "Любой человек"),
    USER(1, "Пользователь"),
    ADMIN(2, "Администратор");

    fun hasRightsOf(role: Role): Boolean {
        return (this.id >= role.id)
    }

    companion object {
        fun fromId(id: Int): Role {
            return entries.find { it.id == id }
                ?: throw IllegalArgumentException("Unknown role id: $id")
        }

        fun fromName(name: String): Role? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }
    }
}