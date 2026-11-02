package edu.booking.hotel_booking.dao

import data.entity.Guest
import exception.DaoException
import io.github.oshai.kotlinlogging.KotlinLogging
import mapper.GuestRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID


class GuestDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val guestRowMapper: GuestRowMapper
) : Dao<UUID, Guest> {

    private val logger = KotlinLogging.logger { }

    override fun findById(id: UUID): Guest? {
        logger.trace { "findById called with: id=$id" }
        val FIND_BY_ID_SQL = "SELECT id, surname, firstname, lastname, birthdate, phone_number, role_id, password_hash " +
                "FROM guest " +
                "WHERE id = :id"

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        val result = jdbcTemplate.query(FIND_BY_ID_SQL, params, guestRowMapper).firstOrNull()
        logger.debug { "findById finished: $result" }
        return result
    }

    fun findAll(): MutableList<Guest> {
        logger.trace { "findAll called" }
        val FIND_ALL_SQL =
            "SELECT id, surname, firstname, lastname, birthdate, phone_number, role_id FROM guest "

        val result = jdbcTemplate.query(FIND_ALL_SQL, guestRowMapper)
        logger.debug { "findAll finished: ${result.size} guests" }
        return result
    }

    fun findAllByIds(ids: List<UUID>): List<Guest> {
        logger.trace { "findAllByIds called with: ids=$ids" }
        val FIND_ALL_BY_IDS_SQL = "SELECT id, surname, firstname, lastname, birthdate, phone_number, role_id " +
                "FROM guest " +
                "WHERE id IN (:ids)"

        val params = MapSqlParameterSource()
        params.addValue("ids", ids)

        val result = jdbcTemplate.query(FIND_ALL_BY_IDS_SQL, params, guestRowMapper)
        logger.debug { "findAllByIds finished: ${result.size} guests" }
        return result
    }

    override fun create(entity: Guest): Guest {
        logger.trace { "create called with: id=${entity.id}, phoneNumber=${entity.phoneNumber}" }
        val CREATE_SQL = """
            INSERT INTO guest (id, surname, firstname, lastname, birthdate, phone_number, password_hash, role_id) 
            VALUES (:id, :surname, :firstname, :lastname, :birthdate, :phoneNumber, :passwordHash, :roleId)
        """.trimIndent()

        val params = createParams(entity)

        if (jdbcTemplate.update(CREATE_SQL, params) > 0) {
            logger.debug { "create finished: SUCCESS id=${entity.id}" }
            return entity
        } else {
            throw DaoException("Failed to save guest")
        }
    }

    override fun update(entity: Guest): kotlin.Boolean {
        logger.trace { "update called with: id=${entity.id}, phoneNumber=${entity.phoneNumber}" }
        val UPDATE_SQL = "UPDATE guest " +
                "SET surname = :surname, firstname = :firstname, lastname = :lastname, " +
                "birthdate = :birthdate, phone_number = :phoneNumber " +
                "WHERE id = :id"

        val params = createParams(entity)

        val result = jdbcTemplate.update(UPDATE_SQL, params) > 0
        logger.debug { "update finished: $result" }
        return result
    }

    override fun delete(id: UUID): Boolean {
        logger.trace { "delete called with: id=$id" }
        val DELETE_SQL = "DELETE FROM guest " +
                "WHERE id = :id"

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        val result = jdbcTemplate.update(DELETE_SQL, params) > 0
        logger.debug { "delete finished: $result" }
        return result
    }

    fun findByPhoneNumberWithPasswordHash(phone_number: String): Guest? {
        logger.trace { "findByPhoneNumberWithPasswordHash called with: phoneNumber=$phone_number" }
        val SQL = """
            SELECT id, surname, firstname, lastname, birthdate, phone_number, role_id, password_hash FROM guest
            WHERE phone_number = :phone_number
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("phone_number", phone_number)

        val result = jdbcTemplate.query(SQL, params, guestRowMapper).firstOrNull()
        logger.debug { "findByPhoneNumberWithPasswordHash finished: $result" }
        return result
    }

    fun existsPhoneNumber(phone_number: String): Boolean {
        logger.trace { "existsPhoneNumber called with: phoneNumber=$phone_number" }
        val PHONE_NUMBER_EXISTS = """
            SELECT EXISTS(
                SELECT 1 FROM guest WHERE phone_number = :phone_number
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("phone_number", phone_number)

        val result = jdbcTemplate.queryForObject(PHONE_NUMBER_EXISTS, params, Boolean::class.java)!!
        logger.debug { "existsPhoneNumber finished: $result" }
        return result
    }

    fun changeRole(id: UUID, roleId: Int): Boolean {
        logger.trace { "changeRole called with: id=$id, roleId=$roleId" }
        val SQL = """
            UPDATE guest
            SET role_id = :roleId
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("roleId", roleId)

        val result = jdbcTemplate.update(SQL, params) > 0
        logger.debug { "changeRole finished: $result" }
        return result
    }

    fun changePassword(uuid: UUID, newPassword: String) {
        logger.trace { "changePassword called with: uuid=$uuid" }
        val CHANGE_PASSWORD = """
            UPDATE guest
            SET password_hash = :newPassword
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", uuid)
            .addValue("newPassword", newPassword)

        jdbcTemplate.update(CHANGE_PASSWORD, params)
        logger.debug { "changePassword finished for uuid=$uuid" }
    }

    private fun createParams(guest: Guest): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("id", guest.id)
            .addValue("surname", guest.surname)
            .addValue("firstname", guest.firstname)
            .addValue("lastname", guest.lastname)
            .addValue("birthdate", guest.birthdate)
            .addValue("phoneNumber", guest.phoneNumber)
            .addValue("passwordHash", guest.passwordHash)
            .addValue("roleId", guest.role.id)
    }
}
