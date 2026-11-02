package controller

import com.sun.net.httpserver.HttpExchange
import dto.BookingAvailableRoomsDto
import dto.BookingCreateDto
import dto.BookingEditDto
import dto.BookingReadDto
import dto.OperationResult
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.service.AuthService
import tech.ecom.booking.service.BookingService
import java.util.UUID

class BookingController(private val bookingService: BookingService) : BaseController() {

    override fun configureHandlers(router: Router) {
        router.get("booking/", ::findById, Role.USER)
        router.get("booking/available-rooms", ::findAvailableRooms, Role.USER)
        router.post("booking", ::create, Role.USER)
        router.put("booking", ::update, Role.USER)
    }

    fun findById(exchange: HttpExchange) {
        val id: UUID? = extractUuid(exchange)

        val result: OperationResult<BookingReadDto>

        if (id == null) {
            logger.warn { "Request without id from ${exchange.remoteAddress}" }
            result = OperationResult.badRequest("Invalid id format in request")

        } else {
            val guestId = AuthService.getUserInfoFromAccessToken(exchange).id
            result = bookingService.findById(id, guestId)
        }

        when (result) {
            is OperationResult.Success -> writeResultSuccess(exchange, result)
            is OperationResult.Error -> writeResultError(exchange, result)
        }
    }

    fun findAvailableRooms(exchange: HttpExchange) {
        val parseResult: OperationResult<BookingAvailableRoomsDto> =
            parseRequestBody<BookingAvailableRoomsDto>(exchange)

        when (parseResult) {
            is OperationResult.Success -> {
                val data = parseResult.data
                val result = bookingService.findAvailableRooms(data)

                when (result) {
                    is OperationResult.Success -> writeResultSuccess(exchange, result)
                    is OperationResult.Error -> writeResultError(exchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(exchange, parseResult)
            }
        }
    }

    fun create(exchange: HttpExchange) {
        val parseResult: OperationResult<BookingCreateDto> =
            parseRequestBody<BookingCreateDto>(exchange)

        when (parseResult) {
            is OperationResult.Success -> {
                val data = parseResult.data
                val guestId = AuthService.getUserInfoFromAccessToken(exchange).id
                val result = bookingService.create(data, guestId)

                when (result) {
                    is OperationResult.Success -> writeResultSuccess(exchange, result)
                    is OperationResult.Error -> writeResultError(exchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(exchange, parseResult)
            }
        }
    }

    fun update(exchange: HttpExchange) {
        val parseResult: OperationResult<BookingEditDto> =
            parseRequestBody<BookingEditDto>(exchange)

        when (parseResult) {
            is OperationResult.Success -> {
                val data = parseResult.data
                val guestId = AuthService.getUserInfoFromAccessToken(exchange).id
                val result = bookingService.update(data, guestId)

                when (result) {
                    is OperationResult.Success -> writeResultSuccess(exchange, result)
                    is OperationResult.Error -> writeResultError(exchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(exchange, parseResult)
            }
        }
    }
}