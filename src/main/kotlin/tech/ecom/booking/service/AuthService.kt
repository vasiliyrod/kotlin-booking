package tech.ecom.booking.service

import com.sun.net.httpserver.HttpExchange
import data.entity.Guest
import data.entity.RefreshToken
import dto.OperationResult
import edu.booking.hotel_booking.dao.GuestDao
import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import org.springframework.dao.DataAccessException
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.data.repository.RefreshTokenDao
import tech.ecom.booking.dto.auth.AuthLoginDto
import tech.ecom.booking.dto.auth.AuthTokensDto
import tech.ecom.booking.dto.auth.UserInfoDto
import tech.ecom.booking.utils.AppConfig
import tech.ecom.booking.utils.PasswordUtil
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

class AuthService(
    private val refreshTokenDao: RefreshTokenDao,
    private val guestDao: GuestDao
) {
    private val logger = KotlinLogging.logger { }

    private val accessTokenTtlMs = AppConfig.accessTokenTtlMs
    private val refreshTokenTtlDays = AppConfig.refreshTokenTtlDays

    companion object {
        private val logger = KotlinLogging.logger { }

        fun getUserInfoFromAccessToken(token: String): UserInfoDto {
            logger.debug { "getUserInfoFromAccessToken called with: token=$token" }
            val parser = Jwts.parser().build()
            val claims = parser.parseSignedClaims(token).payload

            val guestId = UUID.fromString(claims.subject)
            val role = Role.fromName(claims["role"].toString())!!

            val result = UserInfoDto(guestId, role)

            logger.info { "getUserInfoFromAccessToken finished: $result" }
            return result
        }

        fun getUserInfoFromAccessToken(httpExchange: HttpExchange): UserInfoDto {
            logger.debug { "getUserInfoFromAccessToken called with: httpExchange" }
            val token = httpExchange.requestHeaders.getFirst("Authorization").removePrefix("Bearer ").trim()
            val result = getUserInfoFromAccessToken(token)

            logger.info { "getUserInfoFromAccessToken finished: $result" }
            return result
        }

        fun getCurrentUserRefreshToken(httpExchange: HttpExchange): String {
            logger.debug { "getCurrentUserRefreshToken called" }
            val cookiesStr = httpExchange.requestHeaders.getFirst("Cookie")

            var token: String = "";
            val cookies = cookiesStr.split("; ")
            for (cookie in cookies) {
                val cookieName = cookie.split("=")[0]
                if (cookieName == "refresh_token") {
                    token = cookie.split("=")[1]
                    break
                }
            }
            logger.info { "getCurrentUserRefreshToken finished: token=$token" }
            return token
        }
    }

    fun authenticate(authLoginDto: AuthLoginDto): OperationResult<AuthTokensDto> {
        logger.debug { "authenticate called with: phoneNumber=${authLoginDto.phoneNumber}" }

        val guest: Guest? = guestDao.findByPhoneNumberWithPasswordHash(authLoginDto.phoneNumber)

        if (guest == null) {
            logger.info { "authenticate finished: UNAUTHORIZED - guest not found for phoneNumber=${authLoginDto.phoneNumber}" }
            return OperationResult.unauthorized("Invalid phone number or password")
        }

        val isPasswordCorrect: Boolean = PasswordUtil.verifyPassword(authLoginDto.password, guest.passwordHash!!)
        if (!isPasswordCorrect){
            logger.info { "authenticate finished: UNAUTHORIZED - invalid password" }
            return OperationResult.unauthorized("Invalid phone number or password")
        }

        val refreshToken = createRefreshToken(guest.id)
        val accessToken = createAccessToken(guest.id, guest.role)

        val result = OperationResult.ok(
            AuthTokensDto(
                refreshToken = refreshToken,
                accessToken = accessToken
            )
        )
        logger.info { "authenticate finished: SUCCESS" }
        return result
    }

    fun createAccessToken(guestId: UUID, role: Role): String {
        logger.debug { "createAccessToken called with: guestId=$guestId, role=$role" }
        val now = Date()
        val expiryDate = Date(now.time + accessTokenTtlMs)

        val token = Jwts.builder()
            .subject(guestId.toString())
            .claim("role", role.name)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(AppConfig.jwtSecretKey)
            .compact()

        logger.info { "createAccessToken finished: token=$token" }
        return token
    }

    fun validateAccessToken(token: String): OperationResult<UserInfoDto> {
        logger.debug { "validateAccessToken called: token=$token" }
        return try {
            val claims: Claims = Jwts.parser()
                .verifyWith(AppConfig.jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val tokenType = claims["type", String::class.java]
            if (tokenType != "access") {
                logger.info { "validateAccessToken finished: UNAUTHORIZED - wrong token type" }
                return OperationResult.unauthorized()
            }

            if (claims.expiration.before(Date())) {
                logger.info { "validateAccessToken finished: UNAUTHORIZED - token expired" }
                return OperationResult.unauthorized()
            }

            val result = OperationResult.ok(
                UserInfoDto(
                    id = UUID.fromString(claims.subject),
                    role = Role.fromName(claims.get("role", String::class.java))!!
                )
            )
            logger.info { "validateAccessToken finished: SUCCESS" }
            result
        } catch (e: ExpiredJwtException) {
            logger.info { "validateAccessToken finished: UNAUTHORIZED - expired jwt" }
            OperationResult.unauthorized()
        } catch (e: Exception) {
            logger.info { "validateAccessToken finished: UNAUTHORIZED - ${e.message}" }
            OperationResult.unauthorized()
        }
    }


    fun changePassword(guestId: UUID, oldPassword: String, newPassword: String): OperationResult<Unit> {
        logger.debug { "changePassword called with: guestId=$guestId" }
        val guest = guestDao.findById(guestId)
        if (guest == null) {
            logger.info { "changePassword finished: UNAUTHORIZED - guest not found guestId=$guestId" }
            return OperationResult.unauthorized("User not found")
        }

        if (guest.passwordHash == null || !PasswordUtil.verifyPassword(oldPassword, guest.passwordHash!!)) {
            logger.info { "changePassword finished: UNAUTHORIZED - invalid old password" }
            return OperationResult.unauthorized("Invalid old password")
        }

        try {
            val newHash = PasswordUtil.hashPassword(newPassword)
            guestDao.changePassword(guestId, newHash)
            val result = OperationResult.ok(Unit)
            logger.info { "changePassword finished: SUCCESS for guestId=$guestId" }
            return result
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while changing password for guestId=$guestId" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while changing password for guestId=$guestId" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while changing password for guestId=$guestId" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun createRefreshToken(guestId: UUID): String {
        logger.debug { "createRefreshToken called with: guestId=$guestId" }
        val now = LocalDateTime.now()
        val expiresAt = now.plusDays(refreshTokenTtlDays)
        val token = UUID.randomUUID().toString()

        val entity = RefreshToken(
            guestId = guestId,
            token = token,
            expiresAt = expiresAt
        )
        refreshTokenDao.create(entity)
        logger.info { "createRefreshToken finished: token=$token" }
        return token
    }

    fun validateRefreshToken(token: String): OperationResult<UserInfoDto> {
        logger.debug { "validateRefreshToken called" }
        val refreshToken = refreshTokenDao.findValidByToken(token)
        if (refreshToken == null) {
            logger.info { "validateRefreshToken finished: UNAUTHORIZED - token not found" }
            return OperationResult.unauthorized()
        }
        val guest = guestDao.findById(refreshToken.guestId)
        if (guest == null) {
            logger.info { "validateRefreshToken finished: UNAUTHORIZED - guest not found" }
            return OperationResult.unauthorized()
        }
        val result = OperationResult.ok(UserInfoDto(id = guest.id, role = guest.role))
        logger.info { "validateRefreshToken finished: SUCCESS" }
        return result
    }

    fun deleteRefreshToken(token: String): OperationResult<Boolean> {
        logger.debug { "deleteRefreshToken called" }
        try {
            val result = OperationResult.ok(refreshTokenDao.delete(token))
            logger.info { "deleteRefreshToken finished: $result" }
            return result
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while deleting token" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while deleting token" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while deleting token" }
            return OperationResult.internalError("Internal server error")
        }

    }

    fun deleteAllRefreshTokensFromGuest(guestId: UUID): OperationResult<Boolean> {
        logger.debug { "deleteAllRefreshTokensFromGuest called with: guestId=$guestId" }
        try {
            val result = OperationResult.ok(
                refreshTokenDao.deleteAllForGuest(guestId)
            )
            logger.info { "deleteAllRefreshTokensFromGuest finished: $result" }
            return result
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while deleting tokens for guestId=$guestId" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while deleting tokens for guestId=$guestId" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while deleting tokens for guestId=$guestId" }
            return OperationResult.internalError("Internal server error")
        }
    }

}
