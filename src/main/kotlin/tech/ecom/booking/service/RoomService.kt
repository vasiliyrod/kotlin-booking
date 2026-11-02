package service

import data.entity.Room
import dto.OperationResult
import dto.RoomCreateDto
import dto.RoomEditDto
import dto.RoomReadDto
import edu.booking.hotel_booking.dao.RoomDao
import exception.DaoException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataAccessException
import java.sql.SQLException

class RoomService(
    private val roomDao: RoomDao
) {
    private val logger = KotlinLogging.logger { }

    fun findById(id: Int): OperationResult<RoomReadDto> {
        logger.debug { "findById called with: id=$id" }
        if (id <= 0){
            logger.info { "findById finished: BAD_REQUEST - invalid id=$id" }
            return OperationResult.badRequest("Room id must be greater than 0")
        }

        try {

            val room: Room? = roomDao.findById(id)

            if (room == null) {
                logger.info { "findById finished: NOT_FOUND id=$id" }
                return OperationResult.notFound("Room with id=$id not found")
            } else {
                val result = OperationResult.ok(RoomReadDto.fromEntity(room))
                logger.info { "findById finished: SUCCESS" }
                return result
            }

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while finding room id=$id" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while finding room id=$id" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while finding room id=$id" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun create(createDto: RoomCreateDto): OperationResult<RoomReadDto> {
        logger.debug { "create called with: floor=${createDto.floor}, number=${createDto.number}" }
        val validationResult = createDto.validate()
        if (!validationResult.isValid) {
            logger.info { "create finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {

            var entity = createDto.toEntity()

            if (roomDao.existsByFloorAndNumber(entity)) {
                logger.info { "create finished: CONFLICT - room floor=${entity.floor}, number=${entity.number} already exists" }
                return OperationResult.conflict("Room with floor ${entity.floor} and number ${entity.number} already exists")
            }

            entity = roomDao.create(entity)

            val resultDto = RoomReadDto.fromEntity(entity)

            val result = OperationResult.created(resultDto)
            logger.info { "create finished: SUCCESS roomId=${entity.id}, floor=${entity.floor}, number=${entity.number}" }
            return result

        } catch (e: DaoException) {
            logger.error(e) { "Dao error while creating room" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while creating room" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while creating room" }
            return OperationResult.internalError("Internal server error")
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Data conversion error while creating room" }
            return OperationResult.internalError("Data conversion error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while creating room" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun update(editDto: RoomEditDto): OperationResult<RoomReadDto> {
        logger.debug { "update called with: id=${editDto.id}, floor=${editDto.floor}, number=${editDto.number}" }
        val validationResult = editDto.validate()
        if (!validationResult.isValid) {
            logger.info { "update finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {

            val entity = editDto.toEntity()

            if (roomDao.existsAnotherWithSameFloorAndNumber(entity)) {
                logger.info { "update finished: CONFLICT - room floor=${entity.floor}, number=${entity.number} already exists" }
                return OperationResult.conflict("Room with floor ${entity.floor} and number ${entity.number} already exists")
            }

            if (roomDao.update(entity)) {
                val result = OperationResult.ok(RoomReadDto.fromEntity(entity))
                logger.info { "update finished: SUCCESS roomId=${entity.id}" }
                return result
            } else {
                logger.info { "update finished: NOT_FOUND roomId=${editDto.id}" }
                return OperationResult.notFound("Room with id=${editDto.id} does not exist. Update failed.")
            }

        } catch (e: DaoException) {
            logger.error(e) { "Dao error while updating room id=${editDto.id}" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while updating room id=${editDto.id}" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while updating room id=${editDto.id}" }
            return OperationResult.internalError("Internal server error")
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Data conversion error while updating room" }
            return OperationResult.internalError("Data conversion error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while updating room id=${editDto.id}" }
            return OperationResult.internalError("Internal server error")
        }
    }
}
