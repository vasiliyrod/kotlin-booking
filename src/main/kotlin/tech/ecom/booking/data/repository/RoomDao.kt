package edu.booking.hotel_booking.dao

import data.entity.Room
import exception.DaoException
import io.github.oshai.kotlinlogging.KotlinLogging
import mapper.RoomRowMapper
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class RoomDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val roomRowMapper: RoomRowMapper,
) : Dao<Int, Room> {

    private val logger = KotlinLogging.logger { }

    override fun findById(id: Int): Room? {
        logger.trace { "findById called with: id=$id" }
        val FIND_BY_ID_SQL = "SELECT id, floor, number, capacity " +
                "FROM room " +
                "WHERE id = :id"

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        val result = jdbcTemplate.query(
            FIND_BY_ID_SQL,
            params,
            roomRowMapper
        ).firstOrNull()

        logger.debug { "findById finished: $result" }
        return result
    }

    fun findAll(): MutableList<Room> {
        logger.trace { "findAll called" }
        val FIND_ALL_SQL = "SELECT id, floor, number, capacity FROM room "

        val result = jdbcTemplate.query(FIND_ALL_SQL, roomRowMapper)
        logger.debug { "findAll finished: ${result.size} rooms" }
        return result
    }

    override fun create(entity: Room): Room {
        logger.trace { "create called with: floor=${entity.floor}, number=${entity.number}, capacity=${entity.capacity}" }
        val SAVE_SQL = "INSERT INTO room (floor, number, capacity) " +
                "VALUES (:floor, :number, :capacity) " +
                "RETURNING id"

        val params = MapSqlParameterSource()
            .addValue("floor", entity.floor)
            .addValue("number", entity.number)
            .addValue("capacity", entity.capacity)

        val generatedId: Int = jdbcTemplate.queryForObject(SAVE_SQL, params, Int::class.java)!!

        entity.id = generatedId
        logger.debug { "create finished: SUCCESS id=$generatedId" }
        return entity
    }

    override fun update(entity: Room): Boolean {
        logger.trace { "update called with: id=${entity.id}, floor=${entity.floor}, number=${entity.number}" }
        val UPDATE_SQL = "UPDATE room " +
                "SET floor = :floor, number = :number, capacity = :capacity " +
                "WHERE id = :id"

        val params = MapSqlParameterSource()
            .addValue("floor", entity.floor)
            .addValue("number", entity.number)
            .addValue("capacity", entity.capacity)
            .addValue("id", entity.id)

        val updated = jdbcTemplate.update(UPDATE_SQL, params) > 0
        logger.debug { "update finished: $updated" }
        return updated
    }

    override fun delete(id: Int): Boolean {
        logger.trace { "delete called with: id=$id" }
        val DELETE_SQL = "DELETE FROM room " +
                "WHERE id = :id"

        val params = MapSqlParameterSource()
            .addValue("id", id)

        val deleted = jdbcTemplate.update(DELETE_SQL, params) > 0
        logger.debug { "delete finished: $deleted" }
        return deleted
    }

    fun existsByFloorAndNumber(entity: Room): Boolean {
        logger.trace { "existsByFloorAndNumber called with: floor=${entity.floor}, number=${entity.number}" }
        val sql = """
        SELECT EXISTS(
            SELECT 1 FROM room 
            WHERE floor = :floor AND number = :number
        ) as exists
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("floor", entity.floor)
            .addValue("number", entity.number)

        val result = jdbcTemplate.query(sql, params, { rs, _ -> rs.getBoolean("exists") }).first()
        logger.debug { "existsByFloorAndNumber finished: $result" }
        return result
    }

    fun existsAnotherWithSameFloorAndNumber(entity: Room): Boolean {
        logger.trace { "existsAnotherWithSameFloorAndNumber called with: id=${entity.id}, floor=${entity.floor}, number=${entity.number}" }
        val sql = """
        SELECT EXISTS(
            SELECT 1 FROM room 
            WHERE floor = :floor AND number = :number AND id != :id
        ) as exists
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("floor", entity.floor)
            .addValue("number", entity.number)
            .addValue("id", entity.id)

        val result = jdbcTemplate.query(sql, params, { rs, _ -> rs.getBoolean("exists") }).first()
        logger.debug { "existsAnotherWithSameFloorAndNumber finished: $result" }
        return result
    }
}
