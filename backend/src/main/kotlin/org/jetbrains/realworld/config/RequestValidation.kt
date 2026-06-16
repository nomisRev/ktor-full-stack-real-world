package org.jetbrains.realworld.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.jetbrains.realworld.article.NewArticleRequest
import org.jetbrains.realworld.article.UpdateArticleRequest
import org.jetbrains.realworld.comment.NewCommentRequest
import org.jetbrains.realworld.error.GenericErrorModel
import org.jetbrains.realworld.user.UserLogin
import org.jetbrains.realworld.user.NewUserRequest
import org.jetbrains.realworld.user.UserUpdate

fun Application.configureValidation() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, GenericErrorModel(cause.reasons))
        }
    }
    install(RequestValidation) {
        validate<NewUserRequest> { request ->
            val errors = buildList {
                if (request.user.email.isBlank()) add("Email cannot be blank")
                if (!request.user.email.contains("@")) add("Invalid email format")
                if (request.user.username.isBlank()) add("Username cannot be blank")
                if (request.user.username.length < 3) add("Username must be at least 3 characters")
                if (request.user.password.isBlank()) add("Password cannot be blank")
                if (request.user.password.length < 8) add("Password must be at least 8 characters")
            }

            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
        }

        validate<UserLogin> { user ->
            val errors = buildList {
                if (user.email.isBlank()) add("Email cannot be blank")
                if (!user.email.contains("@")) add("Invalid email format")
                if (user.password.isBlank()) add("Password cannot be blank")
            }

            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
        }

        validate<UserUpdate> { user ->
            val errors = buildList {
                if (user.email?.isBlank() == true) add("Email cannot be blank")
                if (user.email?.contains("@") == false) add("Invalid email format")
                if (user.username?.isBlank() == true) add("Username cannot be blank")
                if (user.username?.length?.let { it < 3 } == true) add("Username must be at least 3 characters")
                if (user.password?.isBlank() == true) add("Password cannot be blank")
                if (user.password?.length?.let { it < 8 } == true) add("Password must be at least 8 characters")
            }

            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
        }

        validate<NewArticleRequest> { request ->
            val errors = buildList {
                if (request.article.title.isBlank()) add("Title cannot be blank")
                if (request.article.description.isBlank()) add("Description cannot be blank")
                if (request.article.body.isBlank()) add("Body cannot be blank")
            }

            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
        }

        validate<UpdateArticleRequest> { request ->
            val errors = buildList {
                if (request.article.title?.isBlank() == true) add("Title cannot be blank")
                if (request.article.description?.isBlank() == true) add("Description cannot be blank")
                if (request.article.body?.isBlank() == true) add("Body cannot be blank")
            }

            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
        }

        validate<NewCommentRequest> { request ->
            if (request.comment.body.isBlank()) ValidationResult.Invalid("Comment body cannot be blank")
            else ValidationResult.Valid
        }
    }
}

