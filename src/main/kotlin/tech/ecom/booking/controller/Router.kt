package controller

import com.sun.net.httpserver.HttpExchange
import dto.OperationResult
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.dto.auth.UserInfoDto
import tech.ecom.booking.service.AuthService
import utils.Handler

class Router(
    private val authService: AuthService
) {
    companion object {
        val idRegex = Regex("""/\d+$""")
        val uuidRegex = Regex("""/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$""")
    }

    private val routes = mutableMapOf<String, HandlerInfo>()

    fun get(path: String, handler: Handler, role: Role) {
        if (path.contains(":")) {
            val pathWithParam = path.substringBefore(":")
            routes["GET:$pathWithParam"] = HandlerInfo(handler, role)
        } else {
            routes["GET:$path"] = HandlerInfo(handler, role)
        }
    }

    fun post(path: String, handler: Handler, role: Role) {
        if (path.contains(":")) {
            val pathWithParam = path.substringBefore(":")
            routes["POST:$pathWithParam"] = HandlerInfo(handler, role)
        } else {
            routes["POST:$path"] = HandlerInfo(handler, role)
        }
    }

    fun put(path: String, handler: Handler, role: Role) {
        if (path.contains(":")) {
            val pathWithParam = path.substringBefore(":")
            routes["PUT:$pathWithParam"] = HandlerInfo(handler, role)
        } else {
            routes["PUT:$path"] = HandlerInfo(handler, role)
        }
    }

    fun delete(path: String, handler: Handler, role: Role) {
        if (path.contains(":")) {
            val pathWithParam = path.substringBefore(":")
            routes["DELETE:$pathWithParam"] = HandlerInfo(handler, role)
        } else {
            routes["DELETE:$path"] = HandlerInfo(handler, role)
        }
    }

    fun route(exchange: HttpExchange): Boolean {
        val method = exchange.requestMethod
        val rawPath = exchange.requestURI.path.removePrefix("/")

        val normalizedPath = rawPath
            .replace(uuidRegex, "/")
            .replace(idRegex, "/")

        val key = "$method:$normalizedPath"

        val handlerInfo = routes[key]
        if (handlerInfo != null) {
            executeHandler(handlerInfo, exchange, method, rawPath)
            return true
        }
        return false
    }

    fun registerControllers(vararg controllers: BaseController) {
        controllers.forEach { it.configureHandlers(this) }
    }

    private fun executeHandler(handlerInfo: HandlerInfo, exchange: HttpExchange, method: String, rawPath: String) {
        if (!rawPath.startsWith("auth/")) {

            val authHeader = exchange.requestHeaders.getFirst("Authorization")
            val token = authHeader?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }

            val authResult =
                if (token != null) {
                    authService.validateAccessToken(token)
                } else {
                    OperationResult.unauthorized()
                }

            if (authResult is OperationResult.Error) {
                if (method == "GET") {
                    exchange.responseHeaders.set("Location", "/login")
                    exchange.sendResponseHeaders(302, -1)
                } else {
                    val json = """{"error": "Unauthorized"}"""
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                    exchange.sendResponseHeaders(401, bytes.size.toLong())
                    exchange.responseBody.use { outputStream ->
                        outputStream.write(bytes)
                        outputStream.flush()
                    }
                }
                return
            }

            val userInfo = (authResult as OperationResult.Success<UserInfoDto>).data
            if (!userInfo.role.hasRightsOf(handlerInfo.roleAccess)) {
                val json = """{"error": "Forbidden"}"""
                val bytes = json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                exchange.sendResponseHeaders(403, bytes.size.toLong())
                exchange.responseBody.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }

                return
            }
        }

        handlerInfo.handler(exchange)
    }

    private data class HandlerInfo(
        val handler: Handler,
        val roleAccess: Role
    )
}