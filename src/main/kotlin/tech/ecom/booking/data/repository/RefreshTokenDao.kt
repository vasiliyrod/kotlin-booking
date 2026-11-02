package tech.ecom.booking.data.repository

import data.entity.RefreshToken
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class RefreshTokenDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val logger = KotlinLogging.logger { }

    fun create(token: RefreshToken): RefreshToken {
        logger.trace { "create called with: guestId=${token.guestId}, token=${token.token}" }
        val CREATE_SQL = """
            INSERT INTO refresh_token (guest_id, token, expires_at)
            VALUES (:guestId, :token, :expiresAt)
            RETURNING id, created_at
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("guestId", token.guestId)
            .addValue("token", token.token)
            .addValue("expiresAt", token.expiresAt)

        val result = jdbcTemplate.queryForObject(CREATE_SQL, params) { rs, _ ->
            token.id = rs.getInt("id")
            token.createdAt = rs.getTimestamp("created_at")?.toLocalDateTime()
            token
        }

        logger.debug { "create finished: id=${result.id}" }
        return result
    }

    fun findValidByToken(token: String): RefreshToken? {
        logger.trace { "findValidByToken called" }
        val FIND_BY_TOKEN_SQL = """
            SELECT id, guest_id, token, expires_at, created_at
            FROM refresh_token
            WHERE token = :token AND expires_at > CURRENT_TIMESTAMP
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("token", token)

        val result = jdbcTemplate.query(FIND_BY_TOKEN_SQL, params) { rs, _ ->
            RefreshToken(
                id = rs.getInt("id"),
                guestId = rs.getObject("guest_id", UUID::class.java),
                token = rs.getString("token"),
                expiresAt = rs.getTimestamp("expires_at").toLocalDateTime(),
                createdAt = rs.getTimestamp("created_at")?.toLocalDateTime()
            )
        }.firstOrNull()

        logger.debug { "findValidByToken finished: ${result?.let { "found guestId=${it.guestId}" } ?: "not found"}" }
        return result
    }

    fun delete(token: String): Boolean {
        logger.trace { "delete called" }
        val DELETE_SQL = """
            DELETE FROM refresh_token
            WHERE token = :token
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("token", token)

        val result = jdbcTemplate.update(DELETE_SQL, params) > 0
        logger.debug { "delete finished: $result" }
        return result
    }

    fun deleteAllForGuest(guestId: UUID): Boolean {
        logger.trace { "deleteAllForGuest called with: guestId=$guestId" }
        val DELETE_SQL = """
            DELETE FROM refresh_token
            WHERE guest_id = :guestId
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("guestId", guestId)

        val result = jdbcTemplate.update(DELETE_SQL, params) > 0
        logger.debug { "deleteAllForGuest finished: $result" }
        return result
    }
}
