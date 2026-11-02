package mapper

import data.entity.Guest
import org.springframework.jdbc.core.RowMapper
import tech.ecom.booking.data.entity.Role
import java.util.UUID


class GuestRowMapper : RowMapper<Guest> {

    @Throws(java.sql.SQLException::class)
    override fun mapRow(rs: java.sql.ResultSet, rowNum: kotlin.Int): Guest {

        return Guest(
            id = rs.getObject("id", UUID::class.java),
            surname = rs.getString("surname"),
            firstname = rs.getString("firstname"),
            lastname = rs.getString("lastname"),
            birthdate = rs.getDate("birthdate")?.toLocalDate(),
            phoneNumber = rs.getString("phone_number"),
            passwordHash = rs.getString("password_hash") ?: "",
            role = Role.fromId(rs.getInt("role_id"))
        )
    }
}
