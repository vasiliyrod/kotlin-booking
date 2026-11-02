package tech.ecom.booking.dto

import dto.OperationResult

class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun toOperationResult(): OperationResult<Nothing>? {
        return if (!isValid) {
            OperationResult.badRequest(errors.joinToString(", "))
        } else null
    }

    companion object {
        fun success() = ValidationResult(true)
        fun error(error: String) = ValidationResult(false, listOf(error))
        fun errors(errorList: List<String>) = ValidationResult(false, errorList)
    }
}