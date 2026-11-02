package mapper

import data.entity.Room
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet


class RoomRowMapper : RowMapper<Room> {

    override fun mapRow(rs: ResultSet, rowNum: Int): Room {
        return Room(
            id = rs.getInt("id"),
            floor = rs.getInt("floor"),
            number = rs.getString("number"),
            capacity = rs.getInt("capacity")
        )
    }
}
