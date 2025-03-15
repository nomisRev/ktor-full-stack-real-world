package org.jetbrains.realworld.error

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val errors: Map<String, List<String>>
) {
    companion object {
        fun fromMessage(message: String): ErrorResponse =
            ErrorResponse(mapOf("message" to listOf(message)))

        fun fromFieldError(field: String, message: String): ErrorResponse =
            ErrorResponse(mapOf(field to listOf(message)))

        fun fromFieldErrors(errors: Map<String, List<String>>): ErrorResponse =
            ErrorResponse(errors)
    }
}