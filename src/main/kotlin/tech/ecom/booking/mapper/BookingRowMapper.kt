package mapper

import data.entity.Booking
import data.entity.Room
import org.springframework.jdbc.core.RowMapper
import java.util.UUID


class BookingRowMapper() : RowMapper<Booking> {

    @Throws(java.sql.SQLException::class)
    override fun mapRow(rs: java.sql.ResultSet, rowNum: kotlin.Int): Booking {

        val booking = Booking(
            id = rs.getObject("booking_id", UUID::class.java),
            startDate = rs.getTimestamp("start_date").toLocalDateTime(),
            endDate = rs.getTimestamp("end_date").toLocalDateTime(),
            room = Room(
                id = rs.getInt("room_id"),
                floor = rs.getInt("floor"),
                number = rs.getString("number"),
                capacity = rs.getInt("capacity")
            )
        )

        return booking
    }
}
