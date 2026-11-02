package tech.ecom.booking.service

import data.entity.Guest
import dto.GuestEditDto
import dto.GuestReadDto
import dto.OperationResult
import edu.booking.hotel_booking.dao.GuestDao
import exception.DaoException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataAccessException
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.dto.guest.GuestChangeRoleDto
import tech.ecom.booking.dto.guest.GuestCreateDto
import tech.ecom.booking.utils.PasswordUtil
import java.sql.SQLException
import java.util.UUID

class GuestService(
    val guestDao: GuestDao
) {
    private val logger = KotlinLogging.logger { }

    fun findById(id: UUID): OperationResult<GuestReadDto> {
        logger.debug { "findById called with: id=$id" }
        try {

            val guest: Guest? = guestDao.findById(id)

            if (guest == null) {
                logger.info { "findById finished: NOT_FOUND id=$id" }
                return OperationResult.notFound("Guest with id=$id not found")
            } else {
                val result = OperationResult.ok(GuestReadDto.fromEntity(guest))
                logger.info { "findById finished: SUCCESS" }
                return result
            }

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while finding guest id=$id" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while finding guest id=$id" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while finding guest id=$id" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun findAllByIds(idList: List<UUID>): OperationResult<List<GuestReadDto>>{
        logger.debug { "findAllByIds called with: ids=$idList" }
        try {
            val guests: List<Guest> = guestDao.findAllByIds(idList)

            val result: List<GuestReadDto> = guests.map { GuestReadDto.fromEntity(it) }

            logger.info { "findAllByIds finished: ${result.size} guests" }
            return OperationResult.ok(result)

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while finding guests by ids" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while finding guests by ids" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while finding guests by ids" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun create(createDto: GuestCreateDto): OperationResult<GuestReadDto> {
        logger.debug { "create called with: phoneNumber=${createDto.phoneNumber}" }
        val validationResult = createDto.validate()
        if (!validationResult.isValid) {
            logger.info { "create finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {

            val id = UUID.randomUUID()
            logger.debug { "Generated guest id: $id" }
            var entity = createDto.toEntity(id)
            entity.passwordHash = PasswordUtil.hashPassword(createDto.password)

            entity = guestDao.create(entity)

            val resultDto = GuestReadDto.fromEntity(entity)

            val result = OperationResult.created(resultDto)
            logger.info { "create finished: SUCCESS guestId=${entity.id}" }
            return result

        } catch (e: DaoException) {
            logger.error(e) { "Dao error while creating guest" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while creating guest" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while creating guest" }
            return OperationResult.internalError("Internal server error")
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Data conversion error while creating guest" }
            return OperationResult.internalError("Data conversion error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while creating guest" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun changeRole(dto: GuestChangeRoleDto): OperationResult<GuestReadDto> {
        logger.debug { "changeRole called with: guestId=${dto.id}, roleId=${dto.roleId}" }
        val validationResult = dto.validate()
        if (!validationResult.isValid) {
            logger.info { "changeRole finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {
            val guest = guestDao.findById(dto.id)
            if (guest == null) {
                logger.info { "changeRole finished: NOT_FOUND guestId=${dto.id}" }
                return OperationResult.notFound("Guest with id=${dto.id} not found")
            }

            val newRole = Role.fromId(dto.roleId)
            if (!guestDao.changeRole(dto.id, newRole.id)) {
                logger.info { "changeRole finished: failed to change role for guestId=${dto.id}" }
                return OperationResult.internalError("Failed to change role")
            }

            guest.role = newRole
            val result = OperationResult.ok(GuestReadDto.fromEntity(guest))
            logger.info { "changeRole finished: SUCCESS guestId=${dto.id}, newRole=${newRole.displayName}" }
            return result

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while changing role for guestId=${dto.id}" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while changing role for guestId=${dto.id}" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while changing role for guestId=${dto.id}" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun update(editDto: GuestEditDto, id: UUID): OperationResult<GuestReadDto> {
        logger.debug { "update called with: id=${id}" }
        val validationResult = editDto.validate()
        if (!validationResult.isValid) {
            logger.info { "update finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {
            val entity = editDto.toEntity(id)

            if (guestDao.update(entity)) {
                val result = OperationResult.ok(GuestReadDto.fromEntity(entity))
                logger.info { "update finished: SUCCESS guestId=${entity.id}" }
                return result
            } else {
                logger.info { "update finished: NOT_FOUND guestId=${id}" }
                return OperationResult.notFound("Guest with id=${id} does not exist. Update failed.")
            }

        } catch (e: DaoException) {
            logger.error(e) { "Dao error while updating guest" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while updating guest" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while updating guest" }
            return OperationResult.internalError("Internal server error")
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Data conversion error while updating guest" }
            return OperationResult.internalError("Data conversion error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while updating guest" }
            return OperationResult.internalError("Internal server error")
        }
    }

}
