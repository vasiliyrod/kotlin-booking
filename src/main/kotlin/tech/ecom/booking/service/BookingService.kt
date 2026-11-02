package tech.ecom.booking.service

import data.entity.Booking
import data.entity.Guest
import data.entity.Room
import dto.*
import edu.booking.hotel_booking.dao.BookingDao
import exception.DaoException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import service.RoomService
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class BookingService(
    private val bookingDao: BookingDao,
    private val guestService: GuestService,
    private val roomService: RoomService,
    private val dataSource: DataSource
) {
    private val logger = KotlinLogging.logger { }

    fun findById(id: UUID, guestId: UUID): OperationResult<BookingReadDto> {
        logger.debug { "findById called with: id=$id" }

        try {
            val booking: Booking? = bookingDao.findByIdWithGuests(id)

            if (booking == null) {
                logger.info { "findById finished: NOT_FOUND id=$id" }
                return OperationResult.notFound("Booking with id=$id not found")
            } else {
                var exists: Boolean = false
                for (guest in booking.guests){
                    if (guest.id == guestId){
                        exists = true
                    }
                }
                if (!exists){
                    logger.warn { "User ($guestId) tried to get access to the other user's booking. Booking id: $id" }
                    return OperationResult.forbidden("Access denied")
                }
            }
            val result = OperationResult.ok(BookingReadDto.fromEntity(booking))
            logger.info { "findById finished: SUCCESS" }
            return result

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while finding booking id=$id" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while finding booking id=$id" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while finding booking id=$id" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun findAll(): OperationResult<List<BookingReadDto>> {
        logger.debug { "findAll called" }
        try {
            val bookings = bookingDao.findAll()
            val result = bookings.map { BookingReadDto.fromEntity(it) }
            logger.info { "findAll finished: ${result.size} bookings" }
            return OperationResult.ok(result)

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while getting all bookings" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while getting all bookings" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while getting all bookings" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun findAvailableRooms(dto: BookingAvailableRoomsDto): OperationResult<List<RoomReadDto>> {
        logger.debug { "findAvailableRooms called with: startDate=${dto.startDate}, endDate=${dto.endDate}" }
        val validationResult = dto.validate()
        if (!validationResult.isValid) {
            logger.info { "findAvailableRooms finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {
            val rooms = bookingDao.findAvailableRooms(dto.startDate, dto.endDate)
            val result = rooms.map { RoomReadDto.fromEntity(it) }
            logger.info { "findAvailableRooms finished: ${result.size} rooms available" }
            return OperationResult.ok(result)

        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while finding available rooms" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while finding available rooms" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while finding available rooms" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun create(createDto: BookingCreateDto, userId: UUID): OperationResult<BookingReadDto> {
        logger.debug { "create called with: roomId=${createDto.roomId}, guestIds=${createDto.guestIds}, startDate=${createDto.startDate}, endDate=${createDto.endDate}" }

        val validationResult = createDto.validate()
        if (!validationResult.isValid) {
            logger.info { "create finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        try {
            val roomRes = roomService.findById(createDto.roomId)

            val room: Room
            when (roomRes) {
                is OperationResult.Error -> {
                    logger.trace { "Room validation failed: $roomRes" }
                    return roomRes
                }

                is OperationResult.Success -> room = roomRes.data.toEntity()
            }
            logger.trace { "Room validated: roomId=${room.id}" }

            val guestsIds = createDto.guestIds.toMutableList()
            if (!guestsIds.contains(userId)) {
                guestsIds.add(userId)
            }

            val guests: List<Guest>
            val guestsRes = guestService.findAllByIds(guestsIds)

            when (guestsRes) {
                is OperationResult.Error -> {
                    logger.trace { "Guest validation failed: $guestsRes" }
                    return guestsRes
                }

                is OperationResult.Success -> {
                    val guestDtoList: List<GuestReadDto> = guestsRes.data
                    guests = guestDtoList.map { it.toEntity() }
                }
            }
            logger.trace { "Guests validated: ${guests.size} guests" }


            val bookingId = UUID.randomUUID()
            logger.debug { "Generated booking id: $bookingId" }

            val booking = createDto.toEntity(bookingId, room)
            booking.guests = guests.toMutableList()

            val isCreated = bookingDao.checkAndCreate(booking)
            if (isCreated) {
                logger.info { "create finished: SUCCESS bookingId=${booking.id}" }
                return OperationResult.created(BookingReadDto.fromEntity(booking))
            } else {
                logger.info { "create finished: SUCCESS bookingId=${booking.id}" }
                return OperationResult.internalError("Failed to create new booking")
            }
        } catch (e: DuplicateKeyException) {
            logger.warn(e) { "Room not available on selected dates (conflict detected by DB)" }
            return OperationResult.conflict("Room is not available on the selected dates")
        } catch (e: DaoException) {
            logger.error(e) { "Dao error while creating booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            logger.error(e) { "DB error while creating booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            logger.error(e) { "SQL error while creating booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Data conversion error while creating booking" }
            return OperationResult.internalError("Data conversion error")
        } catch (e: Exception) {
            logger.error(e) { "Unknown error while creating booking" }
            return OperationResult.internalError("Internal server error")
        }
    }

    fun update(editDto: BookingEditDto, userId: UUID): OperationResult<BookingReadDto> {
        logger.debug { "update called with: id=${editDto.id}, roomId=${editDto.roomId}, guestIds=${editDto.guestIds}, startDate=${editDto.startDate}, endDate=${editDto.endDate}" }
        val validationResult = editDto.validate()
        if (!validationResult.isValid) {
            logger.warn { "update finished: validation failed - ${validationResult.errors}" }
            return OperationResult.badRequest(validationResult.errors.joinToString(", "))
        }

        var connection: Connection? = null
        try {
            connection = dataSource.connection
            connection.autoCommit = false
            logger.trace { "Transaction started for booking update" }

            val existingBooking = bookingDao.findByIdWithGuests(editDto.id)
            if (existingBooking == null) {
                logger.warn { "update finished: booking id=${editDto.id} not found" }
                return OperationResult.notFound("Booking with id=${editDto.id} does not exist")
            } else {
                var exists: Boolean = false
                for (guest in existingBooking.guests){
                    if (guest.id == userId){
                        exists = true
                    }
                }
                if (!exists){
                    logger.warn { "User ($userId) tried to get access to the other user's booking. Booking id: ${editDto.id}" }
                    return OperationResult.forbidden("Access denied")
                }
            }

            val roomResult = roomService.findById(editDto.roomId)

            val room: Room
            when (roomResult) {
                is OperationResult.Error -> return roomResult
                is OperationResult.Success<RoomReadDto> -> room = roomResult.data.toEntity()
            }

            val guests: List<Guest>
            val guestsRes = guestService.findAllByIds(editDto.guestIds)

            when (guestsRes) {
                is OperationResult.Error -> return guestsRes
                is OperationResult.Success -> {
                    val guestDtoList: List<GuestReadDto> = guestsRes.data
                    guests = guestDtoList.map { it.toEntity() }
                }
            }

            val datesChanged = !existingBooking.startDate.isEqual(editDto.startDate) ||
                    !existingBooking.endDate.isEqual(editDto.endDate)
            val roomChanged = existingBooking.room.id != room.id

            if (datesChanged || roomChanged) {
                logger.trace { "Checking availability - datesChanged=$datesChanged, roomChanged=$roomChanged" }
                val availableRooms = bookingDao.findAvailableRooms(
                    editDto.startDate,
                    editDto.endDate
                )
                val isAvailable = if (roomChanged) {
                    availableRooms.any { it.id == room.id }
                } else {
                    bookingDao.isRoomAvailableExcludingCurrentBooking(
                        room.id!!,
                        editDto.startDate,
                        editDto.endDate,
                        editDto.id
                    )
                }

                if (!isAvailable) {
                    logger.warn { "update finished: room ${room.number} not available on selected dates" }
                    return OperationResult.badRequest(
                        "Room ${room.number} is not available on the selected dates"
                    )
                }
                logger.trace { "Room availability confirmed for update" }
            }

            val booking = editDto.toEntity(room, guests)
            val updated = bookingDao.update(booking, connection)

            if (!updated) {
                connection.rollback()
                logger.trace { "Transaction rolled back - update failed" }
                logger.info { "update finished: failed to update booking" }
                return OperationResult.internalError("Failed to update booking")
            }

            connection.commit()
            logger.trace { "Transaction committed for booking update" }

            val updatedBooking = bookingDao.findByIdWithGuests(booking.id)
            val result = OperationResult.ok(BookingReadDto.fromEntity(updatedBooking!!))
            logger.info { "update finished: SUCCESS bookingId=${booking.id}" }
            return result

        } catch (e: DaoException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to DaoException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "Dao error while updating booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to DataAccessException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "DB error while updating booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to SQLException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "SQL error while updating booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: IllegalArgumentException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to IllegalArgumentException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "Data conversion error while updating booking" }
            return OperationResult.internalError("Data conversion error")
        } catch (e: Exception) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to unknown error" }
            } catch (_: Exception) {
            }
            logger.error(e) { "Unknown error while updating booking" }
            return OperationResult.internalError("Internal server error")
        } finally {
            try {
                connection?.autoCommit = true
                connection?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun delete(id: UUID): OperationResult<Unit> {
        logger.debug { "delete called with: id=$id" }
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            connection.autoCommit = false
            logger.trace { "Transaction started for booking deletion" }

            val existingBooking = bookingDao.findById(id)
            if (existingBooking == null) {
                logger.warn { "delete finished: booking id=$id not found" }
                return OperationResult.notFound("Booking with id=$id does not exist")
            }

            val deleted = bookingDao.deleteTransactional(id, connection)

            if (!deleted) {
                connection.rollback()
                logger.trace { "Transaction rolled back - deletion failed" }
                logger.warn { "delete finished: failed to delete booking id=$id" }
                return OperationResult.internalError("Failed to delete booking")
            }

            connection.commit()
            logger.trace { "Transaction committed for booking deletion" }

            logger.info { "delete finished: SUCCESS id=$id" }
            return OperationResult.ok(Unit)

        } catch (e: DaoException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to DaoException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "Dao error while deleting booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: DataAccessException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to DataAccessException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "DB error while deleting booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: SQLException) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to SQLException" }
            } catch (_: Exception) {
            }
            logger.error(e) { "SQL error while deleting booking" }
            return OperationResult.internalError("Internal server error")
        } catch (e: Exception) {
            try {
                connection?.rollback(); logger.trace { "Transaction rolled back due to unknown error" }
            } catch (_: Exception) {
            }
            logger.error(e) { "Unknown error while deleting booking" }
            return OperationResult.internalError("Internal server error")
        } finally {
            try {
                connection?.autoCommit = true
                connection?.close()
            } catch (_: Exception) {
            }
        }
    }
}
