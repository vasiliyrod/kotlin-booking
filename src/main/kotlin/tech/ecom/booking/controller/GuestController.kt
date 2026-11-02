package tech.ecom.booking.controller

import com.sun.net.httpserver.HttpExchange
import controller.BaseController
import controller.Router
import dto.GuestEditDto
import dto.GuestReadDto
import dto.OperationResult
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.dto.guest.GuestChangeRoleDto
import tech.ecom.booking.service.AuthService
import tech.ecom.booking.service.GuestService
import java.util.UUID

class GuestController(private val guestService: GuestService) : BaseController() {

    override fun configureHandlers(router: Router) {
        router.get("guest/", ::findById, Role.ADMIN)
        router.put("guest", ::update, Role.USER)
        router.put("guest/role", ::changeRole, Role.ADMIN)
    }

    fun findById(httpExchange: HttpExchange) {
        val id: UUID? = extractUuid(httpExchange)

        val result: OperationResult<GuestReadDto>

        if (id == null) {
            logger.warn { "Request without id from ${httpExchange.remoteAddress}" }
            result = OperationResult.badRequest("Invalid id format in request")

        } else {
            result = guestService.findById(id)
        }

        when (result) {
            is OperationResult.Success -> writeResultSuccess(httpExchange, result)
            is OperationResult.Error -> writeResultError(httpExchange, result)
        }
    }

    fun update(httpExchange: HttpExchange) {
        val parseResult: OperationResult<GuestEditDto> = parseRequestBody<GuestEditDto>(httpExchange)


        when (parseResult) {
            is OperationResult.Success -> {
                val data = parseResult.data
                val userInfo = AuthService.getUserInfoFromAccessToken(httpExchange)
                val result = guestService.update(data, userInfo.id)

                when (result) {
                    is OperationResult.Success -> writeResultSuccess(httpExchange, result)
                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(httpExchange, parseResult)
            }
        }
    }

    fun changeRole(httpExchange: HttpExchange) {
        val parseResult: OperationResult<GuestChangeRoleDto> = parseRequestBody<GuestChangeRoleDto>(httpExchange)

        when (parseResult) {
            is OperationResult.Success -> {
                val result = guestService.changeRole(parseResult.data)

                when (result) {
                    is OperationResult.Success -> writeResultSuccess(httpExchange, result)
                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }

            is OperationResult.Error -> writeResultError(httpExchange, parseResult)
        }
    }
}