package edu.booking.hotel_booking.dao

import data.entity.Booking
import data.entity.Guest
import data.entity.Room
import exception.DaoException
import io.github.oshai.kotlinlogging.KotlinLogging
import mapper.BookingRowMapper
import mapper.GuestRowMapper
import mapper.RoomRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.toTypedArray

class BookingDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val bookingRowMapper: BookingRowMapper,
    private val guestRowMapper: GuestRowMapper,
    private val roomRowMapper: RoomRowMapper
) : Dao<UUID, Booking> {

    private val logger = KotlinLogging.logger { }

    private fun getNamedTemplate(conn: Connection): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(SingleConnectionDataSource(conn, false))
    }

    override fun findById(id: UUID): Booking? {
        logger.trace { "findById called with: id=$id" }
        val FIND_BY_ID_SQL = """
            SELECT b.id as booking_id, b_room, start_date, end_date, r.id as room_id, r.floor, r.number, r.capacity
            FROM booking AS b
                JOIN room AS r ON b.b_room = r.id
            WHERE b.id = :id
            """.trimIndent()

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        val booking: Booking? = jdbcTemplate.query(FIND_BY_ID_SQL, params, bookingRowMapper).firstOrNull()

        logger.debug { "findById finished: $booking" }
        return booking
    }

    fun findByIdWithGuests(id: UUID): Booking? {
        logger.trace { "findByIdWithGuests called with: id=$id" }

        val FIND_BY_ID_SQL = """
            SELECT b.id as booking_id, b_room, start_date, end_date, r.id as room_id, r.floor, r.number, r.capacity
            FROM booking AS b
                JOIN room AS r ON b.b_room = r.id
            WHERE b.id = :id
            """.trimIndent()

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        val booking: Booking? = jdbcTemplate.query(FIND_BY_ID_SQL, params, bookingRowMapper).firstOrNull()

        if (booking == null) {
            logger.debug { "findByIdWithGuests finished: not found" }
            return null
        }

        val FIND_BG_GUESTS_BY_BOOKING_ID_SQL = """
            SELECT g.id, surname, firstname, lastname, birthdate, phone_number, password_hash, role_id
            FROM booking_guests bg
            JOIN guest AS g ON g.id = bg.bg_guest
            WHERE bg_booking = :id
            """.trimIndent()

        val guests: MutableList<Guest> = jdbcTemplate.query(
            FIND_BG_GUESTS_BY_BOOKING_ID_SQL,
            params,
            guestRowMapper
        )
        booking.guests = guests
        logger.debug { "findByIdWithGuests finished: bookingId=${booking.id}, guests=${guests.size}" }
        return booking
    }

    fun findAll(): List<Booking> {
        logger.trace { "findAll called" }
        val FIND_ALL_SQL = """
            SELECT b.id as booking_id, b_room, start_date, end_date, r.id as room_id, r.floor, r.number, r.capacity
            FROM booking AS b
            JOIN room AS r ON b_room = r.id
            """.trimIndent()

        val bookings: List<Booking> = jdbcTemplate.query(FIND_ALL_SQL, bookingRowMapper)

        if (bookings.isEmpty()) {
            logger.debug { "findAll finished: no bookings" }
            return bookings
        }

        val FIND_GUESTS_FOR_ALL_BOOKINGS = """
            SELECT bg_booking, g.id, surname, firstname, lastname, birthdate, phone_number, password_hash, role_id
            FROM booking_guests bg
            JOIN guest g ON bg_guest = g.id
            """.trimIndent()

        val guestsMap: MutableMap<UUID, MutableList<Guest>> =
            jdbcTemplate.query(
                FIND_GUESTS_FOR_ALL_BOOKINGS,
                ::mapGuestWithBookingIdExtractor
            )

        if (guestsMap.isEmpty()) {
            logger.debug { "findAll finished: ${bookings.size} bookings, no guests" }
            return bookings
        }

        bookings.forEach { booking ->
            val guests: MutableList<Guest> = guestsMap.getOrDefault(booking.id, mutableListOf())
            booking.guests = guests
        }

        logger.debug { "findAll finished: ${bookings.size} bookings" }
        return bookings
    }

    fun findAvailableRooms(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Room> {
        logger.trace { "findAvailableRooms called with: startDate=$startDate, endDate=$endDate" }

        val FIND_AVAILABLE_ROOMS = """
            SELECT id, floor, number, capacity FROM room
            WHERE id NOT IN
            (SELECT DISTINCT b_room FROM booking
            WHERE start_date < :endDate AND end_date > :startDate)
            """.trimIndent()

        val params = MapSqlParameterSource()
        params.addValue("startDate", startDate)
        params.addValue("endDate", endDate)

        val result = jdbcTemplate.query(FIND_AVAILABLE_ROOMS, params, roomRowMapper)
        logger.debug { "findAvailableRooms finished: ${result.size} rooms" }
        return result
    }

    fun saveTransactional(entity: Booking, conn: Connection): Booking {
        logger.trace { "saveTransactional called with: id=${entity.id}, roomId=${entity.room.id}" }
        val template = getNamedTemplate(conn)

        val SAVE_SQL = "INSERT INTO booking (id, start_date, end_date, b_room) " +
                "VALUES (:id, :startDate, :endDate, :bRoom)"

        val params = MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("startDate", entity.startDate)
            .addValue("endDate", entity.endDate)
            .addValue("bRoom", entity.room.id)

        if (template.update(SAVE_SQL, params) > 0) {
            createBookingGuests(entity, conn)
            logger.debug { "saveTransactional finished: SUCCESS id=${entity.id}" }
            return entity
        } else {
            throw DaoException("BookingDao save method worked out incorrectly")
        }
    }

    fun checkAndCreate(entity: Booking): Boolean{
        logger.trace { "checkAndCreate called with: id=${entity.id}, roomId=${entity.room.id}" }
        val SQL = """
            INSERT INTO booking (id, start_date, end_date, b_room)
            SELECT :id, :startDate, :endDate, :roomId
            WHERE NOT EXISTS (
                SELECT 1 FROM booking
                WHERE booking.b_room = :roomId
                AND (:startDate < booking.end_date AND :endDate > booking.start_date)
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("startDate", entity.startDate)
            .addValue("endDate", entity.endDate)
            .addValue("bRoom", entity.room.id)

        val result = jdbcTemplate.update(SQL, params) > 0

        if (result){
            createBookingGuests(entity)
        }
        logger.debug { "checkAndCreate finished. Success status = $result" }
        return result
    }

    override fun create(entity: Booking): Booking {
        logger.trace { "create called with: id=${entity.id}, roomId=${entity.room.id}" }
        val SAVE_SQL = "INSERT INTO booking (id, start_date, end_date, b_room) " +
                "VALUES (:id, :startDate, :endDate, :bRoom)"

        val params = MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("startDate", entity.startDate)
            .addValue("endDate", entity.endDate)
            .addValue("bRoom", entity.room.id)

        if (jdbcTemplate.update(SAVE_SQL, params) > 0) {
            createBookingGuests(entity)
            logger.debug { "create finished: SUCCESS id=${entity.id}" }
            return entity
        } else {
            throw DaoException("BookingDao save method worked out incorrectly")
        }
    }

    fun update(entity: Booking, conn: Connection): Boolean {
        logger.trace { "update called with: id=${entity.id}, roomId=${entity.room.id} (transactional)" }
        val template = getNamedTemplate(conn)

        val UPDATE_BOOKING_SQL = """
            UPDATE booking
            SET start_date = :startDate, end_date = :endDate, b_room = :bRoom
            WHERE id = :id
            """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("startDate", entity.startDate)
            .addValue("endDate", entity.endDate)
            .addValue("bRoom", entity.room.id)

        val hasChanged = template.update(UPDATE_BOOKING_SQL, params) > 0
        val guestsChanged = updateBookingGuests(entity, conn)
        val result = hasChanged || guestsChanged
        logger.debug { "update finished: $result (bookingChanged=$hasChanged, guestsChanged=$guestsChanged)" }
        return result
    }

    override fun update(entity: Booking): Boolean {
        logger.trace { "update called with: id=${entity.id}, roomId=${entity.room.id}" }
        val UPDATE_BOOKING_SQL = """
            UPDATE booking
            SET start_date = :startDate, end_date = :endDate, b_room = :bRoom
            WHERE id = :id
            """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("startDate", entity.startDate)
            .addValue("endDate", entity.endDate)
            .addValue("bRoom", entity.room.id)

        val hasChanged = jdbcTemplate.update(UPDATE_BOOKING_SQL, params) > 0
        val guestsChanged = updateBookingGuests(entity)
        val result = hasChanged || guestsChanged
        logger.debug { "update finished: $result (bookingChanged=$hasChanged, guestsChanged=$guestsChanged)" }
        return result
    }

    fun deleteTransactional(id: UUID, conn: Connection): Boolean {
        logger.trace { "deleteTransactional called with: id=$id" }
        val template = getNamedTemplate(conn)

        val DELETE_BOOKING_GUESTS_SQL = """
            DELETE FROM booking_guests
            WHERE bg_booking = :id
            """.trimIndent()

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        template.update(DELETE_BOOKING_GUESTS_SQL, params)

        val DELETE_SQL = """
            DELETE FROM booking
            WHERE id = :id
            """.trimIndent()

        val result = template.update(DELETE_SQL, params) > 0
        logger.debug { "deleteTransactional finished: $result" }
        return result
    }

    fun isRoomAvailableExcludingCurrentBooking(
        roomId: Int,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        bookingId: UUID
    ): Boolean {
        logger.trace { "isRoomAvailableExcludingCurrentBooking called with: roomId=$roomId, bookingId=$bookingId, startDate=$startDate, endDate=$endDate" }
        val CHECK_SQL = """
            SELECT COUNT(*) > 0
            FROM booking
            WHERE b_room = :roomId
              AND start_date < :endDate
              AND end_date > :startDate
              AND id != :bookingId
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("roomId", roomId)
            .addValue("startDate", startDate)
            .addValue("endDate", endDate)
            .addValue("bookingId", bookingId)

        val result = !(jdbcTemplate.queryForObject(CHECK_SQL, params, Boolean::class.java) ?: false)
        logger.debug { "isRoomAvailableExcludingCurrentBooking finished: $result" }
        return result
    }

    override fun delete(id: UUID): Boolean {
        logger.trace { "delete called with: id=$id" }
        val DELETE_BOOKING_GUESTS_SQL = """
            DELETE FROM booking_guests
            WHERE bg_booking = :id
            """.trimIndent()

        val params = MapSqlParameterSource()
        params.addValue("id", id)

        jdbcTemplate.update(DELETE_BOOKING_GUESTS_SQL, params)

        val DELETE_SQL = """
            DELETE FROM booking
            WHERE id = :id
            """.trimIndent()

        val result = jdbcTemplate.update(DELETE_SQL, params) > 0
        logger.debug { "delete finished: $result" }
        return result
    }

    private fun createBookingGuests(booking: Booking, conn: Connection) {
        logger.trace { "createBookingGuests (transactional) called with: bookingId=${booking.id}, guests=${booking.guests.size}" }
        val template = getNamedTemplate(conn)

        val SAVE_BG_SQL = "INSERT INTO booking_guests (id, bg_guest, bg_booking) " +
                "VALUES (:id, :bgGuest, :bgBooking)"

        val paramsList = ArrayList<SqlParameterSource>()
        for (guest in booking.guests) {
            val bg_params = MapSqlParameterSource()
            val bg_uuid = UUID.randomUUID()
            bg_params.addValue("id", bg_uuid)
            bg_params.addValue("bgGuest", guest.id)
            bg_params.addValue("bgBooking", booking.id)

            paramsList.add(bg_params)
        }

        val results: IntArray = template.batchUpdate(SAVE_BG_SQL, paramsList.toTypedArray())
        for (res in results) {
            if (res == 0) throw DaoException("createBookingGuests method worked out incorrectly")
        }
        logger.trace { "createBookingGuests (transactional) finished: ${results.size} guests saved" }
    }

    private fun createBookingGuests(booking: Booking) {
        logger.trace { "createBookingGuests called with: bookingId=${booking.id}, guests=${booking.guests.size}" }
        val SAVE_BG_SQL = "INSERT INTO booking_guests (id, bg_guest, bg_booking) " +
                "VALUES (:id, :bgGuest, :bgBooking)"

        val paramsList = ArrayList<SqlParameterSource>()
        for (guest in booking.guests) {
            val bg_params = MapSqlParameterSource()
            val bg_uuid = UUID.randomUUID()
            bg_params.addValue("id", bg_uuid)
            bg_params.addValue("bgGuest", guest.id)
            bg_params.addValue("bgBooking", booking.id)

            paramsList.add(bg_params)
        }

        val results: IntArray = jdbcTemplate.batchUpdate(SAVE_BG_SQL, paramsList.toTypedArray())
        for (res in results) {
            if (res == 0) throw DaoException("createBookingGuests method worked out incorrectly")
        }
        logger.trace { "createBookingGuests finished: ${results.size} guests saved" }
    }

    private fun mapGuestWithBookingIdExtractor(rs: java.sql.ResultSet): MutableMap<UUID, MutableList<Guest>> {
        val result = java.util.HashMap<UUID, MutableList<Guest>>()
        while (rs.next()) {
            val guest = guestRowMapper.mapRow(rs, 0)
            val bookingId = rs.getObject<UUID>("bg_booking", UUID::class.java)
            val guestList: MutableList<Guest> = result.getOrDefault(bookingId, ArrayList<Guest>())
            guestList.add(guest)
            result[bookingId] = guestList
        }
        return result
    }

    private fun updateBookingGuests(booking: Booking, conn: Connection): Boolean {
        logger.trace { "updateBookingGuests (transactional) called with: bookingId=${booking.id}, newGuests=${booking.guests.size}" }
        val template = getNamedTemplate(conn)

        val GET_BOOKING_GUESTS_IDS_SQL = """
            SELECT id, bg_guest FROM booking_guests
            WHERE booking_guests.bg_booking = :id
            """.trimIndent()

        val newGuests = booking.guests
        val params = MapSqlParameterSource()
        params.addValue("id", booking.id)

        val oldGuests: MutableList<BookingGuest> = template.query<BookingGuest>(
            GET_BOOKING_GUESTS_IDS_SQL,
            params,
            { rs, _ ->
                BookingGuest(
                    id = rs.getObject<UUID>("id", UUID::class.java),
                    guestId = rs.getObject<UUID>("bg_guest", UUID::class.java)
                )
            })

        val result = if (newGuests.isEmpty() && oldGuests.isEmpty()) {
            false
        } else if (oldGuests.isEmpty()) {
            addBookingGuests(booking.id, newGuests.map { it.id }.toMutableList(), conn)
        } else if (areGuestListsEqual(
                oldGuests.map { it.guestId }.toMutableList(),
                newGuests.map { it.id }.toMutableList()
            )
        ) {
            false
        } else {
            val newGuestIds: List<UUID> = newGuests.map { it.id }
            val oldGuestIds = oldGuests.map { it.guestId }

            val bookingGuestsToDelete: MutableList<BookingGuest> = oldGuests.stream()
                .filter { bg: BookingGuest -> !newGuestIds.contains(bg.guestId) }
                .toList()

            val guestsToAdd: MutableList<UUID> = newGuestIds
                .filter({ guestId -> !oldGuestIds.contains(guestId) }).toMutableList()

            val hasChanged = deleteBookingGuests(bookingGuestsToDelete, conn)
            hasChanged || addBookingGuests(booking.id, guestsToAdd, conn)
        }
        logger.trace { "updateBookingGuests (transactional) finished: changed=$result" }
        return result
    }

    private fun updateBookingGuests(booking: Booking): Boolean {
        logger.trace { "updateBookingGuests called with: bookingId=${booking.id}, newGuests=${booking.guests.size}" }
        val GET_BOOKING_GUESTS_IDS_SQL = """
            SELECT id, bg_guest FROM booking_guests
            WHERE booking_guests.bg_booking = :id
            """.trimIndent()

        val newGuests = booking.guests
        val params = MapSqlParameterSource()
        params.addValue("id", booking.id)

        val oldGuests: MutableList<BookingGuest> = jdbcTemplate.query<BookingGuest>(
            GET_BOOKING_GUESTS_IDS_SQL,
            params,
            { rs, _ ->
                BookingGuest(
                    id = rs.getObject<UUID>("id", UUID::class.java),
                    guestId = rs.getObject<UUID>("bg_guest", UUID::class.java)
                )
            })

        val result = if (newGuests.isEmpty() && oldGuests.isEmpty()) {
            false
        } else if (oldGuests.isEmpty()) {
            addBookingGuests(booking.id, newGuests.map { it.id }.toMutableList())
        } else if (areGuestListsEqual(
                oldGuests.map { it.guestId }.toMutableList(),
                newGuests.map { it.id }.toMutableList()
            )
        ) {
            false
        } else {
            val newGuestIds: List<UUID> = newGuests.map { it.id }
            val oldGuestIds = oldGuests.map { it.guestId }

            val bookingGuestsToDelete: MutableList<BookingGuest> = oldGuests.stream()
                .filter { bg: BookingGuest -> !newGuestIds.contains(bg.guestId) }
                .toList()

            val guestsToAdd: MutableList<UUID> = newGuestIds
                .filter({ guestId -> !oldGuestIds.contains(guestId) }).toMutableList()

            val hasChanged = deleteBookingGuests(bookingGuestsToDelete)
            hasChanged || addBookingGuests(booking.id, guestsToAdd)
        }
        logger.trace { "updateBookingGuests finished: changed=$result" }
        return result
    }

    private fun deleteBookingGuests(bookingGuestList: MutableList<BookingGuest>, conn: Connection): Boolean {
        logger.trace { "deleteBookingGuests (transactional) called with: count=${bookingGuestList.size}" }
        val template = getNamedTemplate(conn)

        val DELETE_OLD_BOOKING_GUESTS_SQL = """
            DELETE FROM booking_guests
            WHERE booking_guests.id = :id
            """.trimIndent()

        val paramsList = ArrayList<SqlParameterSource>()
        for (bookingGuest in bookingGuestList) {
            val bg_params = MapSqlParameterSource()
            bg_params.addValue("id", bookingGuest.id)
            paramsList.add(bg_params)
        }

        val results: IntArray =
            template.batchUpdate(DELETE_OLD_BOOKING_GUESTS_SQL, paramsList.toTypedArray<SqlParameterSource>())
        for (res in results) {
            if (res == 0) {
                logger.trace { "deleteBookingGuests (transactional) finished: false" }
                return false
            }
        }
        logger.trace { "deleteBookingGuests (transactional) finished: true" }
        return true
    }

    private fun deleteBookingGuests(bookingGuestList: MutableList<BookingGuest>): Boolean {
        logger.trace { "deleteBookingGuests called with: count=${bookingGuestList.size}" }
        val DELETE_OLD_BOOKING_GUESTS_SQL = """
            DELETE FROM booking_guests
            WHERE booking_guests.id = :id
            """.trimIndent()

        val paramsList = ArrayList<SqlParameterSource>()
        for (bookingGuest in bookingGuestList) {
            val bg_params = MapSqlParameterSource()
            bg_params.addValue("id", bookingGuest.id)
            paramsList.add(bg_params)
        }

        val results: IntArray =
            jdbcTemplate.batchUpdate(DELETE_OLD_BOOKING_GUESTS_SQL, paramsList.toTypedArray<SqlParameterSource>())
        for (res in results) {
            if (res == 0) {
                logger.trace { "deleteBookingGuests finished: false" }
                return false
            }
        }
        logger.trace { "deleteBookingGuests finished: true" }
        return true
    }

    private fun areGuestListsEqual(
        list1: MutableList<UUID>,
        list2: MutableList<UUID>
    ): Boolean {
        if (list1.size != list2.size) return false
        val sorted1 = list1.stream().sorted().toList()
        val sorted2 = list2.stream().sorted().toList()
        return sorted1 == sorted2
    }

    private fun addBookingGuests(
        bookingId: UUID,
        guestsIds: MutableList<UUID>,
        conn: Connection
    ): Boolean {
        logger.trace { "addBookingGuests (transactional) called with: bookingId=$bookingId, guestIds=$guestsIds" }
        val template = getNamedTemplate(conn)

        val ADD_NEW_BOOKING_GUESTS_SQL = """
            INSERT INTO booking_guests (id, bg_guest, bg_booking) VALUES
                (:id, :bgGuest, :bgBooking)
            """.trimIndent()

        val paramsList = ArrayList<SqlParameterSource>()
        for (guestId in guestsIds) {
            val bg_params = MapSqlParameterSource()
            val bg_uuid = UUID.randomUUID()
            bg_params.addValue("id", bg_uuid)
            bg_params.addValue("bgGuest", guestId)
            bg_params.addValue("bgBooking", bookingId)
            paramsList.add(bg_params)
        }

        val results: IntArray = template.batchUpdate(ADD_NEW_BOOKING_GUESTS_SQL, paramsList.toTypedArray())
        for (res in results) {
            if (res == 0) {
                logger.trace { "addBookingGuests (transactional) finished: false" }
                return false
            }
        }
        logger.trace { "addBookingGuests (transactional) finished: true" }
        return true
    }

    private fun addBookingGuests(
        bookingId: UUID,
        guestsIds: MutableList<UUID>
    ): Boolean {
        logger.trace { "addBookingGuests called with: bookingId=$bookingId, guestIds=$guestsIds" }
        val ADD_NEW_BOOKING_GUESTS_SQL = """
            INSERT INTO booking_guests (id, bg_guest, bg_booking) VALUES
                (:id, :bgGuest, :bgBooking)
            """.trimIndent()

        val paramsList = ArrayList<SqlParameterSource>()
        for (guestId in guestsIds) {
            val bg_params = MapSqlParameterSource()
            val bg_uuid = UUID.randomUUID()
            bg_params.addValue("id", bg_uuid)
            bg_params.addValue("bgGuest", guestId)
            bg_params.addValue("bgBooking", bookingId)
            paramsList.add(bg_params)
        }

        val results: IntArray = jdbcTemplate.batchUpdate(ADD_NEW_BOOKING_GUESTS_SQL, paramsList.toTypedArray())
        for (res in results) {
            if (res == 0) {
                logger.trace { "addBookingGuests finished: false" }
                return false
            }
        }
        logger.trace { "addBookingGuests finished: true" }
        return true
    }

    private data class BookingGuest(
        var id: UUID,
        var guestId: UUID
    )
}
