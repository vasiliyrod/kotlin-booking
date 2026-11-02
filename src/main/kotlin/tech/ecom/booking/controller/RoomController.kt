package controller

import com.sun.net.httpserver.HttpExchange
import dto.OperationResult
import dto.RoomCreateDto
import dto.RoomEditDto
import dto.RoomReadDto
import service.RoomService
import tech.ecom.booking.data.entity.Role

class RoomController(private val roomService: RoomService) : BaseController() {
    override fun configureHandlers(router: Router) {
        router.get("room/", ::findById, Role.USER)
        router.post("room", ::create, Role.ADMIN)
        router.put("room", ::update, Role.ADMIN)
    }

    fun findById(httpExchange: HttpExchange) {
        val id: Int? = extractIntId(httpExchange)

        val result: OperationResult<RoomReadDto>

        if (id == null) {
            logger.warn { "Request without id from ${httpExchange.remoteAddress}" }
            result = OperationResult.badRequest("Id not provided in request")

        } else {
            result = roomService.findById(id)
        }

        when (result){
            is OperationResult.Success -> writeResultSuccess(httpExchange, result)
            is OperationResult.Error -> writeResultError(httpExchange, result)
        }
    }

    fun create(httpExchange: HttpExchange) {
        val parseResult: OperationResult<RoomCreateDto> = parseRequestBody<RoomCreateDto>(httpExchange)

        when (parseResult){
            is OperationResult.Success -> {
                val data = parseResult.data
                val result = roomService.create(data)

                when (result){
                    is OperationResult.Success -> writeResultSuccess(httpExchange, result)
                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(httpExchange, parseResult)
            }
        }
    }

    fun update(httpExchange: HttpExchange) {
        val parseResult: OperationResult<RoomEditDto> = parseRequestBody<RoomEditDto>(httpExchange)

        when (parseResult){
            is OperationResult.Success -> {
                val data = parseResult.data
                val result = roomService.update(data)

                when (result){
                    is OperationResult.Success -> writeResultSuccess(httpExchange, result)
                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(httpExchange, parseResult)
            }
        }
    }
}