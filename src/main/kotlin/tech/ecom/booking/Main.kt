package tech.ecom.booking

import com.sun.net.httpserver.HttpServer
import controller.BookingController
import controller.DispatcherHandler
import controller.RoomController
import controller.Router
import tech.ecom.booking.controller.AuthController
import edu.booking.hotel_booking.dao.BookingDao
import edu.booking.hotel_booking.dao.GuestDao
import edu.booking.hotel_booking.dao.RoomDao
import io.github.oshai.kotlinlogging.KotlinLogging
import tech.ecom.booking.data.repository.RefreshTokenDao
import mapper.BookingRowMapper
import mapper.GuestRowMapper
import mapper.RoomRowMapper
import service.RoomService
import tech.ecom.booking.controller.GuestController
import tech.ecom.booking.service.AuthService
import tech.ecom.booking.service.BookingService
import tech.ecom.booking.service.GuestService
import tech.ecom.booking.utils.AppConfig
import utils.DataBaseConfig
import utils.LiquibaseUtil
import java.net.InetSocketAddress

fun main() {
    val server = startServer(AppConfig.serverPort)
    val actualPort = server.address.port
    val logger = KotlinLogging.logger { }

    println("Server started on http://localhost:$actualPort")
    logger.info { "Server started on http://localhost:$actualPort" }
}

fun startServer(requestedPort: Int): HttpServer {
    LiquibaseUtil.applyMigrations()

    val jdbcTemplate = DataBaseConfig.namedParameterJdbcTemplate

    val roomRowMapper = RoomRowMapper()
    val guestRowMapper = GuestRowMapper()
    val bookingRowMapper = BookingRowMapper()

    val roomDao = RoomDao(jdbcTemplate, roomRowMapper)
    val roomService = RoomService(roomDao)
    val roomController = RoomController(roomService)

    val guestDao = GuestDao(jdbcTemplate, guestRowMapper)
    val guestService = GuestService(guestDao)
    val guestController = GuestController(guestService)

    val bookingDao = BookingDao(
        jdbcTemplate = jdbcTemplate,
        bookingRowMapper = bookingRowMapper,
        guestRowMapper = guestRowMapper,
        roomRowMapper = roomRowMapper
    )
    val bookingService = BookingService(bookingDao, guestService, roomService, jdbcTemplate.jdbcTemplate.dataSource!!)
    val bookingController = BookingController(bookingService)

    val refreshTokenDao = RefreshTokenDao(jdbcTemplate)
    val authService = AuthService(refreshTokenDao, guestDao)
    val authController = AuthController(authService, guestService)

    val router = Router(authService)
    router.registerControllers(
        roomController,
        guestController,
        bookingController,
        authController
    )

    val server = HttpServer.create(InetSocketAddress(requestedPort), 0)
    server.createContext("/", DispatcherHandler(router))
    server.start()

    return server
}
