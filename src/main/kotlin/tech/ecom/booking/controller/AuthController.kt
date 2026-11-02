package tech.ecom.booking.controller

import com.sun.net.httpserver.HttpExchange
import controller.BaseController
import controller.Router
import dto.OperationResult
import tech.ecom.booking.data.entity.Role
import tech.ecom.booking.dto.auth.AuthLoginDto
import tech.ecom.booking.dto.guest.GuestCreateDto
import tech.ecom.booking.dto.SuccessResponseDto
import tech.ecom.booking.dto.auth.AccessTokenDto
import tech.ecom.booking.dto.auth.RegisterResultDto
import tech.ecom.booking.dto.auth.AuthChangePasswordDto
import tech.ecom.booking.service.AuthService
import tech.ecom.booking.service.GuestService
import tech.ecom.booking.utils.AppConfig

class AuthController(
    private val authService: AuthService,
    private val guestService: GuestService
) : BaseController() {

    override fun configureHandlers(router: Router) {
        router.post("auth/register", ::register, Role.USER)
        router.post("auth/login", ::login, Role.USER)
        router.post("auth/logout", ::logout, Role.USER)
        router.post("auth/logout-from-all", ::logoutFromAll, Role.USER)
        router.post("auth/refresh", ::refreshAccessToken, Role.USER)
        router.put(
            "auth/change-password",
            ::changePassword,
            Role.USER
        )
    }

    fun register(httpExchange: HttpExchange) {
        val parseResult: OperationResult<GuestCreateDto> = parseRequestBody<GuestCreateDto>(httpExchange)

        when (parseResult) {
            is OperationResult.Success -> {
                val data = parseResult.data


                when (val result = guestService.create(data)) {
                    is OperationResult.Success -> {
                        val guest = result.data

                        val refreshToken = authService.createRefreshToken(guest.id!!)
                        setRefreshToken(httpExchange, refreshToken)

                        val accessToken = authService.createAccessToken(guest.id, guest.role)
                        val registerResultDto = RegisterResultDto(
                            guest,
                            AccessTokenDto(accessToken, "Successfully created access token")
                        )
                        writeResultSuccess(httpExchange, OperationResult.created(registerResultDto))
                    }

                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }

            is OperationResult.Error -> {
                writeResultError(httpExchange, parseResult)
            }
        }
    }

    fun login(httpExchange: HttpExchange) {
        val parseResult: OperationResult<AuthLoginDto> = parseRequestBody<AuthLoginDto>(httpExchange)

        when (parseResult) {
            is OperationResult.Error -> {
                writeResultError(httpExchange, parseResult)
            }

            is OperationResult.Success<AuthLoginDto> -> {

                when (val result = authService.authenticate(parseResult.data)) {
                    is OperationResult.Success -> {
                        setRefreshToken(httpExchange, result.data.refreshToken)

                        writeResultSuccess(
                            httpExchange,
                            OperationResult.ok(AccessTokenDto(result.data.accessToken, "Successfully logged in"))
                        )
                    }

                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }
        }
    }

    fun logout(httpExchange: HttpExchange) {
        val token = AuthService.getCurrentUserRefreshToken(httpExchange)

        val result = authService.deleteRefreshToken(token)

        when (result) {
            is OperationResult.Success -> writeResultSuccess(httpExchange, result)
            is OperationResult.Error -> writeResultError(httpExchange, result)
        }
    }


    fun logoutFromAll(httpExchange: HttpExchange) {
        val guestId = AuthService.getUserInfoFromAccessToken(httpExchange).id

        val result = authService.deleteAllRefreshTokensFromGuest(guestId)

        when (result) {
            is OperationResult.Success -> writeResultSuccess(httpExchange, result)
            is OperationResult.Error -> writeResultError(httpExchange, result)
        }
    }

    fun changePassword(httpExchange: HttpExchange) {
        val userInfo = AuthService.getUserInfoFromAccessToken(httpExchange)

        val parseResult: OperationResult<AuthChangePasswordDto> = parseRequestBody<AuthChangePasswordDto>(httpExchange)

        when (parseResult) {
            is OperationResult.Error -> writeResultError(httpExchange, parseResult)

            is OperationResult.Success<AuthChangePasswordDto> -> {
                val dto = parseResult.data
                val validation = dto.validate()
                if (!validation.isValid) {
                    writeResultError(httpExchange, OperationResult.badRequest(validation.errors.joinToString(", ")))
                    return
                }

                when (val result = authService.changePassword(userInfo.id, dto.oldPassword, dto.newPassword)) {
                    is OperationResult.Success -> writeResultSuccess(httpExchange, OperationResult.ok(SuccessResponseDto("Password changed successfully")))
                    is OperationResult.Error -> writeResultError(httpExchange, result)
                }
            }
        }
    }

    fun refreshAccessToken(httpExchange: HttpExchange) {
        val refreshToken = AuthService.getCurrentUserRefreshToken(httpExchange)

        if (refreshToken.isBlank()) {
            writeResultError(httpExchange, OperationResult.unauthorized("Refresh token not found"))
            return
        }

        when (val result = authService.validateRefreshToken(refreshToken)) {
            is OperationResult.Success -> {
                val userInfo = result.data

                val newAccessToken = authService.createAccessToken(userInfo.id, userInfo.role)

                writeResultSuccess(
                    httpExchange,
                    OperationResult.ok(AccessTokenDto(newAccessToken, "Access token refreshed"))
                )
            }

            is OperationResult.Error -> writeResultError(httpExchange, result)
        }
    }

    private fun setRefreshToken(httpExchange: HttpExchange, token: String) {
        val cookie: String = buildString {
            append("refresh_token=$token; ")
            append("Max-Age=${AppConfig.refreshTokenTtlDays * 24 * 60 * 60}; ")
            append("Path=/; ")
            append("Secure; HttpOnly; ")
        }

        httpExchange.responseHeaders.set("Set-Cookie", cookie)
    }

}