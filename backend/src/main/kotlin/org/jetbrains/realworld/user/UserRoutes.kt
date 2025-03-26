package org.jetbrains.realworld.user

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.realworld.UserJWT
import org.jetbrains.realworld.error.ErrorResponse
import org.postgresql.util.PSQLState

fun Route.userRoutes(userService: UserService) {
    route("/users") {
        post {
            val request = call.receive<NewUserRequest>()
            try {
                val user = userService.registerUser(request.user)
                call.respond(HttpStatusCode.Created, UserResponse(user))
            } catch (e: ExposedSQLException) {
                if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorResponse("email", "is already registered")
                )
                else throw e
            }
        }

        route("/login") {
            post {
                val request = call.receive<UserLoginRequest>()
                val user = userService.loginUserOrNull(request.user)
                if (user != null) call.respond(HttpStatusCode.OK, UserResponse(user))
                else call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorResponse("email or password", "is invalid")
                )
            }
        }
    }

    authenticate {
        route("/user") {
            get {
                val principal = call.principal<UserJWT>()!!
                call.respond(HttpStatusCode.OK, UserResponse(principal.user))
            }

            put {
                val principal = call.principal<UserJWT>()!!
                val request = call.receive<UserUpdateRequest>()
                // TODO: incorrect update which results in conflict. email or username.
                val user = userService.updateUserOrNull(principal.userId, request.user)
                if (user != null) call.respond(HttpStatusCode.OK, UserResponse(user))
                else call.respond(HttpStatusCode.NotFound, mapOf("errors" to mapOf("user" to listOf("not found"))))
            }
        }
    }
}