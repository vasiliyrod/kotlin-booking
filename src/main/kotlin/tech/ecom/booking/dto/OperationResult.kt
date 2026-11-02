package dto

sealed class OperationResult<out T>(
    open val statusCode: Int
) {
    data class Success<T>(
        val data: T,
        override val statusCode: Int = 200

    ) : OperationResult<T>(statusCode)

    data class Error(
        val message: String,
        override val statusCode: Int = 400
    ) : OperationResult<Nothing>(statusCode)

    companion object {
        fun <T> created(data: T) = Success(data, 201)
        fun <T> ok(data: T) = Success(data, 200)

        fun badRequest(message: String) = Error(message, 400)
        fun notFound(message: String) = Error(message, 404)
        fun conflict(message: String) = Error(message, 409)
        fun unauthorized(message: String = "Unauthorized") = Error(message, 401)
        fun forbidden(message: String = "Forbidden") = Error(message, 403)
        fun internalError(message: String = "Internal Server Error") = Error(message, 500)
    }
}