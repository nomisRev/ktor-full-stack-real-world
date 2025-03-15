package org.jetbrains.realworld

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.jetbrains.realworld.error.ErrorResponse
import org.jetbrains.realworld.user.UserLogin
import org.jetbrains.realworld.user.NewUser
import org.jetbrains.realworld.user.NewUserRequest
import org.jetbrains.realworld.user.UserUpdate

fun Application.configureValidation() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            val errorMessages = cause.reasons.associateWith { listOf(it) }
            call.respond(HttpStatusCode.BadRequest, ErrorResponse.fromFieldErrors(errorMessages))
        }
    }
    install(RequestValidation) {
        validate<NewUserRequest> { request ->
            val user = request.user
            when {
                user.email.isBlank() -> ValidationResult.Invalid("Email cannot be blank")
                !user.email.contains("@") -> ValidationResult.Invalid("Invalid email format")
                user.username.isBlank() -> ValidationResult.Invalid("Username cannot be blank")
                user.username.length < 3 -> ValidationResult.Invalid("Username must be at least 3 characters")
                user.password.isBlank() -> ValidationResult.Invalid("Password cannot be blank")
                user.password.length < 8 -> ValidationResult.Invalid("Password must be at least 8 characters")
                else -> ValidationResult.Valid
            }
        }

        validate<UserLogin> { user ->
            when {
                user.email.isBlank() -> ValidationResult.Invalid("Email cannot be blank")
                !user.email.contains("@") -> ValidationResult.Invalid("Invalid email format")
                user.password.isBlank() -> ValidationResult.Invalid("Password cannot be blank")
                else -> ValidationResult.Valid
            }
        }

        validate<UserUpdate> { user ->
            when {
                user.email != null && user.email.isBlank() -> ValidationResult.Invalid("Email cannot be blank")
                user.email != null && !user.email.contains("@") -> ValidationResult.Invalid("Invalid email format")
                user.username != null && user.username.isBlank() -> ValidationResult.Invalid("Username cannot be blank")
                user.username != null && user.username.length < 3 -> ValidationResult.Invalid("Username must be at least 3 characters")
                user.password != null && user.password.isBlank() -> ValidationResult.Invalid("Password cannot be blank")
                user.password != null && user.password.length < 8 -> ValidationResult.Invalid("Password must be at least 8 characters")
                else -> ValidationResult.Valid
            }
        }
    }
}
