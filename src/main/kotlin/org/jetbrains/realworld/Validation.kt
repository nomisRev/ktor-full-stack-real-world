package org.jetbrains.realworld

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.jetbrains.realworld.error.ErrorResponse

/**
 * Configures request validation for the application.
 */
fun Application.configureValidation() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            val errorMessages = cause.reasons.associateWith { listOf(it) }
            call.respond(HttpStatusCode.BadRequest, ErrorResponse.fromFieldErrors(errorMessages))
        }
    }
    install(RequestValidation) {
        // Validation rules will be added in future sprints
        // Example:
        // validate<UserRegistrationRequest> { request ->
        //     when {
        //         request.email.isBlank() -> ValidationResult.Invalid("Email cannot be blank")
        //         !request.email.contains("@") -> ValidationResult.Invalid("Invalid email format")
        //         request.password.length < 8 -> ValidationResult.Invalid("Password must be at least 8 characters")
        //         else -> ValidationResult.Valid
        //     }
        // }
    }
}